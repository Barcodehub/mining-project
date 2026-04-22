# Evaluacion - Datasets y Carga

Este modulo contiene:

- `scripts/generate_datasets.js`: genera datasets analiticos y API-ready.
- `scripts/load_datasets_api.js`: carga masiva de datasets API-ready a `mining-management`.

## Archivos generados

Al ejecutar el generador se crean estos archivos en `data/`:

- `mediciones_ambientales.json` (analitico)
- `denuncias.json` (analitico)
- `mediciones_api.json` (API-ready para `POST /api/v1/environmental-data`)
- `denuncias_api.json` (API-ready para `POST /api/v1/posts` y flujo de estados)

## Uso rapido

```bash
npm run generate:datasets
npm run load:datasets
```

## Variables de entorno del loader

- `MANAGEMENT_URL` (default: `http://localhost:8080`)
- `AUTH_EMAIL` (default: `admin@admin.com`)
- `AUTH_PASSWORD` (default: `admin`)
- `SENSOR_TYPE` (default: `multi_parameter_sensor`)
- `CREATE_ADAPTER` (`true`/`false`, default: `true`)
- `MEDICIONES_FILE` (default: `./data/mediciones_api.json`)
- `DENUNCIAS_FILE` (default: `./data/denuncias_api.json`)
- `MAX_RETRIES` (default: `2`)
- `REQUEST_TIMEOUT_MS` (default: `10000`)
- `REQUEST_DELAY_MS` (default: `0`)
- `DRY_RUN` (`true`/`false`, default: `false`)

Para limitar cantidad de registros usa argumento CLI (no variable de entorno):

`node scripts/load_datasets_api.js --max-items=10`

## Prueba local sin pegarle a la API

```bash
DRY_RUN=true node scripts/load_datasets_api.js --max-items=10
```

En Windows PowerShell:

```powershell
$env:DRY_RUN='true'
node scripts/load_datasets_api.js --max-items=10
```

