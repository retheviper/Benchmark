package com.retheviper.benchmarkjdbc.routes

import com.retheviper.benchmarkjdbc.model.CheckoutRequestDto
import com.retheviper.benchmarkjdbc.model.ErrorResponseDto
import com.retheviper.benchmarkjdbc.model.HealthResponse
import com.retheviper.benchmarkjdbc.service.BookService
import com.retheviper.benchmarkjdbc.service.CheckoutService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.registerRoutes() {
    val bookService by inject<BookService>()
    val checkoutService by inject<CheckoutService>()

    get("/health") { call.respond(HealthResponse("UP")) }
    route("/api/books") {
        get("/bench/small-json") { call.respond(bookService.smallPayload()) }
        get("/bench/large-json") { call.respond(bookService.largePayload(call.request.queryParameters["limit"]?.toIntOrNull() ?: 200)) }
        get("/bench/aggregate-report") { call.respond(bookService.authorInventory(call.request.queryParameters["limit"]?.toIntOrNull() ?: 50)) }
        get { call.respond(bookService.list(call.request.queryParameters["limit"]?.toIntOrNull() ?: 50)) }
        get("/search") {
            val q = call.request.queryParameters["q"]?.trim().orEmpty()
            if (q.isBlank()) return@get call.respond(HttpStatusCode.BadRequest, ErrorResponseDto("query parameter q is required"))
            call.respond(bookService.search(q, call.request.queryParameters["limit"]?.toIntOrNull() ?: 20))
        }
        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponseDto("invalid book id"))
            val book = bookService.findById(id) ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponseDto("book not found"))
            call.respond(book)
        }
    }
    route("/api/checkouts") {
        post {
            val request = call.receive<CheckoutRequestDto>()
            val response = try { checkoutService.checkout(request.bookId, request.customerName, request.customerEmail, request.quantity) }
            catch (e: NoSuchElementException) { return@post call.respond(HttpStatusCode.NotFound, ErrorResponseDto(e.message ?: "book not found")) }
            catch (e: IllegalArgumentException) { return@post call.respond(HttpStatusCode.BadRequest, ErrorResponseDto(e.message ?: "invalid checkout")) }
            call.respond(HttpStatusCode.Created, response)
        }
    }
}
