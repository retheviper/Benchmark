package com.benchmark.springwebfluxr2dbc.repo

import com.benchmark.springwebfluxr2dbc.domain.AuthorInventoryReport
import com.benchmark.springwebfluxr2dbc.domain.AuthorSummary
import com.benchmark.springwebfluxr2dbc.domain.BookDetail
import com.benchmark.springwebfluxr2dbc.domain.BookSummary
import com.benchmark.springwebfluxr2dbc.domain.LargePayloadItem
import org.jooq.DSLContext
import org.jooq.Query
import org.jooq.conf.ParamType
import org.jooq.impl.DSL
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate

@Repository
class BookRepository(
    private val dsl: DSLContext,
    private val databaseClient: DatabaseClient,
) {
    private val authors = DSL.table("authors")
    private val books = DSL.table("books")
    private val authorIdField = DSL.field("authors.id", Long::class.java)
    private val authorNameField = DSL.field("authors.name", String::class.java)
    private val authorBioField = DSL.field("authors.bio", String::class.java)
    private val bookIdField = DSL.field("books.id", Long::class.java)
    private val bookAuthorIdField = DSL.field("books.author_id", Long::class.java)
    private val bookIsbnField = DSL.field("books.isbn", String::class.java)
    private val bookTitleField = DSL.field("books.title", String::class.java)
    private val bookDescriptionField = DSL.field("books.description", String::class.java)
    private val bookPriceCentsField = DSL.field("books.price_cents", Int::class.java)
    private val bookStockField = DSL.field("books.stock", Int::class.java)
    private val bookPublishedAtField = DSL.field("books.published_at", LocalDate::class.java)

    private val authorId = authorIdField.`as`("author_id")
    private val authorName = authorNameField.`as`("author_name")
    private val authorBio = authorBioField.`as`("author_bio")
    private val bookId = bookIdField.`as`("book_id")
    private val bookIsbn = bookIsbnField.`as`("book_isbn")
    private val bookTitle = bookTitleField.`as`("book_title")
    private val bookDescription = bookDescriptionField.`as`("book_description")
    private val bookPriceCents = bookPriceCentsField.`as`("book_price_cents")
    private val bookStock = bookStockField.`as`("book_stock")
    private val bookPublishedAt = bookPublishedAtField.`as`("book_published_at")

    fun findBookById(id: Long): Mono<BookDetail> {
        val query = dsl.select(
            bookId, bookIsbn, bookTitle, bookDescription, bookPriceCents, bookStock, bookPublishedAt,
            authorId, authorName, authorBio,
        ).from(books).join(authors).on(bookAuthorIdField.eq(authorIdField)).where(bookIdField.eq(id))
        return bind(query).map { row, _ ->
            BookDetail(
                id = row.get("book_id", java.lang.Long::class.java)!!.toLong(),
                isbn = row.get("book_isbn", String::class.java)!!,
                title = row.get("book_title", String::class.java)!!,
                description = row.get("book_description", String::class.java)!!,
                author = AuthorSummary(
                    id = row.get("author_id", java.lang.Long::class.java)!!.toLong(),
                    name = row.get("author_name", String::class.java)!!,
                    bio = row.get("author_bio", String::class.java)!!,
                ),
                priceCents = row.get("book_price_cents", Integer::class.java)!!.toInt(),
                stock = row.get("book_stock", Integer::class.java)!!.toInt(),
                publishedAt = row.get("book_published_at", LocalDate::class.java)!!,
            )
        }.one()
    }

    fun listBooks(limit: Int): Flux<BookSummary> {
        val query = dsl.select(
            bookId, bookTitle, authorName, bookPriceCents, bookStock, bookPublishedAt,
        ).from(books).join(authors).on(bookAuthorIdField.eq(authorIdField)).orderBy(bookIdField.asc()).limit(limit)
        return bind(query).map { row, _ ->
            BookSummary(
                id = row.get("book_id", java.lang.Long::class.java)!!.toLong(),
                title = row.get("book_title", String::class.java)!!,
                authorName = row.get("author_name", String::class.java)!!,
                priceCents = row.get("book_price_cents", Integer::class.java)!!.toInt(),
                stock = row.get("book_stock", Integer::class.java)!!.toInt(),
                publishedAt = row.get("book_published_at", LocalDate::class.java)!!,
            )
        }.all()
    }

    fun searchBooks(term: String, limit: Int): Flux<BookSummary> {
        val pattern = "%${term.trim()}%"
        val query = dsl.select(
            bookId, bookTitle, authorName, bookPriceCents, bookStock, bookPublishedAt,
        ).from(books).join(authors).on(bookAuthorIdField.eq(authorIdField))
            .where(bookTitleField.likeIgnoreCase(pattern).or(authorNameField.likeIgnoreCase(pattern)))
            .orderBy(bookIdField.asc()).limit(limit)
        return bind(query).map { row, _ ->
            BookSummary(
                id = row.get("book_id", java.lang.Long::class.java)!!.toLong(),
                title = row.get("book_title", String::class.java)!!,
                authorName = row.get("author_name", String::class.java)!!,
                priceCents = row.get("book_price_cents", Integer::class.java)!!.toInt(),
                stock = row.get("book_stock", Integer::class.java)!!.toInt(),
                publishedAt = row.get("book_published_at", LocalDate::class.java)!!,
            )
        }.all()
    }

    fun listLargePayloadItems(limit: Int): Flux<LargePayloadItem> {
        val query = dsl.select(
            bookId, bookTitle, authorName, bookDescription, bookPublishedAt, bookPriceCents, bookStock,
        ).from(books).join(authors).on(bookAuthorIdField.eq(authorIdField)).orderBy(bookIdField.asc()).limit(limit)
        return bind(query).map { row, _ ->
            LargePayloadItem(
                id = row.get("book_id", java.lang.Long::class.java)!!.toLong(),
                title = row.get("book_title", String::class.java)!!,
                authorName = row.get("author_name", String::class.java)!!,
                description = row.get("book_description", String::class.java)!!,
                publishedAt = row.get("book_published_at", LocalDate::class.java)!!,
                priceCents = row.get("book_price_cents", Integer::class.java)!!.toInt(),
                stock = row.get("book_stock", Integer::class.java)!!.toInt(),
            )
        }.all()
    }

    fun authorInventory(limit: Int): Flux<AuthorInventoryReport> {
        val countField = DSL.count().`as`("book_count")
        val totalStockField = DSL.sum(bookStockField).`as`("total_stock")
        val avgPriceField = DSL.avg(bookPriceCentsField).`as`("average_price")
        val query = dsl.select(authorId, authorName, countField, totalStockField, avgPriceField)
            .from(authors).join(books).on(bookAuthorIdField.eq(authorIdField))
            .groupBy(authorIdField, authorNameField)
            .orderBy(countField.desc(), authorIdField.asc()).limit(limit)
        return bind(query).map { row, _ ->
            AuthorInventoryReport(
                authorId = row.get("author_id", java.lang.Long::class.java)!!.toLong(),
                authorName = row.get("author_name", String::class.java)!!,
                bookCount = row.get("book_count", Integer::class.java)?.toInt() ?: 0,
                totalStock = row.get("total_stock", Integer::class.java)?.toInt() ?: 0,
                averagePriceCents = row.get("average_price", java.math.BigDecimal::class.java)?.toInt() ?: 0,
            )
        }.all()
    }

    private fun bind(query: Query): DatabaseClient.GenericExecuteSpec {
        return databaseClient.sql(query.getSQL(ParamType.INLINED))
    }
}
