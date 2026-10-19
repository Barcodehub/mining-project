/**
 * Prueba de carga v2 (k6).
 *
 * Igual que la v1 (load_test.js) en etapas, mezcla de flujos y umbrales, salvo:
 *  - POST /environmental-data usa un pool de payloads de TRES fuentes heterogéneas:
 *    sintéticos anclados en formato IoT canónico, filas reales de la CDMB y de CORPONOR en su
 *    formato original (data/payload_pool.json). Los adapters se registran en setup().
 *  - Se elimina el contador de "alertas críticas" de la v1, que sumaba filas devueltas por los GET.
 *    El conteo de alertas reales sale del servidor (environmental_alerts_published_total,
 *    alert_consumed_total) y, del lado cliente, de la clasificación de cada POST
 *    (mediciones_publicables_total = respuestas con nivel ALERTA o CRITICO).
 *  - Los reportes usan los clústeres sintéticos SIM-A…SIM-G.
 *  - handleSummary exporta el resumen completo a RESULT_FILE (JSON).
 */
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const BASE_URL_MANAGEMENT = __ENV.MANAGEMENT_URL || 'http://mining-management:8082';
const ESCENARIO = __ENV.ESCENARIO || 'medio';
const RESULT_FILE = __ENV.RESULT_FILE || '';

// ── Métricas ───────────────────────────────────────────────────────────────
const errorRate          = new Rate('error_rate');
const postReportDuration = new Trend('post_report_duration_ms', true);
const getReportsDuration = new Trend('get_reports_duration_ms', true);
const getMedicionesDur   = new Trend('get_mediciones_duration_ms', true);
const postMedicionDur    = new Trend('post_medicion_duration_ms', true);
const reportesCreados    = new Counter('reportes_creados_total');
const medicionesCreadas  = new Counter('mediciones_creadas_total');
const medicionesPublicables = new Counter('mediciones_publicables_total');
const medicionesPorFuente = {
  multi_parameter_sensor: new Counter('mediciones_fuente_iot_total'),
  cdmb_open_data: new Counter('mediciones_fuente_cdmb_total'),
  corponor_report: new Counter('mediciones_fuente_corponor_total'),
};

// ── Perfiles de carga (idénticos a la v1) ──────────────────────────────────
const PERFILES = {
  bajo: {
    scenarios: { carga_baja: { executor: 'ramping-vus', startVUs: 0, gracefulRampDown: '10s',
      stages: [{ duration: '30s', target: 20 }, { duration: '60s', target: 50 }, { duration: '30s', target: 0 }] } },
    thresholds: { http_req_duration: ['p(95)<500', 'p(99)<800'], error_rate: ['rate<0.01'] },
  },
  medio: {
    scenarios: { carga_media: { executor: 'ramping-vus', startVUs: 0, gracefulRampDown: '15s',
      stages: [{ duration: '1m', target: 50 }, { duration: '2m', target: 200 }, { duration: '3m', target: 200 },
               { duration: '1m', target: 100 }, { duration: '30s', target: 0 }] } },
    thresholds: { http_req_duration: ['p(95)<1000', 'p(99)<2000'], error_rate: ['rate<0.03'] },
  },
  alto: {
    scenarios: { ramp_up: { executor: 'ramping-vus', startVUs: 0, gracefulRampDown: '30s',
      stages: [{ duration: '1m', target: 100 }, { duration: '2m', target: 300 }, { duration: '3m', target: 500 },
               { duration: '30s', target: 500 }, { duration: '2m', target: 200 }, { duration: '1m', target: 0 }] } },
    thresholds: { http_req_duration: ['p(95)<2000', 'p(99)<4000'], error_rate: ['rate<0.05'] },
  },
};

export const options = Object.assign({}, PERFILES[ESCENARIO] || PERFILES.medio, {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'p(99.9)', 'count'],
});

// ── Datos compartidos ──────────────────────────────────────────────────────
const POOL = new SharedArray('payload_pool', function () {
  return JSON.parse(open('./data/payload_pool.json')).items;
});
const ADAPTERS = new SharedArray('adapters', function () {
  return JSON.parse(open('./data/adapters.json'));
});
const CLUSTERS = new SharedArray('clusters', function () {
  return [
    { id: 'SIM-A', lat: 7.35, lng: -72.92 }, { id: 'SIM-B', lat: 7.42, lng: -72.80 },
    { id: 'SIM-C', lat: 7.20, lng: -73.02 }, { id: 'SIM-D', lat: 7.60, lng: -72.75 },
    { id: 'SIM-E', lat: 7.88, lng: -72.52 }, { id: 'SIM-F', lat: 7.10, lng: -73.15 },
    { id: 'SIM-G', lat: 7.95, lng: -72.60 },
  ];
});
const DESCRIPCIONES = [
  'Agua turbia y con olor químico en la quebrada.', 'Maquinaria operando en la ribera sin señalización.',
  'Peces muertos aguas abajo del entable.', 'Vertimiento de lodos grises al cauce.',
  'Ruido de molinos de noche cerca del río.', 'Coloración amarillenta del agua tras la lluvia.',
];

function randomItem(arr) { return arr[Math.floor(Math.random() * arr.length)]; }
function randomFloat(min, max, d = 4) { return parseFloat((min + Math.random() * (max - min)).toFixed(d)); }
function isoNow() { return new Date().toISOString(); }
function isoDateBack(days) { const d = new Date(); d.setDate(d.getDate() - days); return d.toISOString(); }
function authHeaders(token) { return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }; }

// ── Flujos (mismas proporciones que la v1) ─────────────────────────────────
function crearReporte(token) {
  const c = randomItem(CLUSTERS);
  const payload = JSON.stringify({
    title: `Reporte ${c.id} - ${new Date().toLocaleDateString('es-CO')}`, content: randomItem(DESCRIPCIONES),
    userId: Math.floor(Math.random() * 200) + 2, type: 'REPORT',
    latitude: randomFloat(c.lat - 0.03, c.lat + 0.03, 6), longitude: randomFloat(c.lng - 0.03, c.lng + 0.03, 6),
    clusterId: c.id, images: [],
  });
  const res = http.post(`${BASE_URL_MANAGEMENT}/api/v1/posts`, payload,
    { headers: authHeaders(token), tags: { name: 'POST /api/v1/posts' }, timeout: '10s' });
  postReportDuration.add(res.timings.duration);
  errorRate.add(res.status !== 201);
  const ok = check(res, {
    'crear reporte status 201': (r) => r.status === 201,
    'reporte tiene id': (r) => { try { return !!JSON.parse(r.body).id; } catch (_) { return false; } },
  });
  if (ok) reportesCreados.add(1);
}

function obtenerReportes(token) {
  const filtros = ['', '?status=PENDIENTE', '?status=VALIDADO', `?startDate=${isoDateBack(30)}&endDate=${isoNow()}`,
    '?minLat=7.0&maxLat=7.6&minLng=-73.2&maxLng=-72.7', '?status=SIN_VALIDAR'];
  const res = http.get(`${BASE_URL_MANAGEMENT}/api/v1/reports${randomItem(filtros)}`,
    { headers: authHeaders(token), tags: { name: 'GET /api/v1/reports' } });
  getReportsDuration.add(res.timings.duration);
  errorRate.add(res.status !== 200);
  check(res, {
    'GET reportes status 200': (r) => r.status === 200,
    'GET reportes devuelve array': (r) => { try { return Array.isArray(JSON.parse(r.body)); } catch (_) { return false; } },
  });
}

function obtenerMediciones(token) {
  const filtros = ['', `?startDate=${isoDateBack(7)}&endDate=${isoNow()}`, '?hgTotalMin=1.0',
    '?minLat=7.2&maxLat=7.5&minLng=-73.0&maxLng=-72.8', '?phMin=4.0&phMax=6.5', '?alertLevel=CRITICO',
    '?alertLevel=ALERTA', '?alertType=IMPACTO', '?alertType=RIESGO'];
  const res = http.get(`${BASE_URL_MANAGEMENT}/api/v1/environmental-data${randomItem(filtros)}`,
    { headers: authHeaders(token), tags: { name: 'GET /api/v1/environmental-data' } });
  getMedicionesDur.add(res.timings.duration);
  errorRate.add(res.status !== 200);
  check(res, { 'GET mediciones status 200': (r) => r.status === 200 });
}

function enviarMedicion(token) {
  const item = randomItem(POOL);
  const res = http.post(`${BASE_URL_MANAGEMENT}/api/v1/environmental-data`,
    JSON.stringify({ sensorType: item.sensorType, payload: item.payload }),
    { headers: authHeaders(token), tags: { name: 'POST /api/v1/environmental-data', source: item.sensorType } });
  postMedicionDur.add(res.timings.duration, { source: item.sensorType });
  errorRate.add(res.status !== 201);
  let level = null;
  const ok = check(res, {
    'POST medicion status 201': (r) => r.status === 201,
    'medicion clasificada': (r) => {
      try { const b = JSON.parse(r.body); level = b.alertLevel; return !!b.id && !!b.alertLevel; }
      catch (_) { return false; }
    },
  });
  if (ok) {
    medicionesCreadas.add(1);
    if (medicionesPorFuente[item.sensorType]) medicionesPorFuente[item.sensorType].add(1);
    if (level === 'ALERTA' || level === 'CRITICO') medicionesPublicables.add(1);
  }
}

function obtenerAgregados(token) {
  const res = http.get(`${BASE_URL_MANAGEMENT}/api/v1/environmental-data/aggregate?startDate=${isoDateBack(30)}&endDate=${isoNow()}`,
    { headers: authHeaders(token), tags: { name: 'GET /api/v1/environmental-data/aggregate' } });
  errorRate.add(res.status !== 200);
  check(res, { 'GET aggregate status 200': (r) => r.status === 200 });
}

function consultarAnalytics(token) {
  const ep = randomItem(['/api/v1/analytics/clusters', '/api/v1/analytics/reports',
    '/api/v1/analytics/fusion', '/api/v1/analytics/correlation']);
  const res = http.get(`${BASE_URL_MANAGEMENT}${ep}`, { headers: authHeaders(token), tags: { name: 'GET /api/v1/analytics' } });
  errorRate.add(res.status !== 200);
  check(res, { 'GET analytics status 200': (r) => r.status === 200 });
}

// ── Setup / default / resumen ──────────────────────────────────────────────
export function setup() {
  const res = http.post(`${BASE_URL_MANAGEMENT}/api/v1/auth/login`,
    JSON.stringify({ email: 'admin@admin.com', password: 'Admin123' }), { headers: { 'Content-Type': 'application/json' } });
  const body = JSON.parse(res.body);
  const token = body.token || (body.data && body.data.token);
  if (!token) throw new Error('[SETUP] sin token');
  for (const a of ADAPTERS) {
    const r = http.post(`${BASE_URL_MANAGEMENT}/api/v1/adapters`, JSON.stringify(a), { headers: authHeaders(token) });
    if (r.status !== 201) throw new Error(`[SETUP] adapter ${a.sensorType}: ${r.status}`);
  }
  return { token, startTime: Date.now() };
}

export default function (data) {
  const token = data.token;
  const r = Math.random();
  if (r < 0.40) group('Consultar reportes', () => { obtenerReportes(token); sleep(randomFloat(0.5, 1.5, 2)); });
  else if (r < 0.70) group('Consultar mediciones ambientales', () => { obtenerMediciones(token); sleep(randomFloat(0.5, 1.2, 2)); });
  else if (r < 0.80) group('Crear reporte de denuncia', () => { crearReporte(token); sleep(randomFloat(1.0, 2.5, 2)); });
  else if (r < 0.90) group('Enviar medición de sensor', () => { enviarMedicion(token); sleep(randomFloat(0.2, 0.8, 2)); });
  else if (r < 0.95) group('Consultar agregados (dashboard)', () => { obtenerAgregados(token); sleep(randomFloat(1.5, 3.0, 2)); });
  else group('Consultar analytics', () => { consultarAnalytics(token); sleep(randomFloat(1.0, 2.0, 2)); });
}

export function handleSummary(data) {
  const out = { stdout: `\n[k6] escenario=${ESCENARIO} iteraciones=${data.metrics.iterations ? data.metrics.iterations.values.count : 0}\n` };
  if (RESULT_FILE) out[RESULT_FILE] = JSON.stringify(data, null, 1);
  return out;
}
