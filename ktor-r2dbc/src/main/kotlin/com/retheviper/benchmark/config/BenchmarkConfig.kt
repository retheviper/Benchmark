package com.retheviper.benchmark.config

import io.ktor.server.config.ApplicationConfig

data class BenchmarkConfig(
    val database: DatabaseConfig,
    val logging: LoggingConfig,
) {
    companion object {
        fun from(config: ApplicationConfig): BenchmarkConfig {
            return BenchmarkConfig(
                database = DatabaseConfig.from(config),
                logging = LoggingConfig.from(config),
            )
        }
    }
}

data class DatabaseConfig(
    val r2dbcUrl: String,
    val username: String,
    val password: String,
    val applicationName: String,
    val initialPoolSize: Int,
    val maxPoolSize: Int,
) {
    companion object {
        fun from(config: ApplicationConfig): DatabaseConfig {
            return DatabaseConfig(
                r2dbcUrl = readString(config, "benchmark.database.r2dbcUrl", "BENCHMARK_R2DBC_URL", "r2dbc:postgresql://localhost:5432/benchmark"),
                username = readString(config, "benchmark.database.username", "BENCHMARK_DB_USER", "benchmark"),
                password = readString(config, "benchmark.database.password", "BENCHMARK_DB_PASSWORD", "benchmark"),
                applicationName = readString(config, "benchmark.database.applicationName", "BENCHMARK_DB_APPLICATION_NAME", "ktor-r2dbc-benchmark"),
                initialPoolSize = readInt(config, "benchmark.database.initialPoolSize", "BENCHMARK_DB_INITIAL_POOL_SIZE", 16),
                maxPoolSize = readInt(config, "benchmark.database.maxPoolSize", "BENCHMARK_DB_MAX_POOL_SIZE", 64),
            )
        }
    }
}

data class LoggingConfig(
    val httpCalls: Boolean,
) {
    companion object {
        fun from(config: ApplicationConfig): LoggingConfig {
            return LoggingConfig(
                httpCalls = readBoolean(config, "benchmark.logging.httpCalls", "BENCHMARK_HTTP_CALL_LOGGING", false),
            )
        }
    }
}

private fun readString(config: ApplicationConfig, path: String, env: String, defaultValue: String): String {
    return System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: config.propertyOrNull(path)?.getString()?.takeIf { it.isNotBlank() }
        ?: defaultValue
}

private fun readInt(config: ApplicationConfig, path: String, env: String, defaultValue: Int): Int {
    return System.getenv(env)?.toIntOrNull()
        ?: config.propertyOrNull(path)?.getString()?.toIntOrNull()
        ?: defaultValue
}

private fun readBoolean(config: ApplicationConfig, path: String, env: String, defaultValue: Boolean): Boolean {
    return System.getenv(env)?.toBooleanStrictOrNull()
        ?: config.propertyOrNull(path)?.getString()?.toBooleanStrictOrNull()
        ?: defaultValue
}
