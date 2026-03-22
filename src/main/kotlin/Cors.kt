package dev.jcasas

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors() {
    val isDevelopment =
        environment.config
            .propertyOrNull("ktor.development")
            ?.getString()
            ?.toBoolean() ?: false

    if (!isDevelopment) return

    val viteDevServer = "localhost:5173" // Vite default dev server

    install(CORS) {
        allowHost(viteDevServer)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
}
