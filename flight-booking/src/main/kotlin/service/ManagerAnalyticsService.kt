package com.example.com.service

import com.example.com.FlightBookingCount
import com.example.com.HourlyBookingCount
import com.example.com.ManagerAnalyticsResponse
import com.example.com.Route
import com.example.com.RouteBookingCount
import com.flightsystem.model.Bookings
import com.flightsystem.model.Flights
import io.ktor.server.routing.route
import model.SupportTickets
import model.TicketStatus
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

/**
 * Provides the functions required for the analytics page
 */
class ManagerAnalyticsService {
    fun getAnalytics(): ManagerAnalyticsResponse {
        val totalBookings =
            transaction {
                Bookings.selectAll().count().toInt()
            }
        val upcomingFlights =
            transaction {
                Flights
                    .selectAll()
                    .where { Flights.date greaterEq LocalDate.now().toString() }
                    .count()
                    .toInt()
            }

        val openTickets =
            transaction {
                SupportTickets
                    .selectAll()
                    .where {
                        (SupportTickets.status eq TicketStatus.OPEN) or
                            (SupportTickets.status eq TicketStatus.IN_PROGRESS)
                    }.count()
                    .toInt()
            }

        val allBookings =
            transaction {
                Bookings.selectAll().toList()
            }
        val bookingTimeCounts = mutableListOf<Int>()
        for (i in 0..23) {
            bookingTimeCounts.add(0)
        }

        for (booking in allBookings) {
            val bookingTime = booking[Bookings.time]
            val hour = bookingTime.take(2).toInt()
            if (hour in 0..23) {
                bookingTimeCounts[hour] = bookingTimeCounts[hour].plus(1)
            }
        }

        val bookingsPerHour =
            bookingTimeCounts.mapIndexed { hour, count ->
                HourlyBookingCount(
                    hour = hour,
                    count = count,
                )
            }

        val allRoutes = mutableListOf<Route>()
        val allFlights =
            transaction {
                Flights.selectAll().toList()
            }
        val flightBookingCounter = mutableMapOf<String, Int>()

        // create list of all routes
        for (flight in allFlights) {
            val arrivalAirport = flight[Flights.arrivalAirport]
            val departureAirport = flight[Flights.departureAirport]
            val route = Route(departureAirport, arrivalAirport)
            if (route !in allRoutes) {
                allRoutes.add(route)
            }
        }

        // Count Bookings per flight
        for (booking in allBookings) {
            val flightId = booking[Bookings.flightId]
            val currentCount = flightBookingCounter[flightId] ?: 0
            flightBookingCounter[flightId] = currentCount.plus(1)
        }

        val bookingsPerFlight =
            allFlights.map { flight ->
                val flightId = flight[Flights.flightId]

                FlightBookingCount(
                    flightId = flightId,
                    date = flight[Flights.date],
                    departureAirport = flight[Flights.departureAirport],
                    arrivalAirport = flight[Flights.arrivalAirport],
                    bookingCount = flightBookingCounter[flightId] ?: 0,
                )
            }

        // Count bookings per route
        val routeBookingCounts = mutableMapOf<String, Int>()

        for (flight in bookingsPerFlight) {
            val arrivalAirport = flight.arrivalAirport
            val departureAirport = flight.departureAirport
            val bookingCount = flight.bookingCount
            val routeKey = "${departureAirport}$arrivalAirport"

            val currentCount = routeBookingCounts[routeKey] ?: 0
            routeBookingCounts[routeKey] = currentCount.plus(bookingCount)
        }

        val bookingsPerRoute =
            allRoutes.map { route ->
                val departureAirport = route.departureAirport
                val arrivalAirport = route.arrivalAirport
                val routeKey = "${departureAirport}$arrivalAirport"

                RouteBookingCount(
                    arrivalAirport = arrivalAirport,
                    departureAirport = departureAirport,
                    bookingCount = routeBookingCounts[routeKey] ?: 0,
                )
            }

        val popularRoutes = bookingsPerRoute.sortedByDescending { it.bookingCount }
        val mostPopularRoute = popularRoutes.first()

        val managerAnalyticsResponse =
            ManagerAnalyticsResponse(
                totalBookings = totalBookings,
                upcomingFlights = upcomingFlights,
                openTickets = openTickets,
                mostPopularRoute = mostPopularRoute,
                bookingsPerHour = bookingsPerHour,
                popularRoutes = popularRoutes,
                bookingsPerRoute = bookingsPerRoute,
                bookingsPerFlight = bookingsPerFlight,
            )

        return managerAnalyticsResponse
    }
}
