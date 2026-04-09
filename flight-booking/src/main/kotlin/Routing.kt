package com.example.com

import com.flightsystem.model.Airport
import com.flightsystem.model.Airports
import com.flightsystem.model.Flights
// imports the flight info
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

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
import org.h2.api.H2Type.row
import org.jetbrains.exposed.sql.transactions.transaction

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
        staticResources("/", "static/home")
        staticResources("/log_in", "static/log_in")
        staticResources("/home", "static/home")

        get("/book") {
            call.respondFile(
                File("src/main/resources/static/home/book.html")
            )
        }

        get("/booking-personal") {
            call.respondFile(
                File("src/main/resources/static/home/booking-personal.html")
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
            val from = call.request.queryParameters["from"]
            val to   = call.request.queryParameters["to"]
            val date = call.request.queryParameters["date"]
            val passengers = call.request.queryParameters["passengers"]

            //read user input from the URL so API can filter flights

            val flightData = transaction {
                Flights.selectAll().map { row ->


                    val departure = row[Flights.departureAirport]
                    val arrival   = row[Flights.arrivalAirport]
                    val flightDate = row[Flights.date]

                    //pull data from the database row into simple variable for comparison

                    var match = true
                    //boolean val used to check if results meet filters or not

                    if (from != null) {
                        if (departure != from) {
                            match = false
                        }
                    }
                    // if the user inputted a departure airport remove results with different departure airport

                    if (to != null) {
                        if (arrival != to) {
                            match = false
                        }
                    }
                    // if the user inputted an arrival airport remove results with different arrival airports


                    if (date != null) {
                        if (flightDate != date) {
                            match = false
                        }
                    }
                    // if the user inputted a date remove results with a different date

                    if (match == true) {
                        // only include flights that match everything

                        FlightResponse(
                            row[Flights.flightId],
                            row[Flights.departureAirport],
                            row[Flights.arrivalAirport],
                            row[Flights.price],
                            row[Flights.date],
                            row[Flights.departureTime],
                            row[Flights.arrivalTime],
                            row[Flights.length]
                        )
                    }
                    else {
                        null
                        //lables flight as non matching (reject)
                    }

                }.filterNotNull()
                // removes all the rejected flights
            }
            call.respond(flightData)
        }

    }

}