package com.retheviper.benchmarkjdbc.repository

import com.retheviper.benchmarkjdbc.db.BooksTable
import com.retheviper.benchmarkjdbc.db.CheckoutsTable
import com.retheviper.benchmarkjdbc.model.CheckoutResponseDto
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid
import java.time.OffsetDateTime
import java.util.UUID

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class CheckoutRepository {
    fun create(bookId: Long, customerName: String, customerEmail: String, quantity: Int): CheckoutResponseDto = transaction {
        val book = BooksTable.selectAll()
            .where { BooksTable.id eq bookId }
            .forUpdate(ForUpdateOption.PostgreSQL.ForUpdate())
            .limit(1)
            .singleOrNull()
            ?: throw NoSuchElementException("Book not found")
        val stock = book[BooksTable.stock]
        require(stock >= quantity) { "Insufficient stock for book $bookId" }
        BooksTable.update({ BooksTable.id eq bookId }) { it[BooksTable.stock] = stock - quantity }
        val checkoutId = UUID.randomUUID()
        val checkedOutAt = OffsetDateTime.now()
        CheckoutsTable.insert {
            it[id] = Uuid.parse(checkoutId.toString())
            it[CheckoutsTable.bookId] = bookId
            it[CheckoutsTable.customerName] = customerName
            it[CheckoutsTable.customerEmail] = customerEmail
            it[CheckoutsTable.quantity] = quantity
            it[CheckoutsTable.checkedOutAt] = checkedOutAt
        }
        CheckoutResponseDto(checkoutId.toString(), bookId, customerName, customerEmail, quantity, checkedOutAt)
    }
}
