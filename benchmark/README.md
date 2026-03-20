# Five-Stack Benchmark

This folder contains the benchmark summary for five different JVM web stack configurations, all measured against the same PostgreSQL database and the same HTTP scenarios.

## Overview

This benchmark is intentionally a stack-level comparison rather than a narrow framework-only comparison. That means the measured path includes the HTTP server, JSON serialization, dependency injection, ORM or SQL layer, and the database driver.

The five compared configurations are:

- Spring MVC JDBC (platform threads)
- Spring MVC JDBC (virtual threads)
- Spring WebFlux R2DBC
- Ktor JDBC
- Ktor R2DBC

## Purpose

The benchmark was designed to answer the following questions:

- How well does Ktor work as a fully non-blocking stack now that Exposed 1.0 supports R2DBC?
- How does Spring MVC with virtual threads compare to Spring WebFlux with R2DBC?
- If Ktor JDBC and Ktor R2DBC are separated, how much of the difference comes from the framework itself versus the database access layer?
- How do these stacks behave across small JSON, large JSON, read-heavy, write-heavy, and contention-heavy scenarios?

## Stack Summary

### Spring MVC JDBC (platform)

- Tomcat
- Jackson
- jOOQ
- JDBC
- HikariCP
- platform threads

### Spring MVC JDBC (virtual)

- Tomcat
- Jackson
- jOOQ
- JDBC
- HikariCP
- virtual threads

### Spring WebFlux R2DBC

- Netty
- Jackson
- jOOQ SQL builder
- R2DBC PostgreSQL
- R2DBC pool

### Ktor JDBC

- Netty
- kotlinx.serialization
- Koin
- Exposed JDBC
- HikariCP

### Ktor R2DBC

- Netty
- kotlinx.serialization
- Koin
- Exposed R2DBC
- R2DBC PostgreSQL
- R2DBC pool

## Measurement Method

- PostgreSQL 17 runs in Docker
- Each run starts from a fresh database container with the same schema and seed data
- A shared Kotlin benchmark runner drives all HTTP scenarios
- Warmup: 1 second
- Measurement: 3 seconds
- Concurrency levels: 128 and 256
- Scenarios: `health`, `smallJson`, `largeJson`, `bookById`, `bookList`, `bookList500`, `bookSearch`, `aggregateReport`, `checkoutCreate`, `checkoutHotspot`, `checkoutDistributed`

Main pool settings:

- Spring MVC JDBC: HikariCP max 32
- Spring WebFlux R2DBC: pool max 32
- Ktor JDBC: HikariCP max 32
- Ktor R2DBC: pool max 32

## Results

The detailed comparison is available here:

- [five-stack-summary.html](./five-stack-summary.html)

The HTML summary includes:

- Technology stack overview for all five configurations
- Scenario list
- Side-by-side comparison tables for concurrency 128 and 256
- Scaling deltas from 128 to 256
- Per-scenario winners
- Notes and caveats
