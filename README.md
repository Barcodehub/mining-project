# Event-driven early warning middleware for mining-related water contamination

Source code, configuration, data and experiment scripts of the article

> B. A. Barco Cardenas, "An Event-Driven Early Warning Middleware for Mining-Related Water Contamination:
> Integrating Sensor, Laboratory, and Citizen Evidence in Northeastern Colombia," submitted to *IEEE Access*, 2026.

The middleware ingests heterogeneous evidence (in-situ probes, laboratory campaigns, citizen reports),
classifies every measurement **at ingestion** against Colombian legal limits and international aquatic-life
criteria, and delivers Alert/Critical events through Apache Kafka to an idempotent audit-and-notification
service that records the end-to-end latency.

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.23485438.svg)](https://doi.org/10.5281/zenodo.23485438)

Archived snapshot of tag `v2-paper`: [doi:10.5281/zenodo.23485438](https://doi.org/10.5281/zenodo.23485438) (all versions: [10.5281/zenodo.23485437](https://doi.org/10.5281/zenodo.23485437)).

| Git tag | State |
|---|---|
| `v2-paper` | System and results reported in the article (this branch) |
| `v1-paper` | Previous version, used as baseline in the article |

---

## Architecture

| Service | Port | Role |
|---|---|---|
| `mining-users` | 8081 | Identities, JWT issuance (user id + role) |
| `mining-management` | 8082 | Source adapters, data-quality gate, classifier, citizen reports, SAI/CRI analytics; Kafka producer |
| `mining-audit-notifications` | 8083 | Kafka consumer: append-only audit (PostgreSQL), notifications (MongoDB), latency metrics |

Infrastructure (Docker Compose): Kafka + ZooKeeper (Confluent 7.4.0, `9092`), PostgreSQL 15 (`15432`),
MongoDB 6 (`27017`), Prometheus (`9090`), Grafana (`4000`), InfluxDB (`8086`) and k6 0.54.0.
All services are Spring Boot 3.2 / Java 17 with a hexagonal (ports and adapters) structure.

Kafka topics: `environmental-alerts` (key = cluster; only Alert and Critical levels) and `mining-events`
(report and audit events).

## Alert model (v2)

The whole policy lives in
[`mining-management/src/main/resources/config-ambiental.yaml`](mining-management/src/main/resources/config-ambiental.yaml);
every threshold cites its source and can be changed without recompiling.

- **11 factors** in three dimensions: human (Res. 2115/2007 and Decree 1076/2015 for Hg, CN, As in water;
  EPA and Res. 122/2012 for Hg in fish), ecological (EPA aquatic-life CCC/CMC, CCME sediment guidelines,
  dissolved oxygen by thermal profile) and geochemical (pH, Eh).
- Strict exceedance; factor score 0 / 0.5 / 1. Censored values (`<LOD`) are kept as measurements and are
  conclusive when the limit of detection is at or below the Alert threshold.
- Composite score renormalized over the dimensions that can be evaluated (weights 0.45 / 0.35 / 0.20);
  levels Normal, Caution, Alert, Critical at 0.25 / 0.50 / 0.75; Indeterminate when nothing is conclusive.
- **Normative floor**: a Critical human-health exceedance guarantees at least Alert, and any exceedance at
  least Caution. Each measurement is typed Impact, Risk, Mixed, None or Unknown.
- Implausible values (e.g., dissolved oxygen > 20 mg/L) and source sentinels (e.g., CDMB `-999`) are
  flagged, never used.

A measurement is accepted when it has a device identifier, a plausible location and at least one
recognized parameter.

## Access control and reporter protection

| Operation | Roles |
|---|---|
| Register adapters, ingest measurements | `ADMINISTRADOR`, `INGENIERO` |
| Validate or reject citizen reports | `ADMINISTRADOR`, `MODERADOR` (validator taken from the token) |
| Query the audit trail | `ADMINISTRADOR` |
| File and read reports, read data and analytics | any authenticated user (`CLIENTE` included) |

Users without a validator role receive report locations generalized to a 0.01° grid and no user
identifiers; the audit trail stores report authors as HMAC-SHA256 pseudonyms (`PSEUDONYM_SECRET`); uploaded
images are re-encoded, which strips EXIF/GPS metadata. `evaluacion/scripts/verify_rbac.py` checks all of
this end to end.

## Quick start

```bash
# Build the three services (Java 17)
(cd mining-users && ./gradlew bootJar) && (cd mining-management && ./gradlew bootJar) \
  && (cd mining-audit-notifications && ./gradlew bootJar)

docker compose up -d --build
```

A seed administrator (`admin@admin.com` / `Admin123`) is created for local use; change it, the JWT secret
and `PSEUDONYM_SECRET` before any real deployment. Swagger UI: `http://localhost:8082/swagger-ui.html`.

Main endpoints (all under `mining-management`, port 8082):

- `POST /api/v1/auth/login` — JWT (proxied to `mining-users`)
- `POST /api/v1/adapters` — field mapping of a source format to the canonical schema
- `POST /api/v1/environmental-data` — ingest and classify; `GET /api/v1/environmental-data[/aggregate]`
- `POST /api/v1/posts`, `GET /api/v1/reports`, `PATCH /api/v1/reports/{id}/request-validation`,
  `PATCH /api/v1/reports/{id}/status`
- `GET /api/v1/analytics/{clusters|reports|fusion|correlation}` — SAI, CRI and Spearman ρ
- `GET /api/v1/notifications/{userId}`, `GET /api/v1/audit` — proxied to `mining-audit-notifications`

## Reproducing the article

See [`evaluacion/README.md`](evaluacion/README.md): real data preparation, synthetic data with known ground
truth, the experiment runner (each run in a clean Docker environment), and the scripts that produce every
table and figure from the raw results in `evaluacion/results/2026-10-19/`.

## Repository layout

```
mining-users/  mining-management/  mining-audit-notifications/   microservices
observability/                     Prometheus, Grafana and PostgreSQL init
evaluacion/real/                   real data (raw, processed, adapters, scripts)
evaluacion/synthetic/              generator, event injection and independent normative oracle
evaluacion/data/v2/                generated datasets (fixed seed)
evaluacion/k6/                     load test and payload pool
evaluacion/run_experiments.py      experiment runner
evaluacion/analysis/               tables, figures, Python replica of the classifier
evaluacion/results/2026-10-19/     raw results used in the article
```

## Data sources and licenses

- Code: [MIT](LICENSE).
- CDMB surface-water monitoring, datos.gov.co `9fpa-27t2`, and Policía Nacional operations against illegal
  mining, datos.gov.co `4y5w-y5sj`: CC BY-SA 4.0; derived files keep that license and attribution.
- CORPONOR 2025 basin reports (IR-25-08, IR-25-01): public documents, transcription documented in
  [`evaluacion/real/README.md`](evaluacion/real/README.md).
- GADM 4.1 municipal boundaries are used but not redistributed (see the GADM license).
