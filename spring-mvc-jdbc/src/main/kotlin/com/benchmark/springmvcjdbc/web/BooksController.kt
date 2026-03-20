package com.benchmark.springmvcjdbc.web

import com.benchmark.springmvcjdbc.domain.AuthorInventoryReport
import com.benchmark.springmvcjdbc.domain.BookDetail
import com.benchmark.springmvcjdbc.domain.BookSummary
import com.benchmark.springmvcjdbc.domain.LargePayload
import com.benchmark.springmvcjdbc.domain.SmallPayload
import com.benchmark.springmvcjdbc.service.BookService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/books")
class BooksController(private val bookService: BookService) {
    @GetMapping("/bench/small-json")
    fun smallJson(): SmallPayload = bookService.smallPayload()

    @GetMapping("/bench/large-json")
    fun largeJson(@RequestParam(defaultValue = "200") limit: Int): LargePayload =
        bookService.largePayload(limit.coerceIn(50, 500))

    @GetMapping("/bench/aggregate-report")
    fun aggregateReport(@RequestParam(defaultValue = "50") limit: Int): List<AuthorInventoryReport> =
        bookService.authorInventory(limit.coerceIn(10, 200))

    @GetMapping("/{id}")
    fun getBook(@PathVariable id: Long): BookDetail = bookService.findBookById(id)

    @GetMapping
    fun listBooks(@RequestParam(defaultValue = "50") limit: Int): List<BookSummary> =
        bookService.listBooks(limit.coerceIn(1, 500))

    @GetMapping("/search")
    fun searchBooks(
        @RequestParam q: String,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<BookSummary> = bookService.searchBooks(q, limit.coerceIn(1, 200))
}
