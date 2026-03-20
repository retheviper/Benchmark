package com.retheviper.benchmarkjdbc.config

import io.ktor.server.config.ApplicationConfig

data class BenchmarkConfig(val database: DatabaseConfig, val logging: LoggingConfig) {
    companion object {
        fun from(config: ApplicationConfig): BenchmarkConfig = BenchmarkConfig(DatabaseConfig.from(config), LoggingConfig.from(config))
    }
}

data class DatabaseConfig(val jdbcUrl: String, val username: String, val password: String, val maxPoolSize: Int) {
    companion object {
        fun from(config: ApplicationConfig): DatabaseConfig = DatabaseConfig(
            readString(config, "benchmark.database.jdbcUrl", "BENCHMARK_JDBC_URL"),
            readString(config, "benchmark.database.username", "BENCHMARK_DB_USER"),
            readString(config, "benchmark.database.password", "BENCHMARK_DB_PASSWORD"),
            readInt(config, "benchmark.database.maxPoolSize", "BENCHMARK_DB_MAX_POOL_SIZE"),
        )
    }
}

data class LoggingConfig(val httpCalls: Boolean) {
    companion object {
        fun from(config: ApplicationConfig): LoggingConfig =
            LoggingConfig(readBoolean(config, "benchmark.logging.httpCalls", "BENCHMARK_HTTP_CALL_LOGGING"))
    }
}

private fun readString(config: ApplicationConfig, path: String, env: String): String =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: config.propertyOrNull(path)?.getString()
    ?: when (path) {
        "benchmark.database.jdbcUrl" -> "jdbc:postgresql://localhost:5432/benchmark?ApplicationName=ktor-jdbc-benchmark"
        "benchmark.database.username" -> "benchmark"
        "benchmark.database.password" -> "benchmark"
        else -> error("Property $path not found")
    }

private fun readInt(config: ApplicationConfig, path: String, env: String): Int =
    System.getenv(env)?.toIntOrNull() ?: config.propertyOrNull(path)?.getString()?.toIntOrNull()
    ?: when (path) {
        "benchmark.database.maxPoolSize" -> 32
        else -> error("Property $path not found")
    }

private fun readBoolean(config: ApplicationConfig, path: String, env: String): Boolean =
    System.getenv(env)?.toBooleanStrictOrNull() ?: config.propertyOrNull(path)?.getString()?.toBooleanStrictOrNull()
    ?: when (path) {
        "benchmark.logging.httpCalls" -> false
        else -> error("Property $path not found")
    }
