package com.retheviper.benchmarkjdbc.service

import com.retheviper.benchmarkjdbc.model.AuthorInventoryReportDto
import com.retheviper.benchmarkjdbc.model.BookDetailDto
import com.retheviper.benchmarkjdbc.model.BookSummaryDto
import com.retheviper.benchmarkjdbc.model.LargePayloadDto
import com.retheviper.benchmarkjdbc.model.PayloadSummaryDto
import com.retheviper.benchmarkjdbc.model.SmallPayloadDto
import com.retheviper.benchmarkjdbc.repository.BookRepository
import java.time.OffsetDateTime

class BookService(private val repository: BookRepository) {
    fun findById(id: Long): BookDetailDto? = repository.findById(id)
    fun list(limit: Int): List<BookSummaryDto> = repository.list(limit.coerceIn(1, 500))
    fun search(query: String, limit: Int): List<BookSummaryDto> = repository.search(query, limit.coerceIn(1, 200))
    fun smallPayload() = SmallPayloadDto("ktor-jdbc", 1, OffsetDateTime.now().toString(), listOf("ktor", "jdbc", "exposed", "benchmark"))
    fun largePayload(limit: Int): LargePayloadDto {
        val items = repository.listLargePayloadItems(limit.coerceIn(50, 500))
        return LargePayloadDto("ktor-jdbc", OffsetDateTime.now().toString(), PayloadSummaryDto(items.size, items.sumOf { it.id + it.priceCents + it.stock.toLong() }), items)
    }
    fun authorInventory(limit: Int): List<AuthorInventoryReportDto> = repository.authorInventory(limit.coerceIn(10, 200))
}

