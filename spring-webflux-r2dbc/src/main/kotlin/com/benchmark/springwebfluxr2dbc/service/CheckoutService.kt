package com.benchmark.springwebfluxr2dbc.service

import com.benchmark.springwebfluxr2dbc.domain.CheckoutRequest
import com.benchmark.springwebfluxr2dbc.domain.CheckoutResponse
import com.benchmark.springwebfluxr2dbc.repo.CheckoutRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CheckoutService(private val checkoutRepository: CheckoutRepository) {
    fun createCheckout(request: CheckoutRequest): Mono<CheckoutResponse> {
        require(request.quantity > 0) { "quantity must be positive" }
        require(request.customerName.isNotBlank()) { "customerName must not be blank" }
        require(request.customerEmail.isNotBlank()) { "customerEmail must not be blank" }
        return checkoutRepository.createCheckout(request.bookId, request.customerName, request.customerEmail, request.quantity)
    }
}

