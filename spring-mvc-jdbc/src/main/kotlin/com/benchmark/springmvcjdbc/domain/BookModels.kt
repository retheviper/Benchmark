package com.benchmark.springmvcjdbc.domain

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class BookSummary(
    val id: Long,
    val title: String,
    val authorName: String,
    val priceCents: Int,
    val stock: Int,
    val publishedAt: LocalDate,
)

data class BookDetail(
    val id: Long,
    val isbn: String,
    val title: String,
    val description: String,
    val author: AuthorSummary,
    val priceCents: Int,
    val stock: Int,
    val publishedAt: LocalDate,
)

data class AuthorSummary(
    val id: Long,
    val name: String,
    val bio: String,
)

data class CheckoutRequest(
    val bookId: Long,
    val customerName: String,
    val customerEmail: String,
    val quantity: Int = 1,
)

data class CheckoutResponse(
    val id: UUID,
    val bookId: Long,
    val customerName: String,
    val customerEmail: String,
    val quantity: Int,
    val checkedOutAt: OffsetDateTime,
)

data class SmallPayload(
    val service: String,
    val version: Int,
    val now: String,
    val tags: List<String>,
)

data class LargePayload(
    val service: String,
    val generatedAt: String,
    val summary: PayloadSummary,
    val items: List<LargePayloadItem>,
)

data class PayloadSummary(
    val itemCount: Int,
    val checksum: Long,
)

data class LargePayloadItem(
    val id: Long,
    val title: String,
    val authorName: String,
    val description: String,
    val publishedAt: LocalDate,
    val priceCents: Int,
    val stock: Int,
)

data class AuthorInventoryReport(
    val authorId: Long,
    val authorName: String,
    val bookCount: Int,
    val totalStock: Int,
    val averagePriceCents: Int,
)
