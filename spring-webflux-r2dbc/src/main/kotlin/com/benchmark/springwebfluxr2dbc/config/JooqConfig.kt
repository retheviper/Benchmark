package com.benchmark.springwebfluxr2dbc.config

import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.r2dbc.connection.R2dbcTransactionManager

@Configuration
class JooqConfig {
    @Bean
    fun dslContext(): DSLContext = DSL.using(SQLDialect.POSTGRES)

    @Bean
    fun databaseClient(connectionFactory: ConnectionFactory): DatabaseClient = DatabaseClient.create(connectionFactory)

    @Bean
    fun transactionManager(connectionFactory: ConnectionFactory): ReactiveTransactionManager =
        R2dbcTransactionManager(connectionFactory)

    @Bean
    fun transactionalOperator(transactionManager: ReactiveTransactionManager): TransactionalOperator =
        TransactionalOperator.create(transactionManager)
}

