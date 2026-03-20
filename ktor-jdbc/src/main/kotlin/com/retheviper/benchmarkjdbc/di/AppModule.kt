package com.retheviper.benchmarkjdbc.di

import com.retheviper.benchmarkjdbc.config.BenchmarkConfig
import com.retheviper.benchmarkjdbc.db.BenchmarkDatabase
import com.retheviper.benchmarkjdbc.repository.BookRepository
import com.retheviper.benchmarkjdbc.repository.CheckoutRepository
import com.retheviper.benchmarkjdbc.service.BookService
import com.retheviper.benchmarkjdbc.service.CheckoutService
import org.koin.dsl.module

fun benchmarkModule(config: BenchmarkConfig) = module {
    single { config }
    single { BenchmarkDatabase(get()) }
    single { BookRepository() }
    single { CheckoutRepository() }
    single { BookService(get()) }
    single { CheckoutService(get()) }
}

