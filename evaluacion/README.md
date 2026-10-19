# Evaluation: data, experiments and analysis

Everything reported in the article is produced by the scripts in this folder from versioned inputs.
Nothing is edited by hand except the CORPONOR transcription, which is documented in
[`real/README.md`](real/README.md) and [`data/v2/README.md`](data/v2/README.md).

Requirements: Docker Desktop (Compose v2), Java 17, Python 3.12 with `pip install -r evaluacion/requirements.txt`,
and the GADM 4.1 level-2 boundaries of Colombia (`gadm41_COL_2.json`, from https://gadm.org; not redistributed).
All commands run from the repository root.

## 1. Real data (`real/`)

| Source | Raw (unmodified) | Script | Processed |
|---|---|---|---|
| CDMB, datos.gov.co `9fpa-27t2` (334 records, 113 stations) | `real/raw/cdmb_9fpa-27t2_2026-10-19.json` | `real/scripts/prepare_cdmb.py --gadm <path>` | `cdmb_api.json`, `cdmb_stations.csv`, `cdmb_transformations.json` |
| CORPONOR 2025, Zulia IR-25-08 and Pamplonita IR-25-01 (11 stations) | `real/raw/corponor_*.pdf` | `real/scripts/prepare_corponor.py` | `corponor_api.json` (from `corponor_2025_transcribed.csv`) |
| Policía Nacional (DICAR), datos.gov.co `4y5w-y5sj` | `real/raw/dicar_4y5w-y5sj_santanderes_2026-10-19.json` | `real/scripts/prepare_dicar.py` | `dicar_municipios.csv` |

`real/adapters/` holds the field mappings that let the API ingest the CDMB and CORPONOR rows in their
original formats. SHA-256 hashes of the raw files are listed in `real/README.md`.

## 2. Synthetic data with known ground truth (`synthetic/`, `data/v2/`)

```bash
python evaluacion/synthetic/generator.py    # 1,000 measurements (SIM-A..G) + report scenarios
python evaluacion/synthetic/injection.py    # 3,564 injected events checked by synthetic/oracle.py
```

Measurements are stratified bootstraps of real observations; `oracle.py` is a normative oracle written
independently of the Java code. Seeds are fixed (`20261019`, k6 pool `20261020`). Details in
[`data/v2/README.md`](data/v2/README.md).

## 3. Experiments (`run_experiments.py`)

Each run starts from a clean Docker environment (`compose down -v` / `up --build`). Build the service jars
first (`./gradlew bootJar` in each service).

```bash
python evaluacion/run_experiments.py functional --system v2   # real data, injection, field fidelity
python evaluacion/run_experiments.py functional --system v1   # same inputs against the previous version
python evaluacion/run_experiments.py v3                       # sensor-citizen co-localization scenarios
python evaluacion/run_experiments.py perf --reps 3 --scenarios bajo,medio,alto   # k6, 50/200/500 VU
```

The v1 baseline needs a worktree of tag `v1-paper` next to the repository with its jars built:
`git worktree add --detach ../mineria-v1 v1-paper` (see `compose/docker-compose.v1.yml`).

Additional end-to-end checks against a running stack:

```bash
python evaluacion/scripts/verify_e2e.py     # alert path: ingestion → Kafka → audit and notification
python evaluacion/scripts/verify_rbac.py    # role-based access and validator taken from the token
```

Raw outputs go to `results/<date>/`. The article uses `results/2026-10-19/` (functional v1/v2, V3 scenarios,
performance runs with Prometheus snapshots and container statistics, the 1,000-replicate Monte Carlo,
`rbac_check.txt`, logs and an environment description per experiment).

## 4. Analysis (`analysis/`)

```bash
python evaluacion/analysis/perf_summary.py                          # latency and throughput per scenario
python evaluacion/analysis/make_all.py --mc 1000 --gadm <path>      # tables T1–T9, figures 2–5, key figures
python evaluacion/analysis/make_fig1.py                             # architecture diagram (Fig. 1)
```

`analysis/scoring_py.py` is a Python replica of the classifier; it reproduces all 5,254 classifications
returned by the Java service and is used for the Monte Carlo study and the sensitivity analysis. Tables
and figures are written to `articulo/` (created on demand, not versioned).

## Load test (`k6/`)

`k6/load_test_v2.js` mixes 40% report queries, 30% measurement queries, 10% report creation, 10% ingestion,
5% aggregates and 5% analytics. Ingestion payloads come from `k6/data/payload_pool.json`: 1,000 synthetic
records in canonical IoT format plus 334 CDMB and 11 CORPONOR rows in their original formats, ingested
through the adapters in `k6/data/adapters.json`. Alert counts come from the server metrics
(`environmental.alerts.published`, `alert.e2e.latency`), not from the client.
