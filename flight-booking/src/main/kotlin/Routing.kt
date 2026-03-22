package com.example.com

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.pebbletemplates.pebble.loader.ClasspathLoader
import org.jetbrains.exposed.sql.*
import io.ktor.http.*
import io.ktor.server.http.content.*

import java.io.File

fun Application.configureRouting() {
    routing {

        staticResources("/", "static/home")
        staticResources("/log_in", "static/log_in")

        get("/book") {
            call.respondFile(
                File("src/main/resources/static/home/book.html")
            )
        }

    }
}

