package com.benchmark.springwebfluxr2dbc.web

import com.benchmark.springwebfluxr2dbc.domain.AuthorInventoryReport
import com.benchmark.springwebfluxr2dbc.domain.BookDetail
import com.benchmark.springwebfluxr2dbc.domain.BookSummary
import com.benchmark.springwebfluxr2dbc.domain.CheckoutRequest
import com.benchmark.springwebfluxr2dbc.domain.CheckoutResponse
import com.benchmark.springwebfluxr2dbc.domain.LargePayload
import com.benchmark.springwebfluxr2dbc.domain.SmallPayload
import com.benchmark.springwebfluxr2dbc.service.BookService
import com.benchmark.springwebfluxr2dbc.service.CheckoutService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class HealthController {
    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "UP")
}

@RestController
@RequestMapping("/api/books")
class BooksController(private val bookService: BookService) {
    @GetMapping("/bench/small-json")
    fun smallJson(): SmallPayload = bookService.smallPayload()

    @GetMapping("/bench/large-json")
    fun largeJson(@RequestParam(defaultValue = "200") limit: Int): Mono<LargePayload> = bookService.largePayload(limit.coerceIn(50, 500))

    @GetMapping("/bench/aggregate-report")
    fun aggregateReport(@RequestParam(defaultValue = "50") limit: Int): Flux<AuthorInventoryReport> = bookService.authorInventory(limit.coerceIn(10, 200))

    @GetMapping("/{id}")
    fun getBook(@PathVariable id: Long): Mono<BookDetail> = bookService.findBookById(id)

    @GetMapping
    fun listBooks(@RequestParam(defaultValue = "50") limit: Int): Flux<BookSummary> = bookService.listBooks(limit.coerceIn(1, 500))

    @GetMapping("/search")
    fun searchBooks(@RequestParam q: String, @RequestParam(defaultValue = "20") limit: Int): Flux<BookSummary> =
        bookService.searchBooks(q, limit.coerceIn(1, 200))
}

@RestController
@RequestMapping("/api/checkouts")
class CheckoutController(private val checkoutService: CheckoutService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCheckout(@RequestBody request: CheckoutRequest): Mono<CheckoutResponse> = checkoutService.createCheckout(request)
}

