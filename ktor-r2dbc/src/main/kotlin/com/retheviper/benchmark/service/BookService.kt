package com.retheviper.benchmark.service

import com.retheviper.benchmark.model.AuthorInventoryReportDto
import com.retheviper.benchmark.model.BookDetailDto
import com.retheviper.benchmark.model.BookSummaryDto
import com.retheviper.benchmark.model.LargePayloadDto
import com.retheviper.benchmark.model.PayloadSummaryDto
import com.retheviper.benchmark.model.SmallPayloadDto
import com.retheviper.benchmark.repository.BookRepository
import java.time.OffsetDateTime

class BookService(
    private val repository: BookRepository,
) {
    suspend fun findById(id: Long): BookDetailDto? = repository.findById(id)

    suspend fun list(limit: Int): List<BookSummaryDto> = repository.list(limit.coerceIn(1, 500))

    suspend fun search(query: String, limit: Int): List<BookSummaryDto> = repository.search(query, limit.coerceIn(1, 200))

    fun smallPayload(): SmallPayloadDto = SmallPayloadDto(
        service = "ktor-r2dbc",
        version = 1,
        now = OffsetDateTime.now().toString(),
        tags = listOf("ktor", "r2dbc", "exposed", "benchmark"),
    )

    suspend fun largePayload(limit: Int): LargePayloadDto {
        val items = repository.listLargePayloadItems(limit.coerceIn(50, 500))
        return LargePayloadDto(
            service = "ktor-r2dbc",
            generatedAt = OffsetDateTime.now().toString(),
            summary = PayloadSummaryDto(
                itemCount = items.size,
                checksum = items.sumOf { it.id + it.priceCents + it.stock.toLong() },
            ),
            items = items,
        )
    }

    suspend fun authorInventory(limit: Int): List<AuthorInventoryReportDto> = repository.authorInventory(limit.coerceIn(10, 200))
}
