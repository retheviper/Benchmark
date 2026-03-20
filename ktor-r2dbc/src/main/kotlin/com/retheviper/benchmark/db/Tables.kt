@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.benchmark.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object AuthorsTable : Table("authors") {
    val id = long("id")
    val name = varchar("name", 200)
    val bio = text("bio")

    override val primaryKey = PrimaryKey(id)
}

object BooksTable : Table("books") {
    val id = long("id")
    val authorId = long("author_id")
    val isbn = varchar("isbn", 20)
    val title = varchar("title", 255)
    val description = text("description")
    val priceCents = integer("price_cents")
    val stock = integer("stock")
    val publishedAt = date("published_at")

    override val primaryKey = PrimaryKey(id)
}

object CheckoutsTable : Table("checkouts") {
    val id = uuid("id")
    val bookId = long("book_id")
    val customerName = varchar("customer_name", 120)
    val customerEmail = varchar("customer_email", 255)
    val quantity = integer("quantity")
    val checkedOutAt = timestampWithTimeZone("checked_out_at")

    override val primaryKey = PrimaryKey(id)
}
