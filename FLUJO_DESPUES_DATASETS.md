# Flujo después de cargar datasets

## Resumen rápido

Después de cargar los datasets (1000 mediciones + 500 denuncias), el flujo es:

1. **Levantar infraestructura de observabilidad** (Prometheus + Grafana via docker-compose)
2. **Ejecutar pruebas de carga con k6** (load_test.js simulará usuarios y medirá rendimiento)
3. **Importar dashboard en Grafana** (dashboard_mineria.json visualiza métricas en tiempo real)

---

## Paso 1: Levantar Infraestructura (Docker Compose)

En el raíz del proyecto (`C:\Users\BRAYAN\Desktop\ins\project-mineria`):

```powershell
docker-compose up prometheus grafana
```

Esto levanta:
- **Prometheus** (http://localhost:9090) - recolector de métricas
- **Grafana** (http://localhost:3000) - visualización de métricas

Espera a que ambos servicios estén listos (~30 segundos). Deberías ver en logs:
- `grafana_1 | GF_SECURITY_ADMIN_PASSWORD=admin`

---

## Paso 2: Ejecutar k6 (Pruebas de Carga)

`evaluacion/k6/load_test.js` es un script de **simulación de carga realista**. Genera tráfico HTTP contra tu backend (mining-management) con 3 escenarios:

| Escenario | VUs | Duración | Uso |
|-----------|-----|----------|-----|
| **bajo** | 50 | ~2 min | Desarrollo/prueba rápida |
| **medio** | 200 | ~7.5 min | Staging (recomendado) |
| **alto** | 500 | ~9.5 min | Producción/estrés |

**Ejecutar escenario medio (recomendado):**

```powershell
Set-Location "C:\Users\BRAYAN\Desktop\ins\project-mineria\evaluacion"
k6 run -e ESCENARIO=medio .\k6\load_test.js
```

Si tienes k6 instalado globalmente:
```powershell
k6 run -e ESCENARIO=medio ./evaluacion/k6/load_test.js
```

**Qué hace k6:**
- Simula 200 usuarios concurrentes (VUs)
- 40% consultan reportes
- 25% consultan mediciones
- 20% crean nuevas denuncias
- 10% envían mediciones de sensores
- 5% consultan agregados (dashboard)
- Mide latencia p50/p95/p99, error rate, throughput
- Emite métricas a Prometheus (automático en docker-compose)

**Salida esperada:**
```
     iterations...............: X completed
     vus_max..................: 200
     http_req_duration........: avg=387ms p(95)=891ms p(99)=1.430s
     http_reqs................: 64.620 total
     error_rate...............: 0.018 (1.8%)
```

---

## Paso 3: Importar Dashboard en Grafana

Mientras k6 se ejecuta (o después), abre Grafana en navegador:

**http://localhost:3000**
- Usuario: `admin`
- Contraseña: `admin`

### Importar dashboard:

1. Ir a **Dashboards** (lateral izquierdo)
2. Click en **Import**
3. Click en **Upload JSON file**
4. Seleccionar: `C:\Users\BRAYAN\Desktop\ins\project-mineria\evaluacion\grafana\dashboard_mineria.json`
5. Click **Import**

### Qué ves en el dashboard:

El archivo `dashboard_mineria.json` contiene **6 filas de paneles**:

| Fila | Paneles | Qué muestran |
|------|---------|------------|
| 1 | Stats | RPS, error rate, p95 latencia, % reportes validados, alertas críticas |
| 2 | Time Series | Latencia por percentil en el tiempo, throughput por endpoint |
| 3 | Gauges | p95 por endpoint (rojo/amarillo/verde visual) |
| 4 | Bar Charts | Denuncias por estado, mediciones por nivel de alerta |
| 5 | Time Series | Memoria JVM, hilos activos, CPU |
| 6 | Time Series / Stats | Kafka produce/consume rate, consumer lag |

Las métricas se actualizarán en tiempo real mientras k6 está corriendo.

---

## Archivos y qué son:

| Archivo | Tipo | Descripción |
|---------|------|------------|
| `evaluacion/k6/load_test.js` | Script de carga | Ejecutable con k6; simula usuarios reales contra tu backend |
| `evaluacion/grafana/dashboard_mineria.json` | Configuración JSON | Plantilla de dashboard; se importa en Grafana UI |
| `evaluacion/ANALISIS_COMPLETO.md` | Documentación | Análisis técnico y académico del sistema (referencia) |
| `docker-compose.yml` | Orquestación | Define servicios: Prometheus, Grafana, Kafka, PostgreSQL, Mongo, Zookeeper |

---

## Resumen de flujo completo:

```
1. docker-compose up prometheus grafana
   ↓
2. [espera 30s - Grafana + Prometheus listos]
   ↓
3. k6 run -e ESCENARIO=medio ./evaluacion/k6/load_test.js
   ↓
4. [mientras k6 corre, abre http://localhost:3000 en navegador]
   ↓
5. Importa dashboard_mineria.json en Grafana
   ↓
6. Observa métricas en tiempo real en dashboard
   ↓
7. [k6 termina, ve resumen de resultados en terminal]
   ↓
8. Revisa valores finales en Grafana para análisis post-test
```

---

## Comandos útiles:

```bash
# Escenario bajo (rápido)
k6 run -e ESCENARIO=bajo ./evaluacion/k6/load_test.js

# Escenario alto (estrés máximo)
k6 run -e ESCENARIO=alto ./evaluacion/k6/load_test.js

# Exportar resultados a JSON para análisis posterior
k6 run -e ESCENARIO=medio --out json=results.json ./evaluacion/k6/load_test.js

# Detener docker-compose (cuando termines)
docker-compose down
```

---

## Puertos en uso:

- **8080** → mining-management (backend)
- **8081** → mining-users (auth)
- **8082** → mining-audit-notifications
- **9090** → Prometheus (métricas)
- **3000** → Grafana (visualización) ← Abre esto
- **9092** → Kafka (mensajería)
- **27017** → MongoDB (notificaciones)
- **5432** → PostgreSQL (auditoría)

---

## Próximos pasos (opcional):

- Analizar el documento `ANALISIS_COMPLETO.md` para entender arquitectura, resultados esperados y limitaciones
- Ejecutar k6 con diferentes escenarios para comparar
- Tunar parámetros de carga en `load_test.js` si necesitas casos específicos
- Exportar datos de Prometheus para análisis de tendencias

