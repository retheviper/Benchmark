package com.benchmark.springmvcjdbc.service

import com.benchmark.springmvcjdbc.domain.CheckoutRequest
import com.benchmark.springmvcjdbc.domain.CheckoutResponse
import com.benchmark.springmvcjdbc.repo.CheckoutRepository
import org.springframework.stereotype.Service

@Service
class CheckoutService(private val checkoutRepository: CheckoutRepository) {

    fun createCheckout(request: CheckoutRequest): CheckoutResponse {
        require(request.quantity > 0) { "quantity must be positive" }
        require(request.customerName.isNotBlank()) { "customerName must not be blank" }
        require(request.customerEmail.isNotBlank()) { "customerEmail must not be blank" }
        return checkoutRepository.createCheckout(
            request.bookId,
            request.customerName,
            request.customerEmail,
            request.quantity,
        )
    }
}
