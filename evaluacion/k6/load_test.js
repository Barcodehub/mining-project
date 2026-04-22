/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SCRIPT DE PRUEBAS DE RENDIMIENTO - k6
 * Sistema de Monitoreo Ambiental y Denuncias de Minería Ilegal
 * Norte de Santander, Colombia
 *
 * Microservicio evaluado : mining-management (puerto 8081)
 * Microservicio de auth  : mining-users     (puerto 8082)
 *
 * Ejecutar:
 *   k6 run load_test.js
 *   k6 run --out influxdb=http://localhost:8086/k6 load_test.js   (con InfluxDB)
 *   k6 run --out json=results.json load_test.js                   (exportar JSON)
 *
 * Escenarios disponibles (cambiar ESCENARIO abajo):
 *   'bajo'  → 50 VU,  60s
 *   'medio' → 200 VU, 120s con ramp-up
 *   'alto'  → 500 VU, 180s con ramp-up + peak
 * ═══════════════════════════════════════════════════════════════════════════
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// ─────────────────────────────────────────────────────────────────────────────
// CONFIGURACIÓN DE ENTORNO
// ─────────────────────────────────────────────────────────────────────────────
const BASE_URL_MANAGEMENT = __ENV.MANAGEMENT_URL || 'http://localhost:8080';
const BASE_URL_AUTH       = __ENV.AUTH_URL       || 'http://localhost:8082';
const ESCENARIO           = __ENV.ESCENARIO      || 'medio';  // bajo | medio | alto
const SENSOR_TYPE_ENV     = __ENV.SENSOR_TYPE    || 'multi_parameter_sensor';

// ─────────────────────────────────────────────────────────────────────────────
// MÉTRICAS PERSONALIZADAS
// ─────────────────────────────────────────────────────────────────────────────
const errorRate          = new Rate('error_rate');
const loginDuration      = new Trend('login_duration_ms',        true);
const postReportDuration = new Trend('post_report_duration_ms',  true);
const getReportsDuration = new Trend('get_reports_duration_ms',  true);
const getMedicionesDur   = new Trend('get_mediciones_duration_ms', true);
const postMedicionDur    = new Trend('post_medicion_duration_ms', true);
const reportesCreados    = new Counter('reportes_creados_total');
const medicionesCreadas  = new Counter('mediciones_creadas_total');
const alertasCriticas    = new Counter('alertas_criticas_detectadas');

// ─────────────────────────────────────────────────────────────────────────────
// PERFILES DE CARGA POR ESCENARIO
// ─────────────────────────────────────────────────────────────────────────────
const PERFILES = {
  bajo: {
    scenarios: {
      carga_baja: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
          { duration: '30s', target: 20 },
          { duration: '60s', target: 50 },
          { duration: '30s', target: 0  },
        ],
        gracefulRampDown: '10s',
      },
    },
    thresholds: {
      http_req_duration:          ['p(95)<500', 'p(99)<800'],
      error_rate:                 ['rate<0.01'],
      post_report_duration_ms:    ['p(95)<600'],
      get_reports_duration_ms:    ['p(95)<400'],
      get_mediciones_duration_ms: ['p(95)<350'],
    },
  },

  medio: {
    scenarios: {
      carga_media: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
          { duration: '1m',  target: 50  },
          { duration: '2m',  target: 200 },
          { duration: '3m',  target: 200 },
          { duration: '1m',  target: 100 },
          { duration: '30s', target: 0   },
        ],
        gracefulRampDown: '15s',
      },
    },
    thresholds: {
      http_req_duration:          ['p(95)<1000', 'p(99)<2000'],
      error_rate:                 ['rate<0.03'],
      post_report_duration_ms:    ['p(95)<1200'],
      get_reports_duration_ms:    ['p(95)<800'],
      get_mediciones_duration_ms: ['p(95)<700'],
    },
  },

  alto: {
    scenarios: {
      ramp_up: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
          { duration: '1m',  target: 100 },
          { duration: '2m',  target: 300 },
          { duration: '3m',  target: 500 },
          { duration: '30s', target: 500 },
          { duration: '2m',  target: 200 },
          { duration: '1m',  target: 0   },
        ],
        gracefulRampDown: '30s',
      },
    },
    thresholds: {
      http_req_duration:          ['p(95)<2000', 'p(99)<4000'],
      error_rate:                 ['rate<0.05'],
      post_report_duration_ms:    ['p(95)<2500'],
      get_reports_duration_ms:    ['p(95)<1500'],
      get_mediciones_duration_ms: ['p(95)<1200'],
    },
  },
};

export const options = PERFILES[ESCENARIO] || PERFILES.medio;

// ─────────────────────────────────────────────────────────────────────────────
// DATOS COMPARTIDOS
// ─────────────────────────────────────────────────────────────────────────────
const COORDENADAS = new SharedArray('coordenadas', function () {
  return [
    { lat: 8.07, lng: -72.83, zona: 'C1' },
    { lat: 8.09, lng: -72.80, zona: 'C1' },
    { lat: 8.06, lng: -72.85, zona: 'C1' },
    { lat: 7.71, lng: -72.64, zona: 'C2' },
    { lat: 7.73, lng: -72.66, zona: 'C2' },
    { lat: 8.35, lng: -72.71, zona: 'C3' },
    { lat: 8.36, lng: -72.73, zona: 'C3' },
    { lat: 7.92, lng: -72.52, zona: 'T1' },
    { lat: 7.94, lng: -72.50, zona: 'T1' },
    { lat: 7.76, lng: -72.31, zona: 'N1' },
    { lat: 7.78, lng: -72.29, zona: 'N1' },
    { lat: 8.41, lng: -72.37, zona: 'N2' },
    { lat: 7.54, lng: -72.21, zona: 'N3' },
  ];
});

const DESCRIPCIONES = new SharedArray('descripciones', function () {
  return [
    'Río con coloración amarilla y olor fuerte, sospecha de mercurio por dragas ilegales.',
    'Maquinaria pesada operando sin permisos ambientales en orilla del río.',
    'Peces muertos en la quebrada, posible contaminación química.',
    'Retroexcavadoras trabajando de noche sin señalización.',
    'Vertimiento de lodos con sustancias químicas al río.',
    'Draga flotante en zona de reserva hídrica sin licencia.',
    'Agua turbia y con residuos metálicos en afluente.',
    'Campamento ilegal con equipos de procesamiento de oro.',
    'Tala de árboles en zona de protección hídrica.',
    'Quema de vegetación cerca a nacimiento de agua.',
  ];
});

const SENSOR_TYPES = new SharedArray('sensorTypes', function () {
  return [SENSOR_TYPE_ENV];
});

const USERS = new SharedArray('users', function () {
  return [
    { email: 'admin@admin.com', password: 'Admin123', role: 'ADMINISTRADOR' },
  ];
});

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────
function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function randomFloat(min, max, decimals = 4) {
  return parseFloat((min + Math.random() * (max - min)).toFixed(decimals));
}

function isoNow() {
  return new Date().toISOString();
}

function isoDateBack(days) {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString();
}

const JSON_HEADERS = { 'Content-Type': 'application/json' };

function authHeaders(token) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

// ─────────────────────────────────────────────────────────────────────────────
// FLUJOS
// ─────────────────────────────────────────────────────────────────────────────
function crearReporte(token) {
  const coord = randomItem(COORDENADAS);
  const payload = JSON.stringify({
    title    : `Denuncia - Zona ${coord.zona} - ${new Date().toLocaleDateString('es-CO')}`,
    content  : randomItem(DESCRIPCIONES),
    userId   : Math.floor(Math.random() * 100) + 1,
    type     : 'REPORT',
    latitude : randomFloat(coord.lat - 0.05, coord.lat + 0.05, 6),
    longitude: randomFloat(coord.lng - 0.05, coord.lng + 0.05, 6),
    images   : [],
  });

  const res = http.post(`${BASE_URL_MANAGEMENT}/api/v1/posts`, payload, {
    headers: authHeaders(token),
    tags   : { endpoint: 'POST /api/v1/posts' },
    timeout: '10s',   // falla rápido en vez de colgar la VU 60s
  });

  // res.error se llena cuando hay timeout u otro error de red
  if (res.error) {
    console.warn(`[crearReporte] Error de red: ${res.error}`);
    errorRate.add(1);
    return false;
  }

  postReportDuration.add(res.timings.duration);
  errorRate.add(res.status !== 201);

  const ok = check(res, {
    'crear reporte status 201'       : (r) => r.status === 201,
    'reporte tiene id'               : (r) => {
      try { return !!JSON.parse(r.body).id; } catch (_) { return false; }
    },
    'latencia crear reporte < 2000ms': (r) => r.timings.duration < 2000,
  });

  if (ok) reportesCreados.add(1);
  return ok;
}

function obtenerReportes(token) {
  const filtros = [
    '',
    '?status=PENDIENTE',
    '?status=VALIDADO',
    `?startDate=${isoDateBack(30)}&endDate=${isoNow()}`,
    `?minLat=7.6&maxLat=8.2&minLng=-73.0&maxLng=-72.5`,
    `?status=SIN_VALIDAR`,
  ];

  const filtro = randomItem(filtros);
  const res = http.get(
      `${BASE_URL_MANAGEMENT}/api/v1/reports${filtro}`,
      { headers: authHeaders(token), tags: { endpoint: 'GET /api/v1/reports' } }
  );

  getReportsDuration.add(res.timings.duration);
  errorRate.add(res.status !== 200);

  check(res, {
    'GET reportes status 200'        : (r) => r.status === 200,
    'GET reportes devuelve array'    : (r) => {
      try { return Array.isArray(JSON.parse(r.body)); } catch (_) { return false; }
    },
    'latencia GET reportes < 1500ms' : (r) => r.timings.duration < 1500,
  });
}

function obtenerMediciones(token) {
  const filtros = [
    '',
    `?startDate=${isoDateBack(7)}&endDate=${isoNow()}`,
    `?mercuryMin=0.8`,
    `?minLat=8.0&maxLat=8.2&minLng=-73.0&maxLng=-72.6`,
    `?phMin=4.0&phMax=6.0`,
    `?mercuryMin=0.5&phMax=6.5`,
  ];

  const filtro = randomItem(filtros);
  const res = http.get(
      `${BASE_URL_MANAGEMENT}/api/v1/environmental-data${filtro}`,
      { headers: authHeaders(token), tags: { endpoint: 'GET /api/v1/environmental-data' } }
  );

  getMedicionesDur.add(res.timings.duration);
  errorRate.add(res.status !== 200);

  const ok = check(res, {
    'GET mediciones status 200'       : (r) => r.status === 200,
    'latencia GET mediciones < 1200ms': (r) => r.timings.duration < 1200,
  });

  if (ok && res.body) {
    try {
      const data = JSON.parse(res.body);
      const criticos = Array.isArray(data) ? data.filter(m => m.mercury > 0.8).length : 0;
      if (criticos > 0) alertasCriticas.add(criticos);
    } catch (_) {}
  }
}

function enviarMedicion(token) {
  const coord      = randomItem(COORDENADAS);
  const esContam   = ['C1', 'C2', 'C3'].includes(coord.zona);
  const sensorType = randomItem(SENSOR_TYPES);

  const payload = JSON.stringify({
    sensorType,
    payload: {
      deviceId   : `SENSOR-${coord.zona}-${String(Math.floor(Math.random() * 99) + 1).padStart(3, '0')}`,
      latitude   : randomFloat(coord.lat - 0.05, coord.lat + 0.05, 6),
      longitude  : randomFloat(coord.lng - 0.05, coord.lng + 0.05, 6),
      ph         : esContam ? randomFloat(4.0, 5.8, 2) : randomFloat(6.5, 7.8, 2),
      mercury    : esContam ? randomFloat(0.6, 1.5, 4) : randomFloat(0.0, 0.25, 4),
      turbidity  : esContam ? randomFloat(30, 120, 1)  : randomFloat(1, 18, 1),
      temperature: randomFloat(18, 32, 1),
      timestamp  : isoNow(),
    },
  });

  const res = http.post(`${BASE_URL_MANAGEMENT}/api/v1/environmental-data`, payload, {
    headers: authHeaders(token),
    tags   : { endpoint: 'POST /api/v1/environmental-data' },
  });

  postMedicionDur.add(res.timings.duration);
  errorRate.add(res.status !== 201);

  const ok = check(res, {
    'POST medicion status 201'       : (r) => r.status === 201,
    'medicion tiene id'              : (r) => {
      try { return !!JSON.parse(r.body).id; } catch (_) { return false; }
    },
    'latencia POST medicion < 2000ms': (r) => r.timings.duration < 2000,
  });

  if (ok) medicionesCreadas.add(1);
}

function obtenerAgregados(token) {
  const res = http.get(
      `${BASE_URL_MANAGEMENT}/api/v1/environmental-data/aggregate?` +
      `startDate=${isoDateBack(30)}&endDate=${isoNow()}`,
      { headers: authHeaders(token), tags: { endpoint: 'GET /api/v1/environmental-data/aggregate' } }
  );

  errorRate.add(res.status !== 200);
  check(res, {
    'GET aggregate status 200': (r) => r.status === 200,
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// SETUP: login UNA SOLA VEZ — el token se pasa a todas las VUs via data
// ─────────────────────────────────────────────────────────────────────────────
export function setup() {
  console.log(`
╔═══════════════════════════════════════════════════════════════╗
║   PRUEBA DE RENDIMIENTO - SISTEMA MINERÍA ILEGAL              ║
║   Escenario    : ${ESCENARIO.toUpperCase().padEnd(44)}║
║   Management   : ${BASE_URL_MANAGEMENT.padEnd(44)}║
║   Auth         : ${BASE_URL_AUTH.padEnd(44)}║
║   SensorType   : ${SENSOR_TYPE_ENV.padEnd(44)}║
╚═══════════════════════════════════════════════════════════════╝
  `);

  // Health check
  const health = http.get(`${BASE_URL_MANAGEMENT}/actuator/health`);
  console.log(`[HEALTH CHECK] management: ${health.status}`);

  // 🔑 Login una sola vez
  const user = USERS[0];
  const res = http.post(
      `${BASE_URL_MANAGEMENT}/api/v1/auth/login`,
      JSON.stringify({ email: user.email, password: user.password }),
      { headers: JSON_HEADERS }
  );

  loginDuration.add(res.timings.duration);

  check(res, {
    'setup: login status 200': (r) => r.status === 200,
    'setup: token presente'  : (r) => {
      try {
        const body = JSON.parse(r.body);
        return !!(body.token || (body.data && body.data.token));
      } catch (_) { return false; }
    },
  });

  const body  = JSON.parse(res.body);
  const token = body.token || (body.data && body.data.token);

  if (!token) {
    throw new Error('[SETUP] No se pudo obtener token — abortando prueba.');
  }

  console.log('[SETUP] Token obtenido correctamente.');
  return { startTime: Date.now(), token };
}

// ─────────────────────────────────────────────────────────────────────────────
// DEFAULT: cada VU reutiliza el token obtenido en setup()
// ─────────────────────────────────────────────────────────────────────────────
export default function (data) {
  const token = data.token;   // ✅ token compartido, sin login por iteración

  if (!token) {
    sleep(1);
    return;
  }

  // Distribución de acciones:
  //   45% → leer reportes       (operación más frecuente)
  //   30% → leer mediciones     (consulta de datos ambientales)
  //   10% → crear reporte       (ciudadano denuncia) ← reducido: endpoint costoso
  //   10% → enviar medición     (sensor automatizado)
  //    5% → consultar agregados (dashboard)
  const r = Math.random();

  if (r < 0.45) {
    group('Consultar reportes', function () {
      obtenerReportes(token);
      sleep(randomFloat(0.5, 1.5, 2));
    });
  } else if (r < 0.75) {
    group('Consultar mediciones ambientales', function () {
      obtenerMediciones(token);
      sleep(randomFloat(0.5, 1.2, 2));
    });
  } else if (r < 0.85) {
    group('Crear reporte de denuncia', function () {
      crearReporte(token);
      sleep(randomFloat(1.0, 2.5, 2));
    });
  } else if (r < 0.95) {
    group('Enviar medición de sensor', function () {
      enviarMedicion(token);
      sleep(randomFloat(0.2, 0.8, 2));
    });
  } else {
    group('Consultar agregados (dashboard)', function () {
      obtenerAgregados(token);
      sleep(randomFloat(1.5, 3.0, 2));
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// TEARDOWN
// ─────────────────────────────────────────────────────────────────────────────
export function teardown(data) {
  const elapsed = ((Date.now() - data.startTime) / 1000).toFixed(1);
  console.log(`\n[TEARDOWN] Prueba completada en ${elapsed}s`);
  console.log('[TEARDOWN] Revisar métricas en la salida de k6 y/o Grafana.');
}