package com.retheviper.benchmark

import com.retheviper.benchmark.config.BenchmarkConfig
import com.retheviper.benchmark.db.BenchmarkDatabaseInitializer
import com.retheviper.benchmark.db.BenchmarkDatabase
import com.retheviper.benchmark.di.benchmarkModule
import com.retheviper.benchmark.routes.registerRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.install
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin
import org.koin.ktor.ext.inject

fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .apply {
            addShutdownHook {
                BenchmarkDatabase.close()
            }
        }
        .start(wait = true)
}

fun Application.module() {
    val benchmarkConfig = BenchmarkConfig.from(environment.config)

    if (benchmarkConfig.logging.httpCalls) {
        install(CallLogging)
    }

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                prettyPrint = false
            }
        )
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(cause.message ?: "Internal Server Error", status = io.ktor.http.HttpStatusCode.InternalServerError)
        }
    }

    install(Koin) {
        modules(benchmarkModule(benchmarkConfig))
    }

    val initializer by inject<BenchmarkDatabaseInitializer>()

    monitor.subscribe(ApplicationStarted) {
        initializer.initialize()
    }

    monitor.subscribe(ApplicationStopping) {
        BenchmarkDatabase.close()
    }

    routing {
        registerRoutes()
    }
}
