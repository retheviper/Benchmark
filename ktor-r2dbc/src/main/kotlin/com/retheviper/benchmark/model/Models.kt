package com.retheviper.benchmark.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.OffsetDateTime

@Serializable
data class HealthResponse(
    val status: String,
)

@Serializable
data class AuthorDto(
    val id: Long,
    val name: String,
    val bio: String,
)

@Serializable
data class BookSummaryDto(
    val id: Long,
    val title: String,
    val authorName: String,
    val priceCents: Int,
    val stock: Int,
    @Serializable(with = LocalDateIsoSerializer::class)
    val publishedAt: LocalDate,
)

@Serializable
data class BookDetailDto(
    val id: Long,
    val isbn: String,
    val title: String,
    val description: String,
    val author: AuthorDto,
    val priceCents: Int,
    val stock: Int,
    @Serializable(with = LocalDateIsoSerializer::class)
    val publishedAt: LocalDate,
)

@Serializable
data class CheckoutRequestDto(
    val bookId: Long,
    val customerName: String,
    val customerEmail: String,
    val quantity: Int = 1,
)

@Serializable
data class CheckoutResponseDto(
    val id: String,
    val bookId: Long,
    val customerName: String,
    val customerEmail: String,
    val quantity: Int,
    @Serializable(with = OffsetDateTimeIsoSerializer::class)
    val checkedOutAt: OffsetDateTime,
)

@Serializable
data class ErrorResponseDto(
    val error: String,
)

@Serializable
data class SmallPayloadDto(
    val service: String,
    val version: Int,
    val now: String,
    val tags: List<String>,
)

@Serializable
data class LargePayloadDto(
    val service: String,
    val generatedAt: String,
    val summary: PayloadSummaryDto,
    val items: List<LargePayloadItemDto>,
)

@Serializable
data class PayloadSummaryDto(
    val itemCount: Int,
    val checksum: Long,
)

@Serializable
data class LargePayloadItemDto(
    val id: Long,
    val title: String,
    val authorName: String,
    val description: String,
    @Serializable(with = LocalDateIsoSerializer::class)
    val publishedAt: LocalDate,
    val priceCents: Int,
    val stock: Int,
)

@Serializable
data class AuthorInventoryReportDto(
    val authorId: Long,
    val authorName: String,
    val bookCount: Int,
    val totalStock: Int,
    val averagePriceCents: Int,
)
