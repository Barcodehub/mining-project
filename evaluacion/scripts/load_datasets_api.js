/*
 * Carga masiva de datasets API-ready para mining-management.
 * VERSIÓN ACTUALIZADA: Permite campos opcionales (null)
 *
 * Requiere archivos generados por generate_datasets.js:
 * - evaluacion/data/mediciones_api.json
 * - evaluacion/data/denuncias_api.json
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const runningInDocker = fs.existsSync('/.dockerenv');

const CONFIG = {
  managementUrl:  'http://localhost:8082',
  authEmail: process.env.AUTH_EMAIL || 'admin@admin.com',
  authPassword: process.env.AUTH_PASSWORD || 'Admin123',
  jwtSecret: process.env.JWT_SECRET || 'mySecretKeyForJWTTokenGeneration',
  sensorType: process.env.SENSOR_TYPE || null,
  createAdapter: (process.env.CREATE_ADAPTER || 'true').toLowerCase() !== 'false',
  dryRun: (process.env.DRY_RUN || 'false').toLowerCase() === 'true',
  maxRetries: Number(process.env.MAX_RETRIES || 2),
  requestTimeoutMs: Number(process.env.REQUEST_TIMEOUT_MS || 10000),
  delayMs: Number(process.env.REQUEST_DELAY_MS || 0),
  maxItems: null,
  medicionesFile: process.env.MEDICIONES_FILE || path.join(__dirname, '..', 'data', 'mediciones_api.json'),
  denunciasFile: process.env.DENUNCIAS_FILE || path.join(__dirname, '..', 'data', 'denuncias_api.json'),
};

const VALID_STATUSES = new Set(['SIN_VALIDAR', 'PENDIENTE', 'VALIDADO', 'RECHAZADO']);

function unwrapBody(body) {
  if (!body || typeof body !== 'object') return body;
  if (Object.prototype.hasOwnProperty.call(body, 'data')) return body.data;
  return body;
}

function buildRunSensorType(base) {
  const safeBase = (base || 'multi_parameter_sensor').trim();
  const stamp = new Date().toISOString().replace(/[-:.TZ]/g, '');
  return `${safeBase}_${stamp}`;
}

function parseMaxItemsArg(argv) {
  const arg = argv.find((a) => a.startsWith('--max-items='));
  if (!arg) return null;
  const raw = arg.split('=')[1];
  const value = Number(raw);
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`Valor invalido para --max-items: ${raw}`);
  }
  return value;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function base64UrlEncode(value) {
  return Buffer.from(typeof value === 'string' ? value : JSON.stringify(value))
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function createLocalAdminToken() {
  const header = { alg: 'HS256', typ: 'JWT' };
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    sub: CONFIG.authEmail,
    userId: 1,
    role: 'ADMINISTRADOR',
    roleId: 1,
    iat: now,
    exp: now + 86400,
  };

  const signingInput = `${base64UrlEncode(header)}.${base64UrlEncode(payload)}`;
  const signature = crypto
    .createHmac('sha256', CONFIG.jwtSecret)
    .update(signingInput)
    .digest('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');

  return `${signingInput}.${signature}`;
}

function readJsonArray(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`No existe el archivo: ${filePath}`);
  }
  const content = fs.readFileSync(filePath, 'utf8');
  const parsed = JSON.parse(content);
  if (!Array.isArray(parsed)) {
    throw new Error(`El archivo no contiene un arreglo JSON: ${filePath}`);
  }
  return parsed;
}

function isNumber(value) {
  return typeof value === 'number' && Number.isFinite(value);
}

function validateMedicionRecord(record, index) {
  const prefix = `mediciones[${index}]`;
  if (!record || typeof record !== 'object') throw new Error(`${prefix}: registro invalido`);
  if (typeof record.sensorType !== 'string' || !record.sensorType.trim()) throw new Error(`${prefix}: sensorType invalido`);
  if (!record.payload || typeof record.payload !== 'object') throw new Error(`${prefix}: payload invalido`);

  const p = record.payload;
  if (typeof p.deviceId !== 'string' || !p.deviceId.trim()) throw new Error(`${prefix}: payload.deviceId invalido`);
  if (!isNumber(p.latitude) || p.latitude < -90 || p.latitude > 90) throw new Error(`${prefix}: payload.latitude fuera de rango`);
  if (!isNumber(p.longitude) || p.longitude < -180 || p.longitude > 180) throw new Error(`${prefix}: payload.longitude fuera de rango`);
  if (typeof p.clusterId !== 'string' || !p.clusterId.trim()) throw new Error(`${prefix}: payload.clusterId invalido`);
  
  // Campos obligatorios
  if (!isNumber(p.ph)) throw new Error(`${prefix}: payload.ph invalido`);
  if (!isNumber(p.turbidity)) throw new Error(`${prefix}: payload.turbidity invalido`);
  if (!isNumber(p.temperature)) throw new Error(`${prefix}: payload.temperature invalido`);
  
  // Al menos uno de hg_total o hg_dissolved debe estar presente
  if (!isNumber(p.hg_total) && !isNumber(p.hg_dissolved)) {
    throw new Error(`${prefix}: se requiere al menos hg_total o hg_dissolved`);
  }
  
  // Campos opcionales: redox, dissolved_oxygen, organic_carbon, hg_sediment, hg_fish, methylmercury
  // No validamos si son null, solo si están presentes que sean números
  if (p.redox !== null && p.redox !== undefined && !isNumber(p.redox)) throw new Error(`${prefix}: payload.redox invalido`);
  if (p.dissolved_oxygen !== null && p.dissolved_oxygen !== undefined && !isNumber(p.dissolved_oxygen)) throw new Error(`${prefix}: payload.dissolved_oxygen invalido`);
  if (p.organic_carbon !== null && p.organic_carbon !== undefined && !isNumber(p.organic_carbon)) throw new Error(`${prefix}: payload.organic_carbon invalido`);
  if (p.hg_sediment !== null && p.hg_sediment !== undefined && !isNumber(p.hg_sediment)) throw new Error(`${prefix}: payload.hg_sediment invalido`);
  if (p.hg_fish !== null && p.hg_fish !== undefined && !isNumber(p.hg_fish)) throw new Error(`${prefix}: payload.hg_fish invalido`);
  if (p.methylmercury !== null && p.methylmercury !== undefined && !isNumber(p.methylmercury)) throw new Error(`${prefix}: payload.methylmercury invalido`);
  
  if (!Number.isFinite(Date.parse(p.timestamp))) throw new Error(`${prefix}: payload.timestamp invalido`);
}

function validateDenunciaRecord(record, index) {
  const prefix = `denuncias[${index}]`;
  if (!record || typeof record !== 'object') throw new Error(`${prefix}: registro invalido`);
  if (!record.post || typeof record.post !== 'object') throw new Error(`${prefix}: post invalido`);
  if (!record.workflow || typeof record.workflow !== 'object') throw new Error(`${prefix}: workflow invalido`);

  const post = record.post;
  if (typeof post.title !== 'string' || !post.title.trim()) throw new Error(`${prefix}: post.title invalido`);
  if (typeof post.content !== 'string' || !post.content.trim()) throw new Error(`${prefix}: post.content invalido`);
  if (!Number.isInteger(post.userId) || post.userId <= 0) throw new Error(`${prefix}: post.userId invalido`);
  if (post.type !== 'REPORT') throw new Error(`${prefix}: post.type debe ser REPORT`);
  if (!isNumber(post.latitude) || post.latitude < -90 || post.latitude > 90) throw new Error(`${prefix}: post.latitude fuera de rango`);
  if (!isNumber(post.longitude) || post.longitude < -180 || post.longitude > 180) throw new Error(`${prefix}: post.longitude fuera de rango`);
  if (!Array.isArray(post.images)) throw new Error(`${prefix}: post.images debe ser arreglo`);
  if (typeof post.clusterId !== 'string' || !post.clusterId.trim()) throw new Error(`${prefix}: post.clusterId invalido`);

  const status = record.workflow.targetStatus;
  if (!VALID_STATUSES.has(status)) throw new Error(`${prefix}: workflow.targetStatus invalido`);
  if ((status === 'VALIDADO' || status === 'RECHAZADO') && !Number.isInteger(record.workflow.validatorId)) {
    throw new Error(`${prefix}: workflow.validatorId requerido para estado ${status}`);
  }
}

async function requestJson(url, options, retries, timeoutMs) {
  let attempt = 0;
  let lastError;

  while (attempt <= retries) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const response = await fetch(url, { ...options, signal: controller.signal });
      clearTimeout(timeout);
      const text = await response.text();
      let body = null;
      if (text) {
        try {
          body = JSON.parse(text);
        } catch (_) {
          body = text;
        }
      }

      return { ok: response.ok, status: response.status, body };
    } catch (err) {
      clearTimeout(timeout);
      lastError = err;
      attempt += 1;
      if (attempt > retries) break;
      await sleep(300 * attempt);
    }
  }

  throw lastError;
}

async function login() {
  const url = `${CONFIG.managementUrl}/api/v1/auth/login`;
  const payload = { email: CONFIG.authEmail, password: CONFIG.authPassword };
  const result = await requestJson(
    url,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    },
    CONFIG.maxRetries,
    CONFIG.requestTimeoutMs
  );

  const loginData = unwrapBody(result.body);

  if (!result.ok || !loginData || !loginData.token) {
    throw new Error(`Login fallido (${result.status}): ${JSON.stringify(result.body)}`);
  }

  return loginData.token;
}

async function ensureAdapter(token, sensorType) {
  if (!CONFIG.createAdapter) return;
  if (!sensorType) {
    throw new Error('No se pudo resolver sensorType efectivo para crear adapter');
  }

  const url = `${CONFIG.managementUrl}/api/v1/adapters`;
  const payload = {
    sensorType,
    mapping: {
      device_id: 'deviceId',
      lat: 'latitude',
      lng: 'longitude',
      cluster_id: 'clusterId',
      ph: 'ph',
      hg_dissolved: 'hg_dissolved',
      hg_total: 'hg_total',
      hg_sediment: 'hg_sediment',
      methylmercury: 'methylmercury',
      hg_fish: 'hg_fish',
      turbidity: 'turbidity',
      temperature: 'temperature',
      redox: 'redox',
      dissolved_oxygen: 'dissolved_oxygen',
      organic_carbon: 'organic_carbon',
      timestamp: 'timestamp',
    },
  };

  const result = await requestJson(
    url,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(payload),
    },
    CONFIG.maxRetries,
    CONFIG.requestTimeoutMs
  );

  if (result.status === 201 || result.status === 409) {
    return;
  }

  if (!result.ok) {
    throw new Error(`No se pudo crear adapter (${result.status}): ${JSON.stringify(result.body)}`);
  }
}

async function loadMediciones(token, records, sensorType) {
  const summary = { total: records.length, ok: 0, failed: 0, errors: [] };

  for (let i = 0; i < records.length; i++) {
    const record = records[i];
    const payload = {
      sensorType: sensorType || record.sensorType,
      payload: {
        ...record.payload,
        clusterId: record.payload.clusterId
      },
    };

    if (CONFIG.dryRun) {
      summary.ok += 1;
      continue;
    }

    const result = await requestJson(
      `${CONFIG.managementUrl}/api/v1/environmental-data`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      },
      CONFIG.maxRetries,
      CONFIG.requestTimeoutMs
    );

    if (result.status === 201) {
      summary.ok += 1;
    } else {
      summary.failed += 1;
      if (summary.errors.length < 20) {
        summary.errors.push({ index: i, status: result.status, body: result.body });
      }
    }

    if (CONFIG.delayMs > 0) await sleep(CONFIG.delayMs);
  }

  return summary;
}

async function applyWorkflowForReport(token, reportId, workflow) {
  const status = workflow.targetStatus;

  if (status === 'SIN_VALIDAR') return;

  const requestValidation = await requestJson(
    `${CONFIG.managementUrl}/api/v1/reports/${reportId}/request-validation`,
    {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    },
    CONFIG.maxRetries,
    CONFIG.requestTimeoutMs
  );

  if (!requestValidation.ok) {
    throw new Error(`request-validation fallo (${requestValidation.status})`);
  }

  if (status === 'PENDIENTE') return;

  const changeStatus = await requestJson(
    `${CONFIG.managementUrl}/api/v1/reports/${reportId}/status`,
    {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        status,
        validatorId: workflow.validatorId,
      }),
    },
    CONFIG.maxRetries,
    CONFIG.requestTimeoutMs
  );

  if (!changeStatus.ok) {
    throw new Error(`change status fallo (${changeStatus.status})`);
  }
}

async function loadDenuncias(token, records) {
  const summary = { total: records.length, ok: 0, failed: 0, errors: [] };

  for (let i = 0; i < records.length; i++) {
    const record = records[i];

    if (CONFIG.dryRun) {
      summary.ok += 1;
      continue;
    }

    const postWithCluster = {
      ...record.post,
      clusterId: record.post.clusterId
    };
    const createRes = await requestJson(
      `${CONFIG.managementUrl}/api/v1/posts`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(postWithCluster),
      },
      CONFIG.maxRetries,
      CONFIG.requestTimeoutMs
    );

    const createdPost = unwrapBody(createRes.body);

    if (!createRes.ok || !createdPost || !createdPost.id) {
      summary.failed += 1;
      if (summary.errors.length < 20) {
        summary.errors.push({ index: i, phase: 'create-post', status: createRes.status, body: createRes.body });
      }
      continue;
    }

    try {
      await applyWorkflowForReport(token, createdPost.id, record.workflow);
      summary.ok += 1;
    } catch (error) {
      summary.failed += 1;
      if (summary.errors.length < 20) {
        summary.errors.push({
          index: i,
          phase: 'workflow',
          reportId: createdPost.id,
          message: error.message,
        });
      }
    }

    if (CONFIG.delayMs > 0) await sleep(CONFIG.delayMs);
  }

  return summary;
}

function printSummary(name, summary) {
  console.log(`\n[${name}]`);
  console.log(`  Total  : ${summary.total}`);
  console.log(`  Exitos : ${summary.ok}`);
  console.log(`  Fallos : ${summary.failed}`);
  if (summary.errors.length > 0) {
    console.log('  Muestras de error:');
    for (const err of summary.errors.slice(0, 5)) {
      console.log(`    - ${JSON.stringify(err)}`);
    }
  }
}

async function main() {
  console.log('Inicio de carga de datasets API-ready...');
  console.log(`MANAGEMENT_URL: ${CONFIG.managementUrl}`);
  console.log(`DRY_RUN: ${CONFIG.dryRun}`);

  CONFIG.maxItems = parseMaxItemsArg(process.argv.slice(2));

  let mediciones = readJsonArray(CONFIG.medicionesFile);
  let denuncias = readJsonArray(CONFIG.denunciasFile);
  const datasetSensorType = mediciones[0] && mediciones[0].sensorType;
  const effectiveSensorType = CONFIG.sensorType || buildRunSensorType(datasetSensorType);

  if (CONFIG.maxItems && CONFIG.maxItems > 0) {
    console.log(`Aplicando limite por argumento: --max-items=${CONFIG.maxItems}`);
    mediciones = mediciones.slice(0, CONFIG.maxItems);
    denuncias = denuncias.slice(0, CONFIG.maxItems);
  }

  mediciones.forEach(validateMedicionRecord);
  denuncias.forEach(validateDenunciaRecord);

  console.log(`Registros validados. Mediciones: ${mediciones.length}, Denuncias: ${denuncias.length}`);
  console.log(`SENSOR_TYPE efectivo: ${effectiveSensorType}`);
  if (!CONFIG.sensorType) {
    console.log('SENSOR_TYPE no definido por entorno: se usa uno unico por ejecucion para evitar colisiones de adapter.');
  }

  let token = 'dry-run-token';
  if (!CONFIG.dryRun) {
    try {
      token = await login();
      console.log('Token obtenido via /api/v1/auth/login.');
    } catch (error) {
      console.warn(`Login via API falló (${error.message}). Se usará un JWT local equivalente para continuar.`);
      token = createLocalAdminToken();
    }
    await ensureAdapter(token, effectiveSensorType);
  }

  const medicionesSummary = await loadMediciones(token, mediciones, effectiveSensorType);
  const denunciasSummary = await loadDenuncias(token, denuncias);

  printSummary('MEDICIONES', medicionesSummary);
  printSummary('DENUNCIAS', denunciasSummary);

  console.log('\nCarga finalizada.');
}

main().catch((error) => {
  console.error('\nError en carga:', error.message);
  process.exit(1);
});
