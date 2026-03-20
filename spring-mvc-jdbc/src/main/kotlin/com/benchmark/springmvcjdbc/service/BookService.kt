package com.benchmark.springmvcjdbc.service

import com.benchmark.springmvcjdbc.domain.AuthorInventoryReport
import com.benchmark.springmvcjdbc.domain.BookDetail
import com.benchmark.springmvcjdbc.domain.BookSummary
import com.benchmark.springmvcjdbc.domain.LargePayload
import com.benchmark.springmvcjdbc.domain.PayloadSummary
import com.benchmark.springmvcjdbc.domain.SmallPayload
import com.benchmark.springmvcjdbc.repo.BookRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class BookService(private val bookRepository: BookRepository) {

    fun findBookById(id: Long): BookDetail =
        bookRepository.findBookById(id) ?: throw NoSuchElementException("Book $id not found")

    fun listBooks(limit: Int): List<BookSummary> = bookRepository.listBooks(limit)

    fun searchBooks(term: String, limit: Int): List<BookSummary> =
        bookRepository.searchBooks(term, limit)

    fun smallPayload(): SmallPayload = SmallPayload(
        service = "spring-mvc-jdbc",
        version = 1,
        now = OffsetDateTime.now().toString(),
        tags = listOf("spring", "mvc", "jdbc", "benchmark"),
    )

    fun largePayload(limit: Int): LargePayload {
        val items = bookRepository.listLargePayloadItems(limit)
        return LargePayload(
            service = "spring-mvc-jdbc",
            generatedAt = OffsetDateTime.now().toString(),
            summary = PayloadSummary(
                itemCount = items.size,
                checksum = items.sumOf { it.id + it.priceCents + it.stock.toLong() },
            ),
            items = items,
        )
    }

    fun authorInventory(limit: Int): List<AuthorInventoryReport> = bookRepository.authorInventory(limit)
}
