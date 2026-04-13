package com.example.com

import com.flightsystem.model.Airport
import com.flightsystem.model.Airports
import com.flightsystem.model.Flights
import com.sun.org.apache.xalan.internal.lib.ExsltDatetime.time
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.transactions.transaction

import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

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

@Serializable
data class UpcomingFlightData(
    val flightId: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val price: Double,
    val date: String,
    val departureTime: String,
    val arrivalTime: String,
    val length: Double
)

@Serializable
data class InsertFlightData(
    val flightId: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val date: String,
    val departureTime: String,
    val arrivalTime: String,
    val length: Double,
    val price: Double
)




fun Application.configureRouting() {
    routing {

        staticResources("/", "static/home")
        staticResources("/log_in", "static/log_in")
        staticResources("/", "static/home")
        staticResources("/log_in", "static/log_in")
        staticResources("/home", "static/home")
        staticResources("/manager/flight_view", "static/manager/flight_view")

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
                //TODO:
                //Remove requirement for date in search

                Flights.selectAll().mapNotNull { row ->


                    val departure = row[Flights.departureAirport]
                    val arrival = row[Flights.arrivalAirport]
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


                    if (date != "" ) {
                        if (flightDate != date){
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
                    } else {
                        null
                        //lables flight as non matching (reject)
                    }

                }
                // removes all the rejected flights
            }
            call.respond(flightData)
        }

        get("/api/manager/flights") {
            //TODO:
            //Add manager only access - requires manager log-in key.
            val upcomingFlightData = transaction {
                Flights.selectAll().where { Flights.date greaterEq LocalDate.now().toString() }
                    .orderBy(Flights.date to SortOrder.ASC, Flights.departureTime to SortOrder.ASC).map { row ->
                    UpcomingFlightData (
                        flightId = row[Flights.flightId],
                        departureAirport = row[Flights.departureAirport],
                        arrivalAirport = row[Flights.arrivalAirport],
                        date = row[Flights.date],
                        departureTime = row[Flights.departureTime],
                        arrivalTime = row[Flights.arrivalTime],
                        price = row[Flights.price],
                        length = row[Flights.length]

                        //TODO:
                        //Add quantity of tickets sold / still available
                    )
                }
            }
            call.respond(upcomingFlightData)
        }

        get("/manager/flight_view") {
            call.respondFile(File("src/main/resources/static/manager/flight_view/flight_view.html"))

            //TODO:
            //Add ability to see historic flights
        }

        post("/api/manager/flight_view") {
            val request = call.receive<InsertFlightData>()

            //TODO:
            //validate flights attempted to be inserted


            transaction {
                Flights.insert {
                    it[flightId] = request.flightId
                    it[departureAirport] = request.departureAirport
                    it[arrivalAirport] = request.arrivalAirport
                    it[date] = request.date.toString()
                    it[departureTime] = request.departureTime
                    it[arrivalTime] = request.arrivalTime
                    it[length] = request.length
                    it[price] = request.price
                }
            }
            call.respond(HttpStatusCode.Created)
        }
    }

}