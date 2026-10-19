# Datos v2 (Fase 3)

Todos los archivos se generan con scripts y una semilla fija (`20261019`). Nada se edita a mano, salvo la transcripción de CORPONOR, que se documenta abajo.

## Datos reales (`evaluacion/real/`)

| Archivo | Origen | Registros | Script |
|---|---|---|---|
| `processed/cdmb_api.json` | CDMB, datos.gov.co `9fpa-27t2` (CC BY-SA 4.0) | 334 (88 con Hg/CN/As) | `real/scripts/prepare_cdmb.py` |
| `processed/cdmb_stations.csv` | Ídem, una fila por estación con municipio (GADM 4.1) y categoría minera | 113 estaciones | ídem |
| `processed/cdmb_transformations.json` | Registro de transformaciones: T1 EPSG:3116→4326; T2 As en µg/L→mg/L (30 valores) | — | ídem |
| `processed/corponor_2025_transcribed.csv` | CORPONOR, informes IR-25-08 (río Zulia) e IR-25-01 (río Pamplonita), tablas de metales y fisicoquímica, **transcritas a mano** y verificadas con `pdftotext -table` | 11 | — |
| `processed/corponor_api.json` | Ídem, en formato de API (EPSG:9377→4326) | 11 | `real/scripts/prepare_corponor.py` |
| `processed/dicar_municipios.csv` | Policía Nacional (DICAR), datos.gov.co `4y5w-y5sj` (CC BY-SA 4.0), jul-2024 a ago-2026 | 67 municipios | `real/scripts/prepare_dicar.py` |
| `adapters/*.json` | Mapeos de las fuentes heterogéneas (CDMB y CORPONOR) al esquema canónico, con conversión de unidades | 2 | — |

Categoría minera de cada estación de la CDMB (criterio geográfico, independiente de los valores medidos):
- `DISTRITO_MINERO`: estación en los municipios de California o Vetas (Rincon-Vasquez et al., 2025; Espitia-Pérez et al., 2026).
- `AGUAS_ABAJO`: río Vetas fuera del distrito y cauce principal del río Suratá.
- `OTRO`: el resto.

## Datos sintéticos (`evaluacion/data/v2/`, `evaluacion/synthetic/generator.py`)

- **`mediciones_sinteticas.json`** (1.000 mediciones, clústeres SIM-A…SIM-G):
  - Cada medición es un **bootstrap estratificado de una observación real** con perturbación multiplicativa: σ_log = 0.15 en concentraciones y ±0.1 en pH. Los valores "<LD" se conservan.
  - Estrato H = distrito minero y aguas abajo (39 observaciones reales). Estrato B = resto, CDMB y CORPONOR (60).
  - Cada clúster tiene una verdad de terreno conocida, p ∈ {0.85, 0.70, 0.55, 0.40, 0.25, 0.10, 0.00} (fracción del estrato H).
  - Los nombres son abstractos a propósito: no representan lugares reales.
- **`denuncias_{aligned,null,noisy10,noisy20,noisy30,noisy50}.json`** (500 reportes cada uno):
  - `aligned`: intensidad y validación crecen con p.
  - `null`: mismos conteos y estados, permutados entre clústeres.
  - `noisyR`: aligned con un R % de reportes reasignados a un clúster al azar.
- **`clusters_ground_truth.json`**: p de cada clúster y CRI esperado por escenario (mismos pesos que la API).
- **`inyeccion_eventos.json`** (3.564 variantes; `synthetic/injection.py`): para cada una de las 99 observaciones reales con metales, se cambia un solo parámetro (Hg, CN, As, pH u OD) a valores alrededor de sus umbrales. La verdad esperada la da `synthetic/oracle.py`, un oráculo normativo **independiente del código Java** que lee los mismos umbrales.

## Carga para k6 (`evaluacion/k6/data/`)

- `payload_pool.json`: 1.000 sintéticos en formato IoT canónico (semilla 20261020), 334 filas CDMB y 11 CORPONOR **en su formato original**. Son tres fuentes heterogéneas bajo carga.
- `adapters.json`: adapters que k6 registra en `setup()`.

## Cargador

`evaluacion/scripts/load_api.py` (reemplaza a `load_datasets_api.js`, que validaba el esquema v1 y rechazaba los "<LD"). Guarda cada respuesta con el índice del registro de entrada.
