package com.example.com

import com.flightsystem.model.*
import com.flightsystem.service.BookingService
import io.ktor.http.*
//import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
//import io.ktor.server.pebble.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
//import io.pebbletemplates.pebble.loader.ClasspathLoader
import org.jetbrains.exposed.sql.*
import io.ktor.server.http.content.*
//import org.h2.api.H2Type.row
import org.jetbrains.exposed.sql.transactions.transaction

import java.io.File

data class CreateBookingRequest(
    val userId: Int,
    val flightId: String,
    val seatNumbers: List<String>
)

fun Application.configureRouting() {
    routing {

        staticResources("/", "static/home")
        staticResources("/log_in", "static/log_in")

        get("/book") {
            call.respondFile(
                File("src/main/resources/static/home/book.html")
            )
        }
        get("/api/airports") {                          //get airport data for the drop-down search menu
            val airportData = transaction {
                Airports.selectAll().orderBy(Airports.country).map { row ->
                    mapOf(
                        "code" to row[Airports.code],
                        "name" to row[Airports.name],
                        "country" to row[Airports.country],
                        "city" to row[Airports.city],
                    )
                }
            }
            call.respond(airportData)
        }
        get("/api/flights") {
            val flights = transaction {
                Flights.selectAll().map { row ->
                    mapOf(
                        "flightId" to row[Flights.flightId],
                        "departureAirport" to row[Flights.departureAirport],
                        "arrivalAirport" to row[Flights.arrivalAirport],
                        "departureTime" to row[Flights.departureTime],
                        "arrivalTime" to row[Flights.arrivalTime],
                        "price" to row[Flights.price]
                    )
                }
            }
            call.respond(flights)
        }
        
        val bookingService = BookingService()

        post("/api/bookings") {
            val request = call.receive<CreateBookingRequest>()

            try {
                val booking = bookingService.createBooking(
                    request.userId,
                    request.flightId,
                    request.seatNumbers
                )
                call.respond(HttpStatusCode.OK, booking)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Error")
            }
        }
    }
}

