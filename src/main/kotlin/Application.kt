package dev.jcasas

import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureFrameworks()
    configureRouting()
}
