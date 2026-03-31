package com.retheviper.benchmarkjdbc.repository

import com.retheviper.benchmarkjdbc.db.AuthorsTable
import com.retheviper.benchmarkjdbc.db.BooksTable
import com.retheviper.benchmarkjdbc.model.AuthorDto
import com.retheviper.benchmarkjdbc.model.AuthorInventoryReportDto
import com.retheviper.benchmarkjdbc.model.BookDetailDto
import com.retheviper.benchmarkjdbc.model.BookSummaryDto
import com.retheviper.benchmarkjdbc.model.LargePayloadItemDto
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.avg
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class BookRepository {
    private val authorBookCount = BooksTable.id.count().alias("book_count")
    private val authorTotalStock = BooksTable.stock.sum().alias("total_stock")
    private val authorAveragePrice = BooksTable.priceCents.avg().alias("average_price")

    fun findById(id: Long): BookDetailDto? = transaction {
        joinedQuery().where { BooksTable.id eq id }.limit(1).singleOrNull()?.toDetail()
    }
    fun list(limit: Int): List<BookSummaryDto> = transaction {
        joinedQuery().orderBy(BooksTable.id to SortOrder.ASC).limit(limit).map { it.toSummary() }
    }
    fun search(query: String, limit: Int): List<BookSummaryDto> = transaction {
        val pattern = "%${query.trim()}%"
        val lowerTitle = BooksTable.title.lowerCase()
        val lowerAuthorName = AuthorsTable.name.lowerCase()
        joinedQuery()
            .where { (lowerTitle like pattern.lowercase()) or (lowerAuthorName like pattern.lowercase()) }
            .orderBy(BooksTable.id to SortOrder.ASC)
            .limit(limit)
            .map { it.toSummary() }
    }
    fun listLargePayloadItems(limit: Int): List<LargePayloadItemDto> = transaction {
        joinedQuery().orderBy(BooksTable.id to SortOrder.ASC).limit(limit).map { it.toLarge() }
    }
    fun authorInventory(limit: Int): List<AuthorInventoryReportDto> = transaction {
        BooksTable.join(AuthorsTable, JoinType.INNER, additionalConstraint = { BooksTable.authorId eq AuthorsTable.id })
            .select(AuthorsTable.id, AuthorsTable.name, authorBookCount, authorTotalStock, authorAveragePrice)
            .groupBy(AuthorsTable.id, AuthorsTable.name)
            .orderBy(authorBookCount to SortOrder.DESC, AuthorsTable.id to SortOrder.ASC)
            .limit(limit)
            .map { row ->
                AuthorInventoryReportDto(
                    authorId = row[AuthorsTable.id],
                    authorName = row[AuthorsTable.name],
                    bookCount = row[authorBookCount].toInt(),
                    totalStock = row[authorTotalStock] ?: 0,
                    averagePriceCents = row[authorAveragePrice]?.toInt() ?: 0,
                )
            }
    }

    private fun joinedQuery() = BooksTable.join(AuthorsTable, JoinType.INNER, additionalConstraint = { BooksTable.authorId eq AuthorsTable.id }).selectAll()
    private fun ResultRow.toSummary() = BookSummaryDto(this[BooksTable.id], this[BooksTable.title], this[AuthorsTable.name], this[BooksTable.priceCents], this[BooksTable.stock], this[BooksTable.publishedAt])
    private fun ResultRow.toDetail() = BookDetailDto(this[BooksTable.id], this[BooksTable.isbn], this[BooksTable.title], this[BooksTable.description], AuthorDto(this[AuthorsTable.id], this[AuthorsTable.name], this[AuthorsTable.bio]), this[BooksTable.priceCents], this[BooksTable.stock], this[BooksTable.publishedAt])
    private fun ResultRow.toLarge() = LargePayloadItemDto(this[BooksTable.id], this[BooksTable.title], this[AuthorsTable.name], this[BooksTable.description], this[BooksTable.publishedAt], this[BooksTable.priceCents], this[BooksTable.stock])
}
