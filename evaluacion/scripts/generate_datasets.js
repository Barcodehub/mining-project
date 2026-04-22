/**
 * GENERADOR DE DATASETS SIMULADOS
 * Sistema de Monitoreo Ambiental y Denuncias de Minería Ilegal
 * Norte de Santander, Colombia
 *
 * Genera:
 *  - 1000 mediciones ambientales con clusters geográficos realistas
 *  - 500 denuncias con correlación espacial a zonas contaminadas
 */

const fs = require('fs');
const path = require('path');

const DEFAULT_SENSOR_TYPE = 'multi_parameter_sensor';

// ─────────────────────────────────────────────────────────────────────────────
// CONFIGURACIÓN DE CLUSTERS GEOGRÁFICOS
// Basado en zonas reales de Norte de Santander con actividad minera ilegal
// ─────────────────────────────────────────────────────────────────────────────
const CLUSTERS = [
  // ── ZONAS CONTAMINADAS (Alta actividad minera ilegal) ──────────────────────
  {
    id: 'C1',
    nombre: 'Cuenca Río Catatumbo',
    tipo: 'contaminada',
    lat: 8.08, lng: -72.82,
    radio: 0.18,           // ~20 km de radio
    peso: 220,             // número de mediciones en este cluster
    mercurio: { min: 0.82, max: 1.50 },
    ph:       { min: 4.0,  max: 5.5  },
    turbidez: { min: 45,   max: 120  },
    temp:     { min: 26,   max: 32   },
  },
  {
    id: 'C2',
    nombre: 'Serranía del Perijá Sur',
    tipo: 'contaminada',
    lat: 7.72, lng: -72.65,
    radio: 0.14,
    peso: 180,
    mercurio: { min: 0.75, max: 1.30 },
    ph:       { min: 4.3,  max: 5.8  },
    turbidez: { min: 38,   max: 95   },
    temp:     { min: 24,   max: 30   },
  },
  {
    id: 'C3',
    nombre: 'Zona Minera Tibú-El Tarra',
    tipo: 'contaminada',
    lat: 8.35, lng: -72.72,
    radio: 0.12,
    peso: 150,
    mercurio: { min: 0.65, max: 1.20 },
    ph:       { min: 4.5,  max: 6.0  },
    turbidez: { min: 30,   max: 80   },
    temp:     { min: 25,   max: 31   },
  },
  // ── ZONA DE TRANSICIÓN (Contaminación moderada) ────────────────────────────
  {
    id: 'T1',
    nombre: 'Área Periurbana Cúcuta Norte',
    tipo: 'transicion',
    lat: 7.93, lng: -72.51,
    radio: 0.10,
    peso: 120,
    mercurio: { min: 0.28, max: 0.65 },
    ph:       { min: 5.8,  max: 6.8  },
    turbidez: { min: 15,   max: 45   },
    temp:     { min: 22,   max: 28   },
  },
  // ── ZONAS NORMALES (Sin actividad minera significativa) ────────────────────
  {
    id: 'N1',
    nombre: 'Reserva Natural Salazar de las Palmas',
    tipo: 'normal',
    lat: 7.77, lng: -72.30,
    radio: 0.15,
    peso: 160,
    mercurio: { min: 0.00, max: 0.18 },
    ph:       { min: 6.5,  max: 7.8  },
    turbidez: { min: 2,    max: 18   },
    temp:     { min: 18,   max: 25   },
  },
  {
    id: 'N2',
    nombre: 'Cuenca Alta Río Zulia',
    tipo: 'normal',
    lat: 8.42, lng: -72.38,
    radio: 0.13,
    peso: 100,
    mercurio: { min: 0.00, max: 0.22 },
    ph:       { min: 6.8,  max: 7.5  },
    turbidez: { min: 1,    max: 12   },
    temp:     { min: 17,   max: 23   },
  },
  {
    id: 'N3',
    nombre: 'Zona Rural Pamplona',
    tipo: 'normal',
    lat: 7.55, lng: -72.20,
    radio: 0.10,
    peso: 70,
    mercurio: { min: 0.00, max: 0.28 },
    ph:       { min: 6.2,  max: 7.6  },
    turbidez: { min: 3,    max: 20   },
    temp:     { min: 14,   max: 20   },
  },
];

// ─────────────────────────────────────────────────────────────────────────────
// UTILIDADES
// ─────────────────────────────────────────────────────────────────────────────

/** Número aleatorio dentro del rango [min, max] con distribución gaussiana truncada */
function gaussianRand(mean, std) {
  let u = 0, v = 0;
  while (u === 0) u = Math.random();
  while (v === 0) v = Math.random();
  return mean + std * Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
}

function clamp(val, min, max) {
  return Math.min(Math.max(val, min), max);
}

function randInRange(min, max) {
  return min + Math.random() * (max - min);
}

function round(val, decimals) {
  return parseFloat(val.toFixed(decimals));
}

/** Fecha aleatoria dentro de los últimos 6 meses */
function randomDate() {
  const now   = new Date('2026-04-19T00:00:00');
  const sixMo = new Date(now);
  sixMo.setMonth(sixMo.getMonth() - 6);
  const ts = sixMo.getTime() + Math.random() * (now.getTime() - sixMo.getTime());
  return new Date(ts).toISOString();
}

/** Genera una posición dentro del radio del cluster usando distribución normal */
function positionInCluster(cluster) {
  const stdLat = cluster.radio * 0.5;
  const stdLng = cluster.radio * 0.5;
  return {
    lat: round(clamp(gaussianRand(cluster.lat, stdLat), 7.50, 8.50), 6),
    lng: round(clamp(gaussianRand(cluster.lng, stdLng), -73.00, -72.00), 6),
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. GENERACIÓN DE MEDICIONES AMBIENTALES (1000 registros)
// ─────────────────────────────────────────────────────────────────────────────

function generateMediciones() {
  const mediciones = [];
  let id = 1;

  for (const cluster of CLUSTERS) {
    for (let i = 0; i < cluster.peso; i++) {
      const pos = positionInCluster(cluster);

      // Valores químicos con leve correlación intra-cluster (misma fuente hídrica)
      const mercurio = round(clamp(
        gaussianRand(
          (cluster.mercurio.min + cluster.mercurio.max) / 2,
          (cluster.mercurio.max - cluster.mercurio.min) / 4
        ), cluster.mercurio.min, cluster.mercurio.max
      ), 4);

      const ph = round(clamp(
        gaussianRand(
          (cluster.ph.min + cluster.ph.max) / 2,
          (cluster.ph.max - cluster.ph.min) / 4
        ), cluster.ph.min, cluster.ph.max
      ), 2);

      const turbidez = round(clamp(
        gaussianRand(
          (cluster.turbidez.min + cluster.turbidez.max) / 2,
          (cluster.turbidez.max - cluster.turbidez.min) / 4
        ), cluster.turbidez.min, cluster.turbidez.max
      ), 1);

      const temperatura = round(clamp(
        gaussianRand(
          (cluster.temp.min + cluster.temp.max) / 2,
          (cluster.temp.max - cluster.temp.min) / 4
        ), cluster.temp.min, cluster.temp.max
      ), 1);

      // Nivel de alerta derivado
      let nivelAlerta = 'NORMAL';
      if (mercurio > 0.8 && ph < 6.0)        nivelAlerta = 'CRITICO';
      else if (mercurio > 0.5 || ph < 6.5)   nivelAlerta = 'ALERTA';
      else if (mercurio > 0.3 || ph < 7.0)   nivelAlerta = 'PRECAUCION';

      mediciones.push({
        id,
        device_id: `SENSOR-${cluster.id}-${String(i + 1).padStart(3, '0')}`,
        cluster_id: cluster.id,
        zona_nombre: cluster.nombre,
        tipo_zona: cluster.tipo,
        latitud: pos.lat,
        longitud: pos.lng,
        ph,
        mercurio_ppm: mercurio,
        turbidez_ntu: turbidez,
        temperatura_c: temperatura,
        nivel_alerta: nivelAlerta,
        fecha: randomDate(),
      });
      id++;
    }
  }

  // Mezcla los registros para que no queden agrupados por cluster
  return shuffleArray(mediciones);
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. GENERACIÓN DE DENUNCIAS (500 registros)
// Más denuncias en zonas contaminadas, menos en zonas limpias.
// Correlación espacial pero no perfecta (ruido realista).
// ─────────────────────────────────────────────────────────────────────────────

const DESCRIPCIONES_CONTAMINADA = [
  'Río con coloración amarilla y olor fuerte, posible uso de mercurio en dragas ilegales.',
  'Maquinaria pesada operando sin permisos ambientales en la orilla del río.',
  'Peces muertos en la quebrada, sospecha de contaminación por minería.',
  'Retroexcavadoras trabajando de noche en el caño sin señalización.',
  'Vertimiento de lodos con sustancias químicas directamente al río.',
  'Draga flotante operando en zona de reserva hídrica sin licencia.',
  'Vegetación muerta a lo largo del río por posible contaminación química.',
  'Campamento ilegal con equipos de procesamiento de oro cerca al río.',
  'Agua turbia y con residuos metálicos en el afluente principal.',
  'Minería de aluvión destruyendo lecho del río y riberas.',
];

const DESCRIPCIONES_NORMAL = [
  'Tala de árboles en zona de protección hídrica.',
  'Quema de vegetación cerca al nacimiento de agua.',
  'Vertimiento de residuos sólidos en quebrada.',
  'Movimiento sospechoso de maquinaria en zona boscosa.',
  'Construcción sin permiso cerca a cuerpo de agua.',
];

const ESTADOS = ['SIN_VALIDAR', 'PENDIENTE', 'VALIDADO', 'RECHAZADO'];

// Distribución de estados según tipo de zona
function randomEstado(tipo) {
  const r = Math.random();
  if (tipo === 'contaminada') {
    // Más validados y pendientes en zonas contaminadas (evidencia más clara)
    if (r < 0.35) return 'VALIDADO';
    if (r < 0.60) return 'PENDIENTE';
    if (r < 0.85) return 'SIN_VALIDAR';
    return 'RECHAZADO';
  } else if (tipo === 'transicion') {
    if (r < 0.20) return 'VALIDADO';
    if (r < 0.45) return 'PENDIENTE';
    if (r < 0.75) return 'SIN_VALIDAR';
    return 'RECHAZADO';
  } else {
    // Zonas normales: más rechazados y sin validar (menos evidencia)
    if (r < 0.10) return 'VALIDADO';
    if (r < 0.25) return 'PENDIENTE';
    if (r < 0.60) return 'SIN_VALIDAR';
    return 'RECHAZADO';
  }
}

// Distribución de denuncias por cluster (peso relativo)
const DENUNCIAS_POR_CLUSTER = {
  'C1': 130,   // zona más contaminada → más denuncias
  'C2': 110,
  'C3':  90,
  'T1':  60,   // transición → moderado
  'N1':  55,
  'N2':  35,
  'N3':  20,
};

function generateDenuncias() {
  const denuncias = [];
  let id = 1;

  for (const cluster of CLUSTERS) {
    const cantidad = DENUNCIAS_POR_CLUSTER[cluster.id];

    for (let i = 0; i < cantidad; i++) {
      // Posición: cerca al cluster pero con mayor dispersión que mediciones
      const clusterExpandido = { ...cluster, radio: cluster.radio * 1.3 };
      const pos = positionInCluster(clusterExpandido);

      const esContaminada = cluster.tipo === 'contaminada';
      const esTrans       = cluster.tipo === 'transicion';

      const descripciones = esContaminada
        ? DESCRIPCIONES_CONTAMINADA
        : esTrans
          ? [...DESCRIPCIONES_CONTAMINADA.slice(0, 4), ...DESCRIPCIONES_NORMAL]
          : DESCRIPCIONES_NORMAL;

      const desc = descripciones[Math.floor(Math.random() * descripciones.length)];
      const estado = randomEstado(cluster.tipo);

      denuncias.push({
        id,
        cluster_id: cluster.id,
        zona_nombre: cluster.nombre,
        tipo_zona: cluster.tipo,
        latitud: pos.lat,
        longitud: pos.lng,
        descripcion: desc,
        fecha: randomDate(),
        estado,
        usuario_id: Math.floor(randInRange(1, 150)),
        tiene_evidencia: Math.random() < (esContaminada ? 0.65 : 0.30),
      });
      id++;
    }
  }

  return shuffleArray(denuncias);
}

function buildDenunciaTitle(denuncia) {
  const datePart = new Date(denuncia.fecha).toISOString().slice(0, 10);
  return `Denuncia ${denuncia.cluster_id} - ${datePart} - #${denuncia.id}`;
}

function toMedicionesApi(mediciones) {
  return mediciones.map((m) => ({
    sensorType: DEFAULT_SENSOR_TYPE,
    payload: {
      deviceId: m.device_id,
      latitude: m.latitud,
      longitude: m.longitud,
      ph: m.ph,
      mercury: m.mercurio_ppm,
      turbidity: m.turbidez_ntu,
      temperature: m.temperatura_c,
      timestamp: m.fecha,
    },
    meta: {
      sourceId: m.id,
      clusterId: m.cluster_id,
      zonaNombre: m.zona_nombre,
      tipoZona: m.tipo_zona,
      nivelAlerta: m.nivel_alerta,
    },
  }));
}

function toDenunciasApi(denuncias) {
  return denuncias.map((d) => ({
    post: {
      title: buildDenunciaTitle(d),
      content: d.descripcion,
      userId: d.usuario_id,
      type: 'REPORT',
      latitude: d.latitud,
      longitude: d.longitud,
      images: [],
    },
    workflow: {
      targetStatus: d.estado,
      validatorId: d.estado === 'VALIDADO' || d.estado === 'RECHAZADO' ? 1 : null,
    },
    meta: {
      sourceId: d.id,
      clusterId: d.cluster_id,
      zonaNombre: d.zona_nombre,
      tipoZona: d.tipo_zona,
      tieneEvidencia: d.tiene_evidencia,
      fecha: d.fecha,
    },
  }));
}

// ─────────────────────────────────────────────────────────────────────────────
// UTILIDAD: Mezcla aleatoria (Fisher-Yates)
// ─────────────────────────────────────────────────────────────────────────────
function shuffleArray(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

// ─────────────────────────────────────────────────────────────────────────────
// ESTADÍSTICAS DE VALIDACIÓN
// ─────────────────────────────────────────────────────────────────────────────
function printStats(mediciones, denuncias) {
  console.log('\n══════════════════════════════════════════════════');
  console.log('  ESTADÍSTICAS DE DATASETS GENERADOS');
  console.log('══════════════════════════════════════════════════');

  // Mediciones por tipo
  const porTipo = {};
  const alertas = {};
  for (const m of mediciones) {
    porTipo[m.tipo_zona]    = (porTipo[m.tipo_zona] || 0) + 1;
    alertas[m.nivel_alerta] = (alertas[m.nivel_alerta] || 0) + 1;
  }

  console.log(`\n[MEDICIONES AMBIENTALES] Total: ${mediciones.length}`);
  console.log('  Por tipo de zona:');
  for (const [t, c] of Object.entries(porTipo)) console.log(`    ${t.padEnd(12)}: ${c}`);
  console.log('  Por nivel de alerta:');
  for (const [a, c] of Object.entries(alertas)) console.log(`    ${a.padEnd(12)}: ${c}`);

  const mercAvg = mediciones.reduce((s, m) => s + m.mercurio_ppm, 0) / mediciones.length;
  const phAvg   = mediciones.reduce((s, m) => s + m.ph,          0) / mediciones.length;
  console.log(`  Mercurio promedio : ${mercAvg.toFixed(4)} ppm`);
  console.log(`  pH promedio       : ${phAvg.toFixed(2)}`);

  // Denuncias por estado
  const porEstado = {};
  const denPorTipo = {};
  for (const d of denuncias) {
    porEstado[d.estado]       = (porEstado[d.estado] || 0) + 1;
    denPorTipo[d.tipo_zona]   = (denPorTipo[d.tipo_zona] || 0) + 1;
  }

  console.log(`\n[DENUNCIAS] Total: ${denuncias.length}`);
  console.log('  Por estado:');
  for (const [e, c] of Object.entries(porEstado)) console.log(`    ${e.padEnd(12)}: ${c}`);
  console.log('  Por tipo de zona:');
  for (const [t, c] of Object.entries(denPorTipo)) console.log(`    ${t.padEnd(12)}: ${c}`);

  // Correlación simple
  const contDen = denuncias.filter(d => d.tipo_zona === 'contaminada').length;
  const contPct = ((contDen / denuncias.length) * 100).toFixed(1);
  const contMed = mediciones.filter(m => m.tipo_zona === 'contaminada').length;
  console.log(`\n[CORRELACIÓN]`);
  console.log(`  % denuncias en zonas contaminadas  : ${contPct}%`);
  console.log(`  % mediciones en zonas contaminadas : ${((contMed / mediciones.length) * 100).toFixed(1)}%`);
  console.log(`  Ratio denuncias/mediciones (cont.) : ${(contDen / contMed).toFixed(2)}`);
  console.log('══════════════════════════════════════════════════\n');
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN
// ─────────────────────────────────────────────────────────────────────────────
const mediciones = generateMediciones();
const denuncias  = generateDenuncias();
const medicionesApi = toMedicionesApi(mediciones);
const denunciasApi = toDenunciasApi(denuncias);

printStats(mediciones, denuncias);

const outputDir = path.join(__dirname, '..', 'data');
fs.mkdirSync(outputDir, { recursive: true });

fs.writeFileSync(
  path.join(outputDir, 'mediciones_ambientales.json'),
  JSON.stringify(mediciones, null, 2)
);
fs.writeFileSync(
  path.join(outputDir, 'denuncias.json'),
  JSON.stringify(denuncias, null, 2)
);
fs.writeFileSync(
  path.join(outputDir, 'mediciones_api.json'),
  JSON.stringify(medicionesApi, null, 2)
);
fs.writeFileSync(
  path.join(outputDir, 'denuncias_api.json'),
  JSON.stringify(denunciasApi, null, 2)
);

console.log(`Archivos generados en: ${outputDir}`);
console.log(`  mediciones_ambientales.json  (${mediciones.length} registros)`);
console.log(`  denuncias.json               (${denuncias.length} registros)`);
console.log(`  mediciones_api.json          (${medicionesApi.length} registros)`);
console.log(`  denuncias_api.json           (${denunciasApi.length} registros)`);
