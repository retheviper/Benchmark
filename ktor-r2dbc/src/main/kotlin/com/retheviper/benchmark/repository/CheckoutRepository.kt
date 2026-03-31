@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.benchmark.repository

import com.retheviper.benchmark.db.BenchmarkDatabase
import com.retheviper.benchmark.db.BooksTable
import com.retheviper.benchmark.db.CheckoutsTable
import com.retheviper.benchmark.model.CheckoutResponseDto
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import java.time.OffsetDateTime
import kotlin.uuid.Uuid

class CheckoutRepository {
    suspend fun create(bookId: Long, customerName: String, customerEmail: String, quantity: Int): CheckoutResponseDto {
        return suspendTransaction(db = BenchmarkDatabase.database) {
            val book = BooksTable.selectAll()
                .where { BooksTable.id eq bookId }
                .forUpdate(ForUpdateOption.PostgreSQL.ForUpdate())
                .limit(1)
                .toList()
                .singleOrNull()
                ?: throw NoSuchElementException("Book not found")

            val stock = book[BooksTable.stock]
            require(stock >= quantity) { "Insufficient stock for book $bookId" }

            BooksTable.update({ BooksTable.id eq bookId }) { statement ->
                statement[BooksTable.stock] = stock - quantity
            }

            val checkoutId = Uuid.random()
            val checkedOutAt = OffsetDateTime.now()

            CheckoutsTable.insert { statement ->
                statement[id] = checkoutId
                statement[CheckoutsTable.bookId] = bookId
                statement[CheckoutsTable.customerName] = customerName
                statement[CheckoutsTable.customerEmail] = customerEmail
                statement[CheckoutsTable.quantity] = quantity
                statement[CheckoutsTable.checkedOutAt] = checkedOutAt
            }

            CheckoutResponseDto(
                id = checkoutId.toString(),
                bookId = bookId,
                customerName = customerName,
                customerEmail = customerEmail,
                quantity = quantity,
                checkedOutAt = checkedOutAt,
            )
        }
    }
}
