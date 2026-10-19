# Sistema de Gestión de Minería API - VERSIÓN CIENTÍFICAMENTE HONESTA

Plataforma basada en microservicios desarrollada con **Spring Boot (Java 17)** y estructurada bajo **Arquitectura Hexagonal**. Permite el registro de actividad minera ilegal, datos de sensores ambientales y notificaciones distribuidas.

---

## 🎯 FILOSOFÍA DEL SISTEMA

### Principio fundamental: **NO INVENTAR DATOS**

Este sistema implementa un enfoque científicamente riguroso:

1. **Solo almacenamos datos MEDIDOS** — Si un campo no viene del sensor o laboratorio, queda en `null`
2. **Scoring defensivo** — Los algoritmos de alerta solo usan campos presentes, nunca asumen
3. **Dos tipos de alerta**:
   - **IMPACTO**: Evidencia directa de contaminación (mercurio medido supera norma)
   - **RIESGO**: Condiciones ambientales peligrosas (pH bajo, redox bajo, etc.)
4. **Transparencia total** — Cada medición incluye metadatos de calidad (`dataCompleteness`, `missingFields`, `dataSource`)

---

## Arquitectura de Microservicios

El sistema está diseñado en 3 microservicios principales:

1. **mining-management (Core / Puerto 8082)**: 
   - Microservicio principal y orquestador.
   - Gestión de reportes de denuncias ciudadanas y foros.
   - Orquestador lógico (API Gateway Proxy) para consumir el microservicio de autenticación.
   - Mapping, serialización y almacenamiento dinámico de datos de sensores ambientales.
   - **Sistema de alertas multidimensional** con scoring de riesgo humano, ecológico y geoquímico.
   - Almacenamiento seguro por S3.
   - *Actúa como Emisor de eventos asíncronos en Kafka.*

2. **mining-users (Auth / Puerto 8081)**:
   - Microservicio dedicado puramente a Identidad y Acceso (IAM).
   - Emisión de JWT y gestión de control de roles (RBAC).

3. **mining-audit-notifications (Event-Driven / Puerto 8083)**:
   - Consumidor puro orientado a eventos.
   - Recibe la carga de crear Notificaciones mediante MongoDb.
   - Recibe la carga de generar trazabilidad de auditoría inmutable (Append-only) utilizando PostgreSQL.

---

## 📊 Modelo de Datos Ambiental

### Campos OBLIGATORIOS (siempre presentes)
- `deviceId`, `latitude`, `longitude`, `timestamp`
- `ph`, `temperature`, `turbidity`
- **Al menos UNO de**: `hg_total` o `hg_dissolved`

### Campos OPCIONALES (pueden ser null)
- **Geoquímicas avanzadas**: `redox`, `dissolved_oxygen`, `organic_carbon`
- **Mercurio en otros compartimentos**: `hg_sediment`, `hg_fish`, `methylmercury`

### Metadatos de Calidad
- `alertLevel`: NORMAL | PRECAUCION | ALERTA | CRITICO
- `alertType`: IMPACTO | RIESGO | MIXTO | DESCONOCIDO
- `dataCompleteness`: Porcentaje de campos opcionales presentes (0.0 - 1.0)
- `missingFields`: Lista CSV de campos críticos faltantes
- `dataSource`: SENSOR | LABORATORIO | MIXTO

### Unidades
- `hg_dissolved`, `hg_total`, `methylmercury`: µg/L
- `hg_sediment`, `hg_fish`: mg/kg
- `redox`: mV
- `organic_carbon`: % peso seco
- `dissolved_oxygen`: mg/L
- `turbidity`: NTU
- `temperature`: °C

---

## Despliegue y Ejecución

### Levantar los contenedores
Todos los servicios de soporte han sido configurados a través de **Docker Compose**. En la raíz del proyecto, ejecuta:

```bash
docker-compose up -d
```

Esto levantará los siguientes servicios en segundo plano:
- **Zookeeper & Kafka**: Infraestructura asíncrona en el puerto `9092`.
- **PostgreSQL**: Base de datos SQL para auditoría en el puerto `5432`.
- **MongoDB**: Base de datos NoSQL para notificaciones en el puerto `27017`.
- **Prometheus**: Base de datos Time-Series en el puerto `9090`.
- **Grafana**: Dashboard de visualización en el puerto `4000`.
- **K6**: Herramienta de pruebas de carga configurada para ejecutar el script `load_test.js`.

### Generar y cargar datasets
1. Generar datasets:
   ```bash
   docker-compose run --rm mining-management node evaluacion/scripts/generate_datasets.js
   ```
2. Cargar datasets:
   ```bash
   docker-compose run --rm mining-management node evaluacion/scripts/load_datasets_api.js
   ```

### Ejecutar pruebas de carga
Para ejecutar las pruebas de carga con K6:
- Con resultados en un archivo JSON:
  ```bash
  docker-compose run --rm k6 --out json=results.json
  ```
- Con resultados enviados a InfluxDB para visualización en Grafana:
  ```bash
  docker-compose run --rm k6
  ```
- Sin guardar resultados:
  ```bash
  docker-compose run --rm k6 --no-summary
  ```
  - *Nota*: Para escenarios alto (500 VU) añadir "-e ESCENARIO=alto" al comando anterior.

---

## Observabilidad (Prometheus & Grafana)

La plataforma completa ha sido instrumentada con **Micrometer** y **Spring Boot Actuator** en los 3 microservicios generados.

- **Prometheus** está pre-configurado (`observability/prometheus.yml`) para interrogar localmente a los 3 microservicios cada 15 segundos sobre sus métricas `/actuator/prometheus`.
- **Grafana** permite visualizar el comportamiento interno de la JVM y rendimiento HTTP. 
  - *Acceso Grafana:* [http://localhost:4000](http://localhost:4000)
  - *Credenciales por defecto:* admin / admin
  - *Setup:* La base de datos `Prometheus` ya fue añadida como proveedor de forma autoconfigurada por *Provisioning*. Puedes agregar dashboards como el **JVM Micrometer**.

---

## Referencia Rápida de Endpoints

### 1. Autenticación (Vía Core)
- `POST /api/v1/auth/login` - Proxifica hacia `mining-users` devolviendo la sesión JWT.

### 2. CORE: Reportes & Foro
- `POST /api/v1/posts` - Registro (REPORT/FORUM).
- `GET /api/v1/posts` - Paginado.
- `GET /api/v1/reports` - Múltiples Query Params dinámicos para los reportes especiales de minería ilegal.
- Flujos de estado: `PATCH /api/v1/reports/{id}/request-validation` y `PATCH /api/v1/reports/{id}/status`.

### 3. CORE: Environmental Data
- `POST /api/v1/adapters` - Creación de plantilla/mapping JSON.
- `POST /api/v1/environmental-data` - Inserción de datos de sensores (acepta campos opcionales en null).
- `GET /api/v1/environmental-data` - Consulta con filtros avanzados:
  - Por mercurio: `?hgDisssolvedMin=1.0&hgTotalMin=2.0`
  - Por nivel de alerta: `?alertLevel=CRITICO`
  - Por tipo de alerta: `?alertType=IMPACTO` o `?alertType=RIESGO`
  - Por geoquímica: `?phMin=4.0&phMax=6.0&redoxMax=100`
- `GET /api/v1/environmental-data/aggregate` - Cálculo de parámetros estadísticos.

### 4. CORE: Analytics
- `GET /api/v1/analytics/clusters` - Métricas ambientales agrupadas por cluster (SAI).
- `GET /api/v1/analytics/reports` - Métricas de reportes ciudadanos agrupadas por cluster (CRI).
- `GET /api/v1/analytics/fusion` - Combinación de SAI y CRI por cluster.
- `GET /api/v1/analytics/correlation` - Coeficiente de correlación Spearman entre SAI y CRI.

### 5. CORE: Storage AWS
- `POST /api/storage/upload` - Sube archivos normales con MultipartFile.
- `POST /api/storage/upload-with-thumbnail` - Generación automática de miniaturas.

### 6. EVENT-DRIVEN: Audit y Notification
- `GET /api/v1/notifications/{userId}` - Bandejas de Notificaciones universales.
- `GET /api/v1/audit` - Consulta de Logs inmutables de auditorías de todo el sistema.

---

## 🔧 Configuración de Umbrales Ambientales

Los umbrales de alerta son **completamente configurables** sin recompilar el código.

### Archivo: `mining-management/src/main/resources/config-ambiental.yaml`

```yaml
ambiental:
  region: "Norte de Santander - Colombia"
  
  hg-disuelto:
    alerta: 1.0       # OMS agua potable: 1 µg/L
    critico: 2.0      # IDEAM cuerpos de agua: 2 µg/L
  
  hg-peces:
    alerta: 0.3       # FDA advisory
    critico: 0.5      # FDA action level / OMS
  
  ph:
    critico-bajo: 5.5 # Umbral metilación acelerada
  
  redox:
    critico-bajo: 0   # mV — ambiente reductor
  
  pesos-score:
    riesgo-humano: 0.45
    riesgo-ecologico: 0.35
    riesgo-geoquimico: 0.20
```

**Para cambiar el estándar regional**: Edita este archivo y reinicia el servicio. No se requiere recompilación.

---

## 📝 Ejemplos de Request/Response

### Ejemplo 1: Sensor Básico (solo campos obligatorios)

**Request:**
```json
POST /api/v1/environmental-data
{
  "sensorType": "multi_parameter_sensor",
  "payload": {
    "deviceId": "SENSOR-C1-001",
    "latitude": 8.076432,
    "longitude": -72.821957,
    "clusterId": "C1",
    "ph": 5.12,
    "hg_dissolved": 2.345678,
    "hg_total": 25.4321,
    "turbidity": 87.3,
    "temperature": 28.4,
    "redox": -85.0,
    "dissolved_oxygen": 2.14,
    "organic_carbon": 3.45,
    "timestamp": "2026-04-19T14:32:00Z"
  }
}
```

**Response:**
```json
{
  "id": 1042,
  "deviceId": "SENSOR-C1-001",
  "latitude": 8.076432,
  "longitude": -72.821957,
  "clusterId": "C1",
  "ph": 5.12,
  "hgDissolved": 2.345678,
  "hgTotal": 25.4321,
  "hgSediment": null,
  "hgFish": null,
  "methylmercury": null,
  "turbidity": 87.3,
  "temperature": 28.4,
  "redox": -85.0,
  "dissolvedOxygen": 2.14,
  "organicCarbon": 3.45,
  "humanRiskScore": 1.0,
  "ecologicalRiskScore": 0.8,
  "geochemicalRiskScore": 1.0,
  "totalRiskScore": 0.915,
  "alertLevel": "CRITICO",
  "alertType": "MIXTO",
  "dataCompleteness": 0.625,
  "missingFields": "hg_sediment,hg_fish,methylmercury",
  "dataSource": "SENSOR",
  "timestamp": "2026-04-19T14:32:00"
}
```

### Ejemplo 2: Con Datos de Laboratorio

**Request:**
```json
POST /api/v1/environmental-data
{
  "sensorType": "multi_parameter_sensor",
  "payload": {
    "deviceId": "SENSOR-C1-002",
    "latitude": 8.076432,
    "longitude": -72.821957,
    "clusterId": "C1",
    "ph": 5.12,
    "hg_dissolved": 2.345678,
    "hg_total": 25.4321,
    "hg_sediment": 0.8123,
    "methylmercury": 0.003456,
    "hg_fish": 0.3456,
    "turbidity": 87.3,
    "temperature": 28.4,
    "redox": -85.0,
    "dissolved_oxygen": 2.14,
    "organic_carbon": 3.45,
    "timestamp": "2026-04-19T14:32:00Z"
  }
}
```

**Response:**
```json
{
  "id": 1043,
  "deviceId": "SENSOR-C1-002",
  "latitude": 8.076432,
  "longitude": -72.821957,
  "clusterId": "C1",
  "ph": 5.12,
  "hgDissolved": 2.345678,
  "hgTotal": 25.4321,
  "hgSediment": 0.8123,
  "hgFish": 0.3456,
  "methylmercury": 0.003456,
  "turbidity": 87.3,
  "temperature": 28.4,
  "redox": -85.0,
  "dissolvedOxygen": 2.14,
  "organicCarbon": 3.45,
  "humanRiskScore": 1.0,
  "ecologicalRiskScore": 1.0,
  "geochemicalRiskScore": 1.0,
  "totalRiskScore": 1.0,
  "alertLevel": "CRITICO",
  "alertType": "IMPACTO",
  "dataCompleteness": 1.0,
  "missingFields": null,
  "dataSource": "MIXTO",
  "timestamp": "2026-04-19T14:32:00"
}
```

---

## Herramientas de Desarrollo

### Postman Collection Completa
Se incluye a nivel raíz el archivo `postman_collection.json`. Incluye:
- Ejemplos de sensor básico vs laboratorio completo
- Filtros por `alertType` (IMPACTO, RIESGO)
- Filtros por `alertLevel` (CRITICO, ALERTA)
- Todas las cabeceras, rutas con las llamadas asíncronas, simulación de autenticación proxy JWT

---

## 📚 Documentación Adicional

- **`evaluacion/ANALISIS_COMPLETO.md`**: Análisis técnico completo del sistema, datasets, pruebas de rendimiento y correlaciones estadísticas.
- **`mining-management/README.md`**: Documentación específica del microservicio core.
- **`mining-users/README.md`**: Documentación del microservicio de autenticación.
- **`mining-audit-notifications/README.md`**: Documentación del microservicio de eventos.

---

## 🎓 Rigor Científico

Este sistema fue diseñado para ser utilizado en investigación académica y publicaciones científicas. Por ello:

1. **No inventa datos** — Solo almacena lo medido
2. **Transparencia total** — Cada registro indica qué campos faltan
3. **Umbrales configurables** — Adaptable a diferentes regiones y normativas
4. **Scoring multidimensional** — Evalúa riesgo humano, ecológico y geoquímico por separado
5. **Distinción IMPACTO vs RIESGO** — Diferencia entre evidencia directa y condiciones peligrosas

---

## Licencia

MIT License - Ver archivo LICENSE para más detalles.
