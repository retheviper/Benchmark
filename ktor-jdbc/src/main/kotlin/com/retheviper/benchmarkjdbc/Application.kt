package com.retheviper.benchmarkjdbc

import com.retheviper.benchmarkjdbc.config.BenchmarkConfig
import com.retheviper.benchmarkjdbc.db.BenchmarkDatabase
import com.retheviper.benchmarkjdbc.di.benchmarkModule
import com.retheviper.benchmarkjdbc.routes.registerRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8083
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    val benchmarkConfig = BenchmarkConfig.from(environment.config)
    if (benchmarkConfig.logging.httpCalls) install(CallLogging)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; explicitNulls = false; prettyPrint = false })
    }
    install(Koin) { modules(benchmarkModule(benchmarkConfig)) }
    val database by inject<BenchmarkDatabase>()
    monitor.subscribe(ApplicationStarted) { database.initialize() }
    monitor.subscribe(ApplicationStopping) { database.close() }
    routing { registerRoutes() }
}
