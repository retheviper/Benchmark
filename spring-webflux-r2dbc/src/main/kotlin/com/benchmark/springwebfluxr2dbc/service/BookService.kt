package com.benchmark.springwebfluxr2dbc.service

import com.benchmark.springwebfluxr2dbc.domain.AuthorInventoryReport
import com.benchmark.springwebfluxr2dbc.domain.BookDetail
import com.benchmark.springwebfluxr2dbc.domain.BookSummary
import com.benchmark.springwebfluxr2dbc.domain.LargePayload
import com.benchmark.springwebfluxr2dbc.domain.PayloadSummary
import com.benchmark.springwebfluxr2dbc.domain.SmallPayload
import com.benchmark.springwebfluxr2dbc.repo.BookRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

@Service
class BookService(private val bookRepository: BookRepository) {
    fun findBookById(id: Long): Mono<BookDetail> = bookRepository.findBookById(id)
    fun listBooks(limit: Int): Flux<BookSummary> = bookRepository.listBooks(limit)
    fun searchBooks(term: String, limit: Int): Flux<BookSummary> = bookRepository.searchBooks(term, limit)
    fun smallPayload(): SmallPayload = SmallPayload("spring-webflux-r2dbc", 1, OffsetDateTime.now().toString(), listOf("spring", "webflux", "r2dbc", "jooq"))
    fun largePayload(limit: Int): Mono<LargePayload> =
        bookRepository.listLargePayloadItems(limit).collectList().map { items ->
            LargePayload(
                service = "spring-webflux-r2dbc",
                generatedAt = OffsetDateTime.now().toString(),
                summary = PayloadSummary(items.size, items.sumOf { it.id + it.priceCents + it.stock.toLong() }),
                items = items,
            )
        }
    fun authorInventory(limit: Int): Flux<AuthorInventoryReport> = bookRepository.authorInventory(limit)
}

