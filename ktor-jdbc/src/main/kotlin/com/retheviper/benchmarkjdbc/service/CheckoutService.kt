package com.retheviper.benchmarkjdbc.service

import com.retheviper.benchmarkjdbc.model.CheckoutResponseDto
import com.retheviper.benchmarkjdbc.repository.CheckoutRepository

class CheckoutService(private val repository: CheckoutRepository) {
    fun checkout(bookId: Long, customerName: String, customerEmail: String, quantity: Int): CheckoutResponseDto {
        require(customerName.isNotBlank()) { "customerName must not be blank" }
        require(customerEmail.isNotBlank()) { "customerEmail must not be blank" }
        require(quantity > 0) { "quantity must be positive" }
        return repository.create(bookId, customerName, customerEmail, quantity)
    }
}

