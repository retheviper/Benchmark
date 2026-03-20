package com.benchmark.springmvcjdbc.repo

import com.benchmark.springmvcjdbc.domain.AuthorSummary
import com.benchmark.springmvcjdbc.domain.AuthorInventoryReport
import com.benchmark.springmvcjdbc.domain.BookDetail
import com.benchmark.springmvcjdbc.domain.BookSummary
import com.benchmark.springmvcjdbc.domain.LargePayloadItem
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class BookRepository(private val dsl: DSLContext) {
    private val authors = DSL.table("authors")
    private val books = DSL.table("books")

    private val authorId = DSL.field("authors.id", Long::class.java)
    private val authorName = DSL.field("authors.name", String::class.java)
    private val authorBio = DSL.field("authors.bio", String::class.java)

    private val bookId = DSL.field("books.id", Long::class.java)
    private val bookAuthorId = DSL.field("books.author_id", Long::class.java)
    private val bookIsbn = DSL.field("books.isbn", String::class.java)
    private val bookTitle = DSL.field("books.title", String::class.java)
    private val bookDescription = DSL.field("books.description", String::class.java)
    private val bookPriceCents = DSL.field("books.price_cents", Int::class.java)
    private val bookStock = DSL.field("books.stock", Int::class.java)
    private val bookPublishedAt = DSL.field("books.published_at", LocalDate::class.java)
    private val authorBookCount = DSL.count().`as`("book_count")
    private val authorTotalStock = DSL.sum(bookStock).`as`("total_stock")
    private val authorAveragePrice = DSL.avg(bookPriceCents).`as`("average_price")

    fun findBookById(id: Long): BookDetail? =
        dsl.select(
            bookId,
            bookIsbn,
            bookTitle,
            bookDescription,
            bookPriceCents,
            bookStock,
            bookPublishedAt,
            authorId,
            authorName,
            authorBio,
        )
            .from(books)
            .join(authors).on(bookAuthorId.eq(authorId))
            .where(bookId.eq(id))
            .fetchOne { record ->
                BookDetail(
                    id = record.get(bookId)!!,
                    isbn = record.get(bookIsbn)!!,
                    title = record.get(bookTitle)!!,
                    description = record.get(bookDescription)!!,
                    author = AuthorSummary(
                        id = record.get(authorId)!!,
                        name = record.get(authorName)!!,
                        bio = record.get(authorBio)!!,
                    ),
                    priceCents = record.get(bookPriceCents)!!,
                    stock = record.get(bookStock)!!,
                    publishedAt = record.get(bookPublishedAt)!!,
                )
            }

    fun listBooks(limit: Int): List<BookSummary> =
        dsl.select(
            bookId,
            bookTitle,
            authorName,
            bookPriceCents,
            bookStock,
            bookPublishedAt,
        )
            .from(books)
            .join(authors).on(bookAuthorId.eq(authorId))
            .orderBy(bookId.asc())
            .limit(limit)
            .fetch { record ->
                BookSummary(
                    id = record.get(bookId)!!,
                    title = record.get(bookTitle)!!,
                    authorName = record.get(authorName)!!,
                    priceCents = record.get(bookPriceCents)!!,
                    stock = record.get(bookStock)!!,
                    publishedAt = record.get(bookPublishedAt)!!,
                )
            }

    fun searchBooks(term: String, limit: Int): List<BookSummary> {
        val pattern = "%${term.trim()}%"
        return dsl.select(
            bookId,
            bookTitle,
            authorName,
            bookPriceCents,
            bookStock,
            bookPublishedAt,
        )
            .from(books)
            .join(authors).on(bookAuthorId.eq(authorId))
            .where(bookTitle.likeIgnoreCase(pattern).or(authorName.likeIgnoreCase(pattern)))
            .orderBy(bookId.asc())
            .limit(limit)
            .fetch { record ->
                BookSummary(
                    id = record.get(bookId)!!,
                    title = record.get(bookTitle)!!,
                    authorName = record.get(authorName)!!,
                    priceCents = record.get(bookPriceCents)!!,
                    stock = record.get(bookStock)!!,
                    publishedAt = record.get(bookPublishedAt)!!,
                )
            }
    }

    fun listLargePayloadItems(limit: Int): List<LargePayloadItem> =
        dsl.select(
            bookId,
            bookTitle,
            authorName,
            bookDescription,
            bookPublishedAt,
            bookPriceCents,
            bookStock,
        )
            .from(books)
            .join(authors).on(bookAuthorId.eq(authorId))
            .orderBy(bookId.asc())
            .limit(limit)
            .fetch { record ->
                LargePayloadItem(
                    id = record.get(bookId)!!,
                    title = record.get(bookTitle)!!,
                    authorName = record.get(authorName)!!,
                    description = record.get(bookDescription)!!,
                    publishedAt = record.get(bookPublishedAt)!!,
                    priceCents = record.get(bookPriceCents)!!,
                    stock = record.get(bookStock)!!,
                )
            }

    fun authorInventory(limit: Int): List<AuthorInventoryReport> =
        dsl.select(
            authorId,
            authorName,
            authorBookCount,
            authorTotalStock,
            authorAveragePrice,
        )
            .from(authors)
            .join(books).on(bookAuthorId.eq(authorId))
            .groupBy(authorId, authorName)
            .orderBy(authorBookCount.desc(), authorId.asc())
            .limit(limit)
            .fetch { record ->
                AuthorInventoryReport(
                    authorId = record.get(authorId)!!,
                    authorName = record.get(authorName)!!,
                    bookCount = ((record.get(authorBookCount) as? Number)?.toInt() ?: 0),
                    totalStock = ((record.get(authorTotalStock) as? Number)?.toInt() ?: 0),
                    averagePriceCents = ((record.get(authorAveragePrice) as? Number)?.toDouble() ?: 0.0).toInt(),
                )
            }
}
