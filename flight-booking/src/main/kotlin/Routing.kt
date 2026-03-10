package com.example.com

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.http.content.*

fun Application.configureRouting() {
    routing {
        staticResources("/", "static/home")
        staticResources("/log_in", "static/log_in")
    }
}
