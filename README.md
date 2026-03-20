# Benchmark

This repository is a Kotlin benchmark workspace for comparing several JVM web stack combinations against the same PostgreSQL dataset and the same HTTP scenarios.

It is designed for stack-level benchmarking, not just framework benchmarking. Each result includes the full request path:

- web framework
- serialization
- DI
- SQL / ORM layer
- driver
- connection pool

## Stacks

The workspace currently contains five benchmark targets:

- `spring-mvc-jdbc`
  Spring Boot MVC, Jackson, jOOQ, JDBC, HikariCP
- `spring-mvc-jdbc` with virtual threads enabled
  Same application, different threading mode
- `spring-webflux-r2dbc`
  Spring Boot WebFlux, Jackson, jOOQ SQL builder, R2DBC PostgreSQL, R2DBC pool
- `ktor-jdbc`
  Ktor, kotlinx.serialization, Koin, Exposed JDBC, HikariCP
- `ktor-r2dbc`
  Ktor, kotlinx.serialization, Koin, Exposed R2DBC, R2DBC PostgreSQL

Supporting modules:

- `benchmark-runner`
  Kotlin CLI that runs the benchmark scenarios and generates JSON and HTML reports
- `db/init`
  Shared PostgreSQL schema and seed data

## Versions

- Gradle `9.4.1`
- Kotlin `2.3.20`
- Spring Boot `4.0.4`
- Ktor `3.4.1`
- Koin `4.2.0`
- Exposed `1.1.1`
- jOOQ `3.20.11`
- PostgreSQL JDBC `42.7.10`
- R2DBC PostgreSQL `1.1.1.RELEASE`

## Repository Layout

```text
Benchmark/
├── benchmark-runner/
├── db/
│   └── init/
├── ktor-jdbc/
├── ktor-r2dbc/
├── spring-mvc-jdbc/
├── spring-webflux-r2dbc/
└── scripts/
```

## Prerequisites

- JDK 21
- Docker and Docker Compose
- A machine with enough CPU and memory to run the app and PostgreSQL locally

## Quick Start

Build everything:

```bash
./gradlew build
```

Start PostgreSQL:

```bash
./scripts/start-postgres.sh
```

Run a single application manually:

```bash
./scripts/run-spring.sh
./scripts/run-ktor.sh
```

These two helper scripts are the original quick-start scripts for:

- Spring MVC JDBC on `http://localhost:8080`
- Ktor R2DBC on `http://localhost:8081`

For the newer targets, run Gradle directly:

```bash
./gradlew :spring-webflux-r2dbc:bootRun
./gradlew :ktor-jdbc:run
```

Default ports:

- Spring MVC JDBC: `8080`
- Ktor R2DBC: `8081`
- Spring WebFlux R2DBC: `8082`
- Ktor JDBC: `8083`

## Benchmark Runner

Run the benchmark runner directly:

```bash
./scripts/run-benchmarks.sh --warmup-seconds=3 --measure-seconds=10 --concurrency=64
```

By default, the runner supports:

- warmup duration
- measurement duration
- concurrency
- target server list
- scenario filtering
- PostgreSQL metrics collection

Example:

```bash
./gradlew :benchmark-runner:run --args="\
--warmup-seconds=3 \
--measure-seconds=10 \
--concurrency=128 \
--targets=spring-mvc-jdbc@http://localhost:8080,ktor-r2dbc@http://localhost:8081 \
--scenarios=health,bookById,checkoutDistributed"
```

## Matrix Scripts

Run a single target with a fresh PostgreSQL instance:

```bash
./scripts/run-matrix.sh spring-mvc ./tmp/spring-mvc 128
./scripts/run-matrix.sh spring-webflux ./tmp/spring-webflux 128
./scripts/run-matrix.sh ktor-jdbc ./tmp/ktor-jdbc 128
./scripts/run-matrix.sh ktor-r2dbc ./tmp/ktor-r2dbc 128
```

Run the full five-stack comparison used in the latest report:

```bash
./scripts/run-five-stack-comparison.sh
```

This script runs:

- Spring MVC JDBC with platform threads
- Spring MVC JDBC with virtual threads
- Spring WebFlux R2DBC
- Ktor JDBC
- Ktor R2DBC

at:

- concurrency `128`
- concurrency `256`

and then generates a consolidated HTML summary.

## Scenarios

The benchmark runner currently uses these scenarios:

| Scenario | Type | Endpoint | Purpose |
| --- | --- | --- | --- |
| `health` | non-DB | `GET /health` | liveness endpoint |
| `smallJson` | read | `GET /api/books/bench/small-json` | small JSON serialization |
| `largeJson` | read | `GET /api/books/bench/large-json?limit=200` | large payload serialization |
| `bookById` | read | `GET /api/books/{id}` | point read by primary key |
| `bookList` | read | `GET /api/books?limit=50` | moderate list read |
| `bookList500` | read | `GET /api/books?limit=500` | large list read |
| `bookSearch` | read | `GET /api/books/search?q=Book&limit=20` | filtered search read |
| `aggregateReport` | read | `GET /api/books/bench/aggregate-report?limit=50` | aggregate-style read |
| `checkoutCreate` | write | `POST /api/checkouts` | random checkout transaction |
| `checkoutHotspot` | write | `POST /api/checkouts` | repeated checkout on the same book row |
| `checkoutDistributed` | write | `POST /api/checkouts` | distributed write workload |

Example checkout request body:

```json
{
  "bookId": 123,
  "customerName": "Benchmark User",
  "customerEmail": "bench@example.com",
  "quantity": 1
}
```

Important note for `checkoutHotspot`:

- it is intentionally contention-heavy
- it quickly reaches stock depletion
- high `req/s` can include many fast failures
- always interpret it together with failure count

## Reports

Generated reports are written under:

- JSON: `benchmark-runner/build/reports/benchmarks/**/*.json`
- HTML: `benchmark-runner/build/reports/benchmarks/**/*.html`

Important report locations:

- Per-run benchmark reports:
  `benchmark-runner/build/reports/benchmarks/...`
- Five-stack comparison summary:
  `benchmark-runner/build/reports/benchmarks/five-stack/five-stack-summary.html`
- Earlier matrix summary:
  `benchmark-runner/build/reports/benchmarks/matrix/matrix-summary.html`

Checked-in summary entry points:

- Five-stack summary:
  [`docs/reports/five-stack-summary.html`](docs/reports/five-stack-summary.html)
- Earlier matrix summary:
  [`docs/reports/matrix-summary.html`](docs/reports/matrix-summary.html)

## PostgreSQL Metrics

The benchmark runner also collects PostgreSQL metrics from:

- `pg_stat_statements`
- `pg_stat_database`

The Docker Compose setup enables:

- `pg_stat_statements`
- I/O timing
- query ID computation

This allows the HTML report to include:

- cache hit ratio
- transaction count
- block read and write time
- top SQL statements

## Interpretation Notes

- This repository measures complete stack combinations, not isolated framework overhead.
- Threading model comparisons are currently most meaningful inside the Spring MVC JDBC target.
- Ktor JDBC was added to separate Ktor framework cost from the Exposed + R2DBC path.
- Spring WebFlux R2DBC uses jOOQ as a SQL builder over `DatabaseClient`, not generated reactive jOOQ repositories.
- Aggregate scenarios are useful for comparison, but you should still inspect implementation differences before treating them as perfectly equivalent.

## Current High-Level Takeaways

Based on the latest local five-stack run in this repository:

- Spring WebFlux R2DBC is very strong on non-DB and small JSON paths.
- Spring MVC remains the strongest overall baseline for DB-heavy reads and most write scenarios.
- Ktor JDBC materially outperforms Ktor R2DBC on several read-heavy scenarios.
- Virtual threads do not automatically win over platform threads in this workload.

Use the HTML reports for the current measured numbers instead of treating these points as universal rules.

## Representative Results

The latest five-stack summary was generated from:

- `concurrency = 128`
- `concurrency = 256`
- fresh PostgreSQL container per run

Representative numbers at `concurrency = 256`:

| Scenario | 1st | 2nd | 3rd |
| --- | --- | --- | --- |
| `health` | Spring WebFlux R2DBC `25,782 req/s` | Spring MVC platform `24,939 req/s` | Ktor JDBC `23,455 req/s` |
| `smallJson` | Spring WebFlux R2DBC `29,220 req/s` | Ktor R2DBC `28,015 req/s` | Ktor JDBC `27,853 req/s` |
| `bookById` | Spring MVC virtual `14,903 req/s` | Spring MVC platform `14,901 req/s` | Spring WebFlux R2DBC `12,023 req/s` |
| `bookList` | Spring MVC virtual `9,574 req/s` | Spring MVC platform `9,553 req/s` | Ktor JDBC `5,773 req/s` |
| `largeJson` | Spring MVC platform `4,011 req/s` | Spring MVC virtual `3,980 req/s` | Ktor JDBC `2,741 req/s` |
| `checkoutCreate` | Spring MVC platform `5,227 req/s` | Spring MVC virtual `4,911 req/s` | Spring WebFlux R2DBC `4,493 req/s` |
| `checkoutDistributed` | Spring MVC virtual `5,126 req/s` | Spring MVC platform `4,772 req/s` | Spring WebFlux R2DBC `4,532 req/s` |

Interpretation:

- Spring WebFlux currently leads the lightweight non-DB paths.
- Spring MVC remains the strongest baseline for most DB-heavy read and write scenarios.
- Ktor JDBC is a useful midpoint: it narrows the gap significantly versus Ktor R2DBC on DB reads.
- `checkoutHotspot` is intentionally omitted from the summary table above because the failure rate dominates the result and raw throughput can be misleading.

## Reproducing the Latest Comparison

To reproduce the latest five-stack summary:

```bash
./gradlew build
./scripts/run-five-stack-comparison.sh
```

Then open:

```text
benchmark-runner/build/reports/benchmarks/five-stack/five-stack-summary.html
```

Or open the HTML report directly from the repository checkout:

- Generated report:
  `benchmark-runner/build/reports/benchmarks/five-stack/five-stack-summary.html`
- Checked-in snapshot:
  [`docs/reports/five-stack-summary.html`](docs/reports/five-stack-summary.html)

## License

No license file has been added in this workspace yet. Add one before publishing or reusing the code outside internal evaluation.
