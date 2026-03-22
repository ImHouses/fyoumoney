package dev.jcasas.features.categories

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.configureCategoryRoutes(service: CategoryService) {
    routing {
        post("/categories/batch") {
            val requests = call.receive<List<CategoryRequest>>()
            val ids = service.createBatch(requests.map { it.toNewCategory() })
            call.respond(HttpStatusCode.Created, ids)
        }

        post("/categories") {
            val request = call.receive<CategoryRequest>()
            val id = service.create(request.toNewCategory())
            call.respond(HttpStatusCode.Created, id)
        }

        get("/categories") {
            val categories = service.getAllActive().map { CategoryResponse.from(it) }
            call.respond(HttpStatusCode.OK, categories)
        }

        get("/categories/{id}") {
            val id =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val category =
                service.getById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(HttpStatusCode.OK, CategoryResponse.from(category))
        }

        put("/categories/{id}") {
            val id =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<CategoryRequest>()
            service.update(id, request.toNewCategory())
            call.respond(HttpStatusCode.OK)
        }

        delete("/categories/{id}") {
            val id =
                call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
            service.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}
