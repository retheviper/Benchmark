package com.benchmark.springmvcjdbc.web

import com.benchmark.springmvcjdbc.domain.CheckoutRequest
import com.benchmark.springmvcjdbc.domain.CheckoutResponse
import com.benchmark.springmvcjdbc.service.CheckoutService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/checkouts")
class CheckoutController(private val checkoutService: CheckoutService) {

    @PostMapping
    fun createCheckout(@RequestBody request: CheckoutRequest): ResponseEntity<CheckoutResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.createCheckout(request))
    }
}
