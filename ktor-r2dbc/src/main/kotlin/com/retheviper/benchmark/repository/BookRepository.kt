package com.retheviper.benchmark.repository

import com.retheviper.benchmark.db.AuthorsTable
import com.retheviper.benchmark.db.BenchmarkDatabase
import com.retheviper.benchmark.db.BooksTable
import com.retheviper.benchmark.model.AuthorDto
import com.retheviper.benchmark.model.AuthorInventoryReportDto
import com.retheviper.benchmark.model.BookDetailDto
import com.retheviper.benchmark.model.BookSummaryDto
import com.retheviper.benchmark.model.LargePayloadItemDto
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class BookRepository {
    suspend fun findById(id: Long): BookDetailDto? {
        return suspendTransaction(db = BenchmarkDatabase.database) {
            joinedQuery()
                .where { BooksTable.id eq id }
                .limit(1)
                .toList()
                .singleOrNull()
                ?.toDetail()
        }
    }

    suspend fun list(limit: Int): List<BookSummaryDto> {
        return suspendTransaction(db = BenchmarkDatabase.database) {
            joinedQuery()
                .orderBy(BooksTable.id to SortOrder.ASC)
                .limit(limit)
                .toList()
                .map { it.toSummary() }
        }
    }

    suspend fun search(query: String, limit: Int): List<BookSummaryDto> {
        val pattern = "%${query.trim()}%"
        return suspendTransaction(db = BenchmarkDatabase.database) {
            joinedQuery()
                .where { (BooksTable.title like pattern) or (AuthorsTable.name like pattern) }
                .orderBy(BooksTable.id to SortOrder.ASC)
                .limit(limit)
                .toList()
                .map { it.toSummary() }
        }
    }

    suspend fun listLargePayloadItems(limit: Int): List<LargePayloadItemDto> {
        return suspendTransaction(db = BenchmarkDatabase.database) {
            joinedQuery()
                .orderBy(BooksTable.id to SortOrder.ASC)
                .limit(limit)
                .toList()
                .map { it.toLargePayloadItem() }
        }
    }

    suspend fun authorInventory(limit: Int): List<AuthorInventoryReportDto> {
        return listLargePayloadItems(limit * 10)
            .groupBy { it.authorName }
            .entries
            .mapIndexed { index, entry ->
                AuthorInventoryReportDto(
                    authorId = index + 1L,
                    authorName = entry.key,
                    bookCount = entry.value.size,
                    totalStock = entry.value.sumOf { it.stock },
                    averagePriceCents = entry.value.map { it.priceCents }.average().toInt(),
                )
            }
            .sortedWith(compareByDescending<AuthorInventoryReportDto> { it.bookCount }.thenBy { it.authorName })
            .take(limit)
    }

    private fun joinedQuery() =
        BooksTable.join(
            otherTable = AuthorsTable,
            joinType = JoinType.INNER,
            additionalConstraint = { BooksTable.authorId eq AuthorsTable.id },
        ).selectAll()

    private fun ResultRow.toSummary(): BookSummaryDto = BookSummaryDto(
        id = this[BooksTable.id],
        title = this[BooksTable.title],
        authorName = this[AuthorsTable.name],
        priceCents = this[BooksTable.priceCents],
        stock = this[BooksTable.stock],
        publishedAt = this[BooksTable.publishedAt].toString(),
    )

    private fun ResultRow.toDetail(): BookDetailDto = BookDetailDto(
        id = this[BooksTable.id],
        isbn = this[BooksTable.isbn],
        title = this[BooksTable.title],
        description = this[BooksTable.description],
        author = AuthorDto(
            id = this[AuthorsTable.id],
            name = this[AuthorsTable.name],
            bio = this[AuthorsTable.bio],
        ),
        priceCents = this[BooksTable.priceCents],
        stock = this[BooksTable.stock],
        publishedAt = this[BooksTable.publishedAt].toString(),
    )

    private fun ResultRow.toLargePayloadItem(): LargePayloadItemDto = LargePayloadItemDto(
        id = this[BooksTable.id],
        title = this[BooksTable.title],
        authorName = this[AuthorsTable.name],
        description = this[BooksTable.description],
        publishedAt = this[BooksTable.publishedAt].toString(),
        priceCents = this[BooksTable.priceCents],
        stock = this[BooksTable.stock],
    )
}
