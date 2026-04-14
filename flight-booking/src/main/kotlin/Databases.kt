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
            BookingSeats,
            PriceHolds,
            PriceHoldSeats,
            Passengers,
            LoyaltyAccounts
        )

        val columns = listOf("A","B","C","D","E","F")
        val flights = Flights.selectAll().map { it[Flights.flightId] }
        for (flightId in flights) {
            val existingSeats = Seats.selectAll().where { Seats.flightId eq flightId }.count()
            if (existingSeats == 0L) {
                for (row in 1..12) {
                    for (col in columns) {
                        Seats.insert {
                            it[Seats.flightId] = flightId
                            it[Seats.seatNumber] = "$row$col"
                            it[Seats.isAvailable] = true
                        }
                    }
                }
            }
        }
    }
}
