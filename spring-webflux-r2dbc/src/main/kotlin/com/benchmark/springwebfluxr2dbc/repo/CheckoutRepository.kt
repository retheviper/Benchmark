package com.benchmark.springwebfluxr2dbc.repo

import com.benchmark.springwebfluxr2dbc.domain.CheckoutResponse
import org.jooq.DSLContext
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class CheckoutRepository(
    private val dsl: DSLContext,
    private val databaseClient: DatabaseClient,
    private val tx: TransactionalOperator,
) {
    fun createCheckout(bookId: Long, customerName: String, customerEmail: String, quantity: Int): Mono<CheckoutResponse> {
        return tx.transactional(
            selectStock(bookId).flatMap { currentStock ->
                if (currentStock < quantity) {
                    return@flatMap Mono.error(IllegalArgumentException("Insufficient stock for book $bookId"))
                }
                val checkoutId = UUID.randomUUID()
                updateStock(bookId, quantity).then(insertCheckout(checkoutId, bookId, customerName, customerEmail, quantity))
            },
        )
    }

    private fun selectStock(bookId: Long): Mono<Int> =
        databaseClient.sql("select stock from books where id = $1 for update")
            .bind(0, bookId)
            .map { row, _ -> row.get("stock", Integer::class.java)!!.toInt() }
            .one()
            .switchIfEmpty(Mono.error<Int>(NoSuchElementException("Book $bookId not found")))

    private fun updateStock(bookId: Long, quantity: Int): Mono<Void> =
        databaseClient.sql("update books set stock = stock - $2 where id = $1")
            .bind(0, bookId)
            .bind(1, quantity)
            .fetch()
            .rowsUpdated()
            .then()

    private fun insertCheckout(
        checkoutId: UUID,
        bookId: Long,
        customerName: String,
        customerEmail: String,
        quantity: Int,
    ): Mono<CheckoutResponse> =
        databaseClient.sql(
            """
            insert into checkouts (id, book_id, customer_name, customer_email, quantity)
            values ($1, $2, $3, $4, $5)
            returning id, book_id, customer_name, customer_email, quantity, checked_out_at
            """.trimIndent()
        )
            .bind(0, checkoutId)
            .bind(1, bookId)
            .bind(2, customerName)
            .bind(3, customerEmail)
            .bind(4, quantity)
            .map { row, _ ->
                CheckoutResponse(
                    id = row.get("id", UUID::class.java)!!,
                    bookId = row.get("book_id", java.lang.Long::class.java)!!.toLong(),
                    customerName = row.get("customer_name", String::class.java)!!,
                    customerEmail = row.get("customer_email", String::class.java)!!,
                    quantity = row.get("quantity", Integer::class.java)!!.toInt(),
                    checkedOutAt = row.get("checked_out_at", OffsetDateTime::class.java)!!,
                )
            }
            .one()
}
