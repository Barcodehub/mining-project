# Evaluacion - Datasets y Carga

Este modulo contiene:

- `scripts/generate_datasets.js`: genera datasets analíticos y API-ready.
- `scripts/load_datasets_api.js`: carga masiva de datasets API-ready a `mining-management`.

## Archivos generados

Al ejecutar el generador se crean estos archivos en `data/`:

- `mediciones_ambientales.json` (analítico)
- `denuncias.json` (analítico)
- `mediciones_api.json` (API-ready para `POST /api/v1/environmental-data`)
- `denuncias_api.json` (API-ready para `POST /api/v1/posts` y flujo de estados)

## Uso con Docker

### Generar datasets
```bash
docker-compose run --rm mining-management node evaluacion/scripts/generate_datasets.js
```

### Cargar datasets
```bash
docker-compose run --rm mining-management node evaluacion/scripts/load_datasets_api.js
```

### Variables de entorno del loader

- `MANAGEMENT_URL` (default: `http://localhost:8082` en host / `http://mining-management:8082` dentro de Docker)
- `AUTH_EMAIL` (default: `admin@admin.com`)
- `AUTH_PASSWORD` (default: `Admin123`)
- `SENSOR_TYPE` (default: `multi_parameter_sensor`)
- `CREATE_ADAPTER` (`true`/`false`, default: `true`)
- `MEDICIONES_FILE` (default: `./data/mediciones_api.json`)
- `DENUNCIAS_FILE` (default: `./data/denuncias_api.json`)
- `MAX_RETRIES` (default: `2`)
- `REQUEST_TIMEOUT_MS` (default: `10000`)
- `REQUEST_DELAY_MS` (default: `0`)
- `DRY_RUN` (`true`/`false`, default: `false`)

Para limitar cantidad de registros usa argumento CLI (no variable de entorno):

```bash
docker-compose run --rm mining-management node evaluacion/scripts/load_datasets_api.js --max-items=10
```

### Prueba local sin pegarle a la API

```bash
docker-compose run --rm -e DRY_RUN=true mining-management node evaluacion/scripts/load_datasets_api.js --max-items=10
```
