package com.retheviper.benchmark.db

import com.retheviper.benchmark.config.BenchmarkConfig
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import java.net.URI

object BenchmarkDatabase {
    lateinit var database: R2dbcDatabase
        private set

    private var connectionFactory: ConnectionFactory? = null

    fun initialize(config: BenchmarkConfig) {
        val endpoint = PostgresEndpoint.parse(config.database.r2dbcUrl)
        connectionFactory = ConnectionFactories.get(
            ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "pool")
                .option(ConnectionFactoryOptions.PROTOCOL, "postgresql")
                .option(ConnectionFactoryOptions.HOST, endpoint.host)
                .option(ConnectionFactoryOptions.PORT, endpoint.port)
                .option(ConnectionFactoryOptions.DATABASE, endpoint.database)
                .option(ConnectionFactoryOptions.USER, config.database.username)
                .option(ConnectionFactoryOptions.PASSWORD, config.database.password)
                .option(Option.valueOf("applicationName"), config.database.applicationName)
                .option(Option.valueOf("initialSize"), config.database.initialPoolSize)
                .option(Option.valueOf("maxSize"), config.database.maxPoolSize)
                .build(),
        )
        database = R2dbcDatabase.connect(
            connectionFactory!!,
            R2dbcDatabaseConfig.Builder().apply {
                setUrl(config.database.r2dbcUrl)
                connectionFactoryOptions = ConnectionFactoryOptions.builder()
                    .option(ConnectionFactoryOptions.DRIVER, "pool")
                    .option(ConnectionFactoryOptions.PROTOCOL, "postgresql")
                    .option(ConnectionFactoryOptions.HOST, endpoint.host)
                    .option(ConnectionFactoryOptions.PORT, endpoint.port)
                    .option(ConnectionFactoryOptions.DATABASE, endpoint.database)
                    .option(ConnectionFactoryOptions.USER, config.database.username)
                    .option(ConnectionFactoryOptions.PASSWORD, config.database.password)
                    .option(Option.valueOf("applicationName"), config.database.applicationName)
                    .option(Option.valueOf("initialSize"), config.database.initialPoolSize)
                    .option(Option.valueOf("maxSize"), config.database.maxPoolSize)
                    .build()
            },
        )
    }

    fun close() {
        connectionFactory = null
    }
}

class BenchmarkDatabaseInitializer(
    private val config: BenchmarkConfig,
) {
    fun initialize() {
        BenchmarkDatabase.initialize(config)
    }
}

private data class PostgresEndpoint(
    val host: String,
    val port: Int,
    val database: String,
) {
    companion object {
        fun parse(r2dbcUrl: String): PostgresEndpoint {
            val normalized = r2dbcUrl.removePrefix("r2dbc:")
            val uri = URI(normalized)
            return PostgresEndpoint(
                host = uri.host ?: "localhost",
                port = if (uri.port == -1) 5432 else uri.port,
                database = uri.path.removePrefix("/"),
            )
        }
    }
}
