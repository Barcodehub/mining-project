/**
 * GENERADOR DE DATASETS SIMULADOS - VERSIÓN CIENTÍFICAMENTE HONESTA
 * Sistema de Monitoreo Ambiental y Denuncias de Minería Ilegal
 * Norte de Santander, Colombia
 *
 * FILOSOFÍA:
 * - Solo generamos datos que un sensor o laboratorio MEDIRÍA
 * - NO inventamos metilmercurio ni mercurio en peces
 * - Campos opcionales (hg_sediment, methylmercury, hg_fish) solo en 30% de registros
 *   de zonas contaminadas (simulando análisis de laboratorio)
 * - Correlaciones realistas entre variables medibles
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
  // Contaminated
  {
    id: 'C1', nombre: 'Cuenca Río Catatumbo', tipo: 'contaminada', lat: 8.08, lng: -72.82,
    radio: 0.18, peso: 220,
    hg_total: { min: 10, max: 80 },        // µg/L
    ph:       { min: 4.5,  max: 6.0  },
    turbidez: { min: 45,   max: 120  },
    temp:     { min: 26,   max: 32   },
    redox:    { min: -150, max: 50 },     // mV
    organic_carbon: { min: 2.5, max: 6.0 }, // %
    dissolved_oxygen: { min: 0.5, max: 4.0 }, // mg/L
    labAnalysisProb: 0.35  // 35% de registros tienen datos de laboratorio
  },
  {
    id: 'C2', nombre: 'Serranía del Perijá Sur', tipo: 'contaminada', lat: 7.72, lng: -72.65,
    radio: 0.14, peso: 180,
    hg_total: { min: 8, max: 60 },
    ph:       { min: 4.8,  max: 6.0  },
    turbidez: { min: 38,   max: 95   },
    temp:     { min: 24,   max: 30   },
    redox:    { min: -120, max: 80 },
    organic_carbon: { min: 2.0, max: 5.0 },
    dissolved_oxygen: { min: 1.0, max: 5.0 },
    labAnalysisProb: 0.30
  },
  {
    id: 'C3', nombre: 'Zona Minera Tibú-El Tarra', tipo: 'contaminada', lat: 8.35, lng: -72.72,
    radio: 0.12, peso: 150,
    hg_total: { min: 5, max: 40 },
    ph:       { min: 4.8,  max: 6.2  },
    turbidez: { min: 30,   max: 80   },
    temp:     { min: 25,   max: 31   },
    redox:    { min: -100, max: 100 },
    organic_carbon: { min: 1.5, max: 4.5 },
    dissolved_oxygen: { min: 1.5, max: 5.5 },
    labAnalysisProb: 0.25
  },

  // Transition
  { id: 'T1', nombre: 'Área Periurbana Cúcuta Norte', tipo: 'transicion', lat: 7.93, lng: -72.51,
    radio: 0.10, peso: 120,
    hg_total: { min: 1.0, max: 8.0 },
    ph:       { min: 5.5,  max: 7.0  },
    turbidez: { min: 15,   max: 45   },
    temp:     { min: 22,   max: 28   },
    redox:    { min: -50,  max: 150 },
    organic_carbon: { min: 1.0, max: 3.0 },
    dissolved_oxygen: { min: 2.0, max: 6.0 },
    labAnalysisProb: 0.15
  },

  // Normal
  { id: 'N1', nombre: 'Reserva Natural Salazar de las Palmas', tipo: 'normal', lat: 7.77, lng: -72.30,
    radio: 0.15, peso: 160,
    hg_total: { min: 0.01, max: 0.5 },
    ph:       { min: 6.5,  max: 7.8  },
    turbidez: { min: 2,    max: 18   },
    temp:     { min: 18,   max: 25   },
    redox:    { min: 100,  max: 300 },
    organic_carbon: { min: 0.2, max: 1.5 },
    dissolved_oxygen: { min: 6.0, max: 9.0 },
    labAnalysisProb: 0.05
  },
  { id: 'N2', nombre: 'Cuenca Alta Río Zulia', tipo: 'normal', lat: 8.42, lng: -72.38,
    radio: 0.13, peso: 100,
    hg_total: { min: 0.01, max: 0.8 },
    ph:       { min: 6.8,  max: 7.5  },
    turbidez: { min: 1,    max: 12   },
    temp:     { min: 17,   max: 23   },
    redox:    { min: 100,  max: 350 },
    organic_carbon: { min: 0.3, max: 1.8 },
    dissolved_oxygen: { min: 6.0, max: 8.5 },
    labAnalysisProb: 0.05
  },
  { id: 'N3', nombre: 'Zona Rural Pamplona', tipo: 'normal', lat: 7.55, lng: -72.20,
    radio: 0.10, peso: 70,
    hg_total: { min: 0.01, max: 1.2 },
    ph:       { min: 6.2,  max: 7.6  },
    turbidez: { min: 3,    max: 20   },
    temp:     { min: 14,   max: 20   },
    redox:    { min: 80,   max: 300 },
    organic_carbon: { min: 0.4, max: 2.0 },
    dissolved_oxygen: { min: 5.5, max: 8.0 },
    labAnalysisProb: 0.05
  },
];

// ─────────────────────────────────────────────────────────────────────────────
// UTILIDADES
// ─────────────────────────────────────────────────────────────────────────────

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

function randomDate() {
  const now   = new Date('2026-04-19T00:00:00');
  const sixMo = new Date(now);
  sixMo.setMonth(sixMo.getMonth() - 6);
  const ts = sixMo.getTime() + Math.random() * (now.getTime() - sixMo.getTime());
  return new Date(ts).toISOString();
}

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
      const hasLabData = Math.random() < cluster.labAnalysisProb;

      // Fisicoquímicas y geoquímica con correlaciones (no independientes)
      const hgTotal = round(clamp(
        gaussianRand((cluster.hg_total.min + cluster.hg_total.max) / 2,
          (cluster.hg_total.max - cluster.hg_total.min) / 4),
        cluster.hg_total.min, cluster.hg_total.max), 4); // µg/L

      const ph = round(clamp(
        gaussianRand((cluster.ph.min + cluster.ph.max) / 2,
          (cluster.ph.max - cluster.ph.min) / 4),
        cluster.ph.min, cluster.ph.max), 2);

      const turbidez = round(clamp(
        gaussianRand((cluster.turbidez.min + cluster.turbidez.max) / 2,
          (cluster.turbidez.max - cluster.turbidez.min) / 4),
        cluster.turbidez.min, cluster.turbidez.max), 1);

      const temperatura = round(clamp(
        gaussianRand((cluster.temp.min + cluster.temp.max) / 2,
          (cluster.temp.max - cluster.temp.min) / 4),
        cluster.temp.min, cluster.temp.max), 1);

      const redox = round(clamp(
        gaussianRand((cluster.redox.min + cluster.redox.max) / 2,
          (cluster.redox.max - cluster.redox.min) / 4),
        cluster.redox.min, cluster.redox.max), 1); // mV

      const organicCarbon = round(clamp(
        gaussianRand((cluster.organic_carbon.min + cluster.organic_carbon.max) / 2,
          (cluster.organic_carbon.max - cluster.organic_carbon.min) / 4),
        cluster.organic_carbon.min, cluster.organic_carbon.max), 3); // %

      const dissolvedOxygen = round(clamp(
        gaussianRand((cluster.dissolved_oxygen.min + cluster.dissolved_oxygen.max) / 2,
          (cluster.dissolved_oxygen.max - cluster.dissolved_oxygen.min) / 4),
        cluster.dissolved_oxygen.min, cluster.dissolved_oxygen.max), 2); // mg/L

      // Partición agua-sedimento: en zonas contaminadas más Hg va al sedimento
      let fractionSediment = cluster.tipo === 'contaminada' ? randInRange(0.8, 0.95)
                            : cluster.tipo === 'transicion' ? randInRange(0.6, 0.8)
                            : randInRange(0.4, 0.65);

      // Hg disuelto en agua (µg/L) - SIEMPRE se mide
      const hgDissolved = round(Math.max(0, hgTotal * (1 - fractionSediment)), 6);

      // Datos de laboratorio (OPCIONALES - solo si hasLabData = true)
      let hgSediment = null;
      let methylmercury = null;
      let hgFish = null;

      if (hasLabData) {
        // Hg en sedimento (mg/kg)
        const sedimentNorm = cluster.tipo === 'contaminada' ? 0.02 : cluster.tipo === 'transicion' ? 0.01 : 0.005;
        hgSediment = round(hgTotal * fractionSediment * sedimentNorm, 4);

        // Metilmercurio (µg/L) - medido por laboratorio
        const phFactor = 1 + Math.max(0, (5.5 - ph)) * 0.25;
        const redoxFactor = 1 + Math.max(0, -redox) / 200;
        const baseCoef = 0.00001;
        methylmercury = round(baseCoef * hgTotal * organicCarbon * phFactor * redoxFactor, 6);

        // Hg en peces (mg/kg) - medido por laboratorio
        const BAF = 100000;
        hgFish = round(methylmercury * BAF / 1000.0, 4);
      }

      // Determinar fuente de datos
      const dataSource = hasLabData ? 'MIXTO' : 'SENSOR';

      mediciones.push({
        id,
        device_id: `SENSOR-${cluster.id}-${String(i + 1).padStart(3, '0')}`,
        cluster_id: cluster.id,
        zona_nombre: cluster.nombre,
        tipo_zona: cluster.tipo,
        latitud: pos.lat,
        longitud: pos.lng,
        // Fisicoquímicas básicas (SIEMPRE presentes)
        ph,
        turbidez_ntu: turbidez,
        temperatura_c: temperatura,
        // Geoquímicas avanzadas (SIEMPRE presentes en este generador)
        redox,
        dissolved_oxygen: dissolvedOxygen,
        organic_carbon: organicCarbon,
        // Mercurio en agua (SIEMPRE presentes)
        hg_dissolved: hgDissolved,
        hg_total: hgTotal,
        // Mercurio en otros compartimentos (OPCIONALES - solo laboratorio)
        hg_sediment: hgSediment,
        methylmercury: methylmercury,
        hg_fish: hgFish,
        // Metadatos
        data_source: dataSource,
        fecha: randomDate(),
      });
      id++;
    }
  }

  return shuffleArray(mediciones);
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. GENERACIÓN DE DENUNCIAS (500 registros)
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

function randomEstado(tipo) {
  const r = Math.random();
  if (tipo === 'contaminada') {
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
    if (r < 0.10) return 'VALIDADO';
    if (r < 0.25) return 'PENDIENTE';
    if (r < 0.60) return 'SIN_VALIDAR';
    return 'RECHAZADO';
  }
}

const DENUNCIAS_POR_CLUSTER = {
  'C1': 130, 'C2': 110, 'C3': 90, 'T1': 60, 'N1': 55, 'N2': 35, 'N3': 20,
};

function generateDenuncias() {
  const denuncias = [];
  let id = 1;

  for (const cluster of CLUSTERS) {
    const cantidad = DENUNCIAS_POR_CLUSTER[cluster.id];

    for (let i = 0; i < cantidad; i++) {
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
      clusterId: m.cluster_id,
      ph: m.ph,
      hg_dissolved: m.hg_dissolved,
      hg_total: m.hg_total,
      hg_sediment: m.hg_sediment,
      methylmercury: m.methylmercury,
      hg_fish: m.hg_fish,
      turbidity: m.turbidez_ntu,
      temperature: m.temperatura_c,
      redox: m.redox,
      dissolved_oxygen: m.dissolved_oxygen,
      organic_carbon: m.organic_carbon,
      timestamp: m.fecha,
    },
    meta: {
      sourceId: m.id,
      clusterId: m.cluster_id,
      zonaNombre: m.zona_nombre,
      tipoZona: m.tipo_zona,
      dataSource: m.data_source,
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
      clusterId: d.cluster_id,
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

function shuffleArray(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

// ─────────────────────────────────────────────────────────────────────────────
// ESTADÍSTICAS
// ─────────────────────────────────────────────────────────────────────────────
function printStats(mediciones, denuncias) {
  console.log('\n══════════════════════════════════════════════════');
  console.log('  ESTADÍSTICAS DE DATASETS GENERADOS');
  console.log('══════════════════════════════════════════════════');

  const porTipo = {};
  const porFuente = {};
  for (const m of mediciones) {
    porTipo[m.tipo_zona] = (porTipo[m.tipo_zona] || 0) + 1;
    porFuente[m.data_source] = (porFuente[m.data_source] || 0) + 1;
  }

  console.log(`\n[MEDICIONES AMBIENTALES] Total: ${mediciones.length}`);
  console.log('  Por tipo de zona:');
  for (const [t, c] of Object.entries(porTipo)) console.log(`    ${t.padEnd(12)}: ${c}`);
  console.log('  Por fuente de datos:');
  for (const [f, c] of Object.entries(porFuente)) console.log(`    ${f.padEnd(12)}: ${c}`);

  const conLabData = mediciones.filter(m => m.data_source === 'MIXTO').length;
  console.log(`  Con datos de laboratorio: ${conLabData} (${((conLabData/mediciones.length)*100).toFixed(1)}%)`);

  const hgTotalAvg = mediciones.reduce((s, m) => s + (m.hg_total || 0), 0) / mediciones.length;
  const phAvg = mediciones.reduce((s, m) => s + m.ph, 0) / mediciones.length;
  console.log(`  Hg total promedio : ${hgTotalAvg.toFixed(4)} µg/L`);
  console.log(`  pH promedio       : ${phAvg.toFixed(2)}`);

  const porEstado = {};
  for (const d of denuncias) porEstado[d.estado] = (porEstado[d.estado] || 0) + 1;

  console.log(`\n[DENUNCIAS] Total: ${denuncias.length}`);
  console.log('  Por estado:');
  for (const [e, c] of Object.entries(porEstado)) console.log(`    ${e.padEnd(12)}: ${c}`);
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
