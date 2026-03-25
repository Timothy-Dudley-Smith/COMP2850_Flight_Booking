package com.example.com

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.Serializable
import com.flightsystem.model.*
import java.io.File

@Serializable
data class FlightResponse(
    val flightId: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val price: Double,
    val date: String,
    val departureTime: String,  
    val arrivalTime: String,    
    val length: Double
)

fun Application.configureRouting() {

    
    routing {

        staticResources("/", "static/home")
        staticResources("/log_in", "static/log_in")

        get("/book") {
            call.respondFile(File("src/main/resources/static/home/book.html"))
        }

        get("/api/all-flights") {
            val flights = transaction {
                Flights.selectAll().map {
                    "${it[Flights.flightId]} | ${it[Flights.departureAirport]} → ${it[Flights.arrivalAirport]} | ${it[Flights.date]}"
            
                }
            }
            call.respond(flights)
        }
    



        get("/api/flights") {
            val from = call.request.queryParameters["from"]
            val to   = call.request.queryParameters["to"]
            val date = call.request.queryParameters["date"]

            val flights = transaction {
                var query = Flights.selectAll()
                if (from != null) query = query.andWhere { Flights.departureAirport eq from }
                if (to   != null) query = query.andWhere { Flights.arrivalAirport   eq to   }
                if (date != null) query = query.andWhere { Flights.date             eq date  }

                query.map {
                    FlightResponse(
                        flightId         = it[Flights.flightId],
                        departureAirport = it[Flights.departureAirport],
                        arrivalAirport   = it[Flights.arrivalAirport],
                        price            = it[Flights.price],
                        date             = it[Flights.date],
                        departureTime    = it[Flights.departureTime],
                        arrivalTime      = it[Flights.arrivalTime],
                        length           = it[Flights.length]
                    )
                }
            }
            call.respond(flights)
        }
    }
}

