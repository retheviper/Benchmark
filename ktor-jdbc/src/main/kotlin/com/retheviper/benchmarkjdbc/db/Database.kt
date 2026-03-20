package com.retheviper.benchmarkjdbc.db

import com.retheviper.benchmarkjdbc.config.BenchmarkConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database

class BenchmarkDatabase(private val config: BenchmarkConfig) {
    private lateinit var dataSource: HikariDataSource
    fun initialize() {
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = config.database.jdbcUrl
            username = config.database.username
            password = config.database.password
            maximumPoolSize = config.database.maxPoolSize
            minimumIdle = config.database.maxPoolSize
        })
        Database.connect(dataSource)
    }
    fun close() { if (::dataSource.isInitialized) dataSource.close() }
}

