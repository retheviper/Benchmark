package com.retheviper.benchmark.routes

import com.retheviper.benchmark.model.CheckoutRequestDto
import com.retheviper.benchmark.model.ErrorResponseDto
import com.retheviper.benchmark.model.HealthResponse
import com.retheviper.benchmark.service.BookService
import com.retheviper.benchmark.service.CheckoutService
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

    get("/health") {
        call.respond(HealthResponse(status = "UP"))
    }

    route("/api/books") {
        get("/bench/small-json") {
            call.respond(bookService.smallPayload())
        }

        get("/bench/large-json") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 200
            call.respond(bookService.largePayload(limit))
        }

        get("/bench/aggregate-report") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            call.respond(bookService.authorInventory(limit))
        }

        get {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            call.respond(bookService.list(limit))
        }

        get("/search") {
            val query = call.request.queryParameters["q"]?.trim().orEmpty()
            if (query.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponseDto("query parameter q is required"))
                return@get
            }

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            call.respond(bookService.search(query, limit))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponseDto("invalid book id"))
                return@get
            }

            val book = bookService.findById(id)
            if (book == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponseDto("book not found"))
                return@get
            }

            call.respond(book)
        }
    }

    route("/api/checkouts") {
        post {
            val request = call.receive<CheckoutRequestDto>()
            val checkout = try {
                checkoutService.checkout(
                    request.bookId,
                    request.customerName,
                    request.customerEmail,
                    request.quantity,
                )
            } catch (error: NoSuchElementException) {
                call.respond(HttpStatusCode.NotFound, ErrorResponseDto(error.message ?: "book not found"))
                return@post
            } catch (error: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponseDto(error.message ?: "invalid checkout"))
                return@post
            }

            call.respond(HttpStatusCode.Created, checkout)
        }
    }
}
