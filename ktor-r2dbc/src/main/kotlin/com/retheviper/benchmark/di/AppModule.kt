package com.retheviper.benchmark.di

import com.retheviper.benchmark.config.BenchmarkConfig
import com.retheviper.benchmark.db.BenchmarkDatabaseInitializer
import com.retheviper.benchmark.repository.BookRepository
import com.retheviper.benchmark.repository.CheckoutRepository
import com.retheviper.benchmark.service.BookService
import com.retheviper.benchmark.service.CheckoutService
import org.koin.dsl.module

fun benchmarkModule(config: BenchmarkConfig) = module {
    single { config }
    single { BenchmarkDatabaseInitializer(get()) }
    single { BookRepository() }
    single { CheckoutRepository() }
    single { BookService(get()) }
    single { CheckoutService(get()) }
}
