package dev.jcasas.features.transactions

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.configureTransactionRoutes(service: TransactionService) {
    routing {
        post("/transactions") {
            val request = call.receive<TransactionRequest>()
            try {
                val id = service.create(request.toNewTransaction())
                call.respond(HttpStatusCode.Created, id)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.UnprocessableEntity, mapOf("error" to e.message))
            }
        }

        get("/transactions") {
            val categoryId = call.parameters["categoryId"]?.toIntOrNull()
            val year = call.parameters["year"]?.toIntOrNull()
            val month = call.parameters["month"]?.toIntOrNull()
            val budgetItemId = call.parameters["budgetItemId"]?.toIntOrNull()
            val transactions =
                service
                    .getAll(
                        categoryId,
                        year,
                        month,
                        budgetItemId,
                    ).map { TransactionResponse.from(it) }
            call.respond(HttpStatusCode.OK, transactions)
        }

        get("/transactions/{id}") {
            val id =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val transaction =
                service.getById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(HttpStatusCode.OK, TransactionResponse.from(transaction))
        }

        put("/transactions/{id}") {
            val id =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<TransactionRequest>()
            try {
                service.update(id, request.toNewTransaction())
                call.respond(HttpStatusCode.OK)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.UnprocessableEntity, mapOf("error" to e.message))
            }
        }

        delete("/transactions/{id}") {
            val id =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
            service.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}
