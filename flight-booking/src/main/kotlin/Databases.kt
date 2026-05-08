package com.example.com

import com.flightsystem.flightservice.AuthenticationService
import com.flightsystem.flightservice.PromoCodeService
import com.flightsystem.model.Airports
import com.flightsystem.model.BookingSeats
import com.flightsystem.model.Bookings
import com.flightsystem.model.Flights
import com.flightsystem.model.Layovers
import com.flightsystem.model.LoyaltyAccounts
import com.flightsystem.model.Passengers
import com.flightsystem.model.Payments
import com.flightsystem.model.PriceHoldSeats
import com.flightsystem.model.PriceHolds
import com.flightsystem.model.PromoCodeUsages
import com.flightsystem.model.PromoCodes
import com.flightsystem.model.Seats
import com.flightsystem.model.Users
import createEmptySeatMaps
import io.ktor.server.application.Application
import model.ManagerSentEmails
import model.SupportTicketHistory
import model.SupportTickets
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val database =
        Database.connect(
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
            LoyaltyAccounts,
            PromoCodes,
            PromoCodeUsages,
            SupportTickets,
            SupportTicketHistory,
            ManagerSentEmails,
        )

        try {
            exec("""ALTER TABLE BOOKINGS ADD COLUMN "date" VARCHAR(255)""")
        } catch (e: Exception) {
            println("date column already exists")
        }

        try {
            exec("""ALTER TABLE BOOKINGS ADD COLUMN "time" VARCHAR(255)""")
        } catch (e: Exception) {
            println("time column already exists")
        }

        try {
            exec("""ALTER TABLE BOOKINGS ADD COLUMN cabin VARCHAR(128)""")
        } catch (e: Exception) {
            println("cabin column already exists")
        }

        try {
            exec("""ALTER TABLE BOOKINGS ADD COLUMN "addOns" VARCHAR(1000)""")
        } catch (e: Exception) {
            println("addOns column already exists")
        }

        try {
            exec("""ALTER TABLE SUPPORT_TICKETS ADD COLUMN ARCHIVED BOOLEAN DEFAULT FALSE""")
        } catch (e: Exception) {
            println("archived column already exists")
        }

        try {
            exec("""ALTER TABLE BOOKINGS ADD COLUMN "returnFlightId" VARCHAR(128)""")
        } catch (e: Exception) {
            println("returnFlightId column already exists in BOOKINGS")
        }

        try {
            exec("""ALTER TABLE PRICEHOLDS ADD COLUMN "returnFlightId" VARCHAR(128)""")
        } catch (e: Exception) {
            println("returnFlightId column already exists in PRICEHOLDS")
        }

        try {
            exec("""ALTER TABLE BOOKINGS ADD COLUMN "date" VARCHAR(255)""")
        } catch (e: Exception) {
            println("date column already exists")
        }

        try {
            exec("""ALTER TABLE BOOKINGS ADD COLUMN "time" VARCHAR(255)""")
        } catch (e: Exception) {
            println("time column already exists")
        }

        try {
            exec("ALTER TABLE USERS ADD COLUMN STATUS VARCHAR(30) DEFAULT 'ACTIVE'")
        } catch (e: Exception) {
            println("status column already exists")
        }

        val authservice = AuthenticationService()

        authservice.setDefaultManager(
            firstName = "Admin",
            lastName = "User",
            dateOfBirth = "1990-01-01",
            email = "manager@astraeus.com",
            rawPassword = "password123",
        )

        PromoCodeService().makeDefaultPromoCodes()

        val flights = Flights.selectAll().map { it[Flights.flightId] }

        createEmptySeatMaps(flights)
    }
}
