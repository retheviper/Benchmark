package com.retheviper.benchmarkjdbc.repository

import com.retheviper.benchmarkjdbc.db.AuthorsTable
import com.retheviper.benchmarkjdbc.db.BooksTable
import com.retheviper.benchmarkjdbc.model.AuthorDto
import com.retheviper.benchmarkjdbc.model.AuthorInventoryReportDto
import com.retheviper.benchmarkjdbc.model.BookDetailDto
import com.retheviper.benchmarkjdbc.model.BookSummaryDto
import com.retheviper.benchmarkjdbc.model.LargePayloadItemDto
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class BookRepository {
    fun findById(id: Long): BookDetailDto? = transaction {
        joinedQuery().where { BooksTable.id eq id }.limit(1).singleOrNull()?.toDetail()
    }
    fun list(limit: Int): List<BookSummaryDto> = transaction {
        joinedQuery().orderBy(BooksTable.id to SortOrder.ASC).limit(limit).map { it.toSummary() }
    }
    fun search(query: String, limit: Int): List<BookSummaryDto> = transaction {
        val pattern = "%${query.trim()}%"
        joinedQuery().where { (BooksTable.title like pattern) or (AuthorsTable.name like pattern) }.orderBy(BooksTable.id to SortOrder.ASC).limit(limit).map { it.toSummary() }
    }
    fun listLargePayloadItems(limit: Int): List<LargePayloadItemDto> = transaction {
        joinedQuery().orderBy(BooksTable.id to SortOrder.ASC).limit(limit).map { it.toLarge() }
    }
    fun authorInventory(limit: Int): List<AuthorInventoryReportDto> =
        listLargePayloadItems(limit * 10).groupBy { it.authorName }.entries.mapIndexed { index, entry ->
            AuthorInventoryReportDto(index + 1L, entry.key, entry.value.size, entry.value.sumOf { it.stock }, entry.value.map { it.priceCents }.average().toInt())
        }.sortedWith(compareByDescending<AuthorInventoryReportDto> { it.bookCount }.thenBy { it.authorName }).take(limit)

    private fun joinedQuery() = BooksTable.join(AuthorsTable, JoinType.INNER, additionalConstraint = { BooksTable.authorId eq AuthorsTable.id }).selectAll()
    private fun ResultRow.toSummary() = BookSummaryDto(this[BooksTable.id], this[BooksTable.title], this[AuthorsTable.name], this[BooksTable.priceCents], this[BooksTable.stock], this[BooksTable.publishedAt].toString())
    private fun ResultRow.toDetail() = BookDetailDto(this[BooksTable.id], this[BooksTable.isbn], this[BooksTable.title], this[BooksTable.description], AuthorDto(this[AuthorsTable.id], this[AuthorsTable.name], this[AuthorsTable.bio]), this[BooksTable.priceCents], this[BooksTable.stock], this[BooksTable.publishedAt].toString())
    private fun ResultRow.toLarge() = LargePayloadItemDto(this[BooksTable.id], this[BooksTable.title], this[AuthorsTable.name], this[BooksTable.description], this[BooksTable.publishedAt].toString(), this[BooksTable.priceCents], this[BooksTable.stock])
}

