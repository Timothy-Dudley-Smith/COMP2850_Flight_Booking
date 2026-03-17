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
import org.jetbrains.exposed.sql.transactions.transaction

import com.flightsystem.model.*

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:h2:./database;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = "",
    )

    transaction {
        addLogger(StdOutSqlLogger)

        SchemaUtils.create(
            Airports,
            Users,
            Flights,
            Layovers,
            Seats,
            Bookings,
            Payments,
            BookingSeats
        )
    }
}
