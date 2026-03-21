package dev.jcasas.features.budgets

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.configureBudgetRoutes(service: BudgetService) {
    routing {
        get("/budgets") {
            val year = call.parameters["year"]?.toIntOrNull()
            val budgets = service.getAllBudgets(year)
            call.respond(HttpStatusCode.OK, budgets)
        }

        get("/budgets/{year}/{month}") {
            val year = call.parameters["year"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val month = call.parameters["month"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val budget = service.getOrCreateBudget(year, month)
            call.respond(HttpStatusCode.OK, budget)
        }

        put("/budgets/{year}/{month}/items/{itemId}") {
            val itemId = call.parameters["itemId"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<BudgetItemUpdateRequest>()
            service.updateBudgetItem(itemId, request.allocationCents, request.snoozed)
            call.respond(HttpStatusCode.OK)
        }
    }
}
