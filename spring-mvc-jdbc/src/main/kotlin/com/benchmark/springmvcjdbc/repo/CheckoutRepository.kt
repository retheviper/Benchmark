package com.benchmark.springmvcjdbc.repo

import com.benchmark.springmvcjdbc.domain.CheckoutResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class CheckoutRepository(private val jdbcTemplate: JdbcTemplate) {

    @Transactional
    fun createCheckout(bookId: Long, customerName: String, customerEmail: String, quantity: Int): CheckoutResponse {
        val currentStock = jdbcTemplate.queryForObject(
            """
            select stock
            from books
            where id = ?
            for update
            """.trimIndent(),
            Int::class.java,
            bookId,
        ) ?: throw NoSuchElementException("Book $bookId not found")

        require(currentStock >= quantity) { "Insufficient stock for book $bookId" }

        jdbcTemplate.update("update books set stock = stock - ? where id = ?", quantity, bookId)

        val checkoutId = UUID.randomUUID()
        return jdbcTemplate.queryForObject(
            """
            insert into checkouts (id, book_id, customer_name, customer_email, quantity)
            values (?, ?, ?, ?, ?)
            returning id, book_id, customer_name, customer_email, quantity, checked_out_at
            """.trimIndent(),
            { rs: ResultSet, _ ->
                CheckoutResponse(
                    id = rs.getObject("id", UUID::class.java),
                    bookId = rs.getLong("book_id"),
                    customerName = rs.getString("customer_name"),
                    customerEmail = rs.getString("customer_email"),
                    quantity = rs.getInt("quantity"),
                    checkedOutAt = rs.getObject("checked_out_at", OffsetDateTime::class.java),
                )
            },
            checkoutId,
            bookId,
            customerName,
            customerEmail,
            quantity,
        )
    }
}
