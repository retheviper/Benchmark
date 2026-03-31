package com.retheviper.benchmark.repository

import com.retheviper.benchmark.db.AuthorsTable
import com.retheviper.benchmark.db.BenchmarkDatabase
import com.retheviper.benchmark.db.BooksTable
import com.retheviper.benchmark.model.AuthorDto
import com.retheviper.benchmark.model.AuthorInventoryReportDto
import com.retheviper.benchmark.model.BookDetailDto
import com.retheviper.benchmark.model.BookSummaryDto
import com.retheviper.benchmark.model.LargePayloadItemDto
import org.jetbrains.exposed.v1.core.alias
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.avg
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class BookRepository {
    private val authorBookCount = BooksTable.id.count().alias("book_count")
    private val authorTotalStock = BooksTable.stock.sum().alias("total_stock")
    private val authorAveragePrice = BooksTable.priceCents.avg().alias("average_price")

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
            val lowerTitle = BooksTable.title.lowerCase()
            val lowerAuthorName = AuthorsTable.name.lowerCase()
            joinedQuery()
                .where { (lowerTitle like pattern.lowercase()) or (lowerAuthorName like pattern.lowercase()) }
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
        return suspendTransaction(db = BenchmarkDatabase.database) {
            BooksTable.join(
                otherTable = AuthorsTable,
                joinType = JoinType.INNER,
                additionalConstraint = { BooksTable.authorId eq AuthorsTable.id },
            )
                .select(AuthorsTable.id, AuthorsTable.name, authorBookCount, authorTotalStock, authorAveragePrice)
                .groupBy(AuthorsTable.id, AuthorsTable.name)
                .orderBy(authorBookCount to SortOrder.DESC, AuthorsTable.id to SortOrder.ASC)
                .limit(limit)
                .toList()
                .map {
                    AuthorInventoryReportDto(
                        authorId = it[AuthorsTable.id],
                        authorName = it[AuthorsTable.name],
                        bookCount = it[authorBookCount].toInt(),
                        totalStock = it[authorTotalStock] ?: 0,
                        averagePriceCents = it[authorAveragePrice]?.toInt() ?: 0,
                    )
                }
        }
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
        publishedAt = this[BooksTable.publishedAt],
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
        publishedAt = this[BooksTable.publishedAt],
    )

    private fun ResultRow.toLargePayloadItem(): LargePayloadItemDto = LargePayloadItemDto(
        id = this[BooksTable.id],
        title = this[BooksTable.title],
        authorName = this[AuthorsTable.name],
        description = this[BooksTable.description],
        publishedAt = this[BooksTable.publishedAt],
        priceCents = this[BooksTable.priceCents],
        stock = this[BooksTable.stock],
    )
}
