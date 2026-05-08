import com.flightsystem.model.SeatClass
import com.flightsystem.model.Seats
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
* On function call, every flight in the database is checked for a corresponding seat map
* if any flights don't have one, it creates one
* otherwise existing seat maps are left as is
*/

fun createEmptySeatMaps(flights: List<String>) {
    transaction {
        val columns = listOf("A", "B", "C", "D", "E", "F")
        for (flightId in flights) {
            val existingSeats = Seats.selectAll().where { Seats.flightId eq flightId }.count()
            if (existingSeats == 0L) {
                for (row in 1..12) {
                    val seatClass =
                        when (row) {
                            1, 2 -> SeatClass.BUSINESS
                            3, 4, 5 -> SeatClass.PREMIUM_ECONOMY
                            else -> SeatClass.ECONOMY
                        }

                    for (col in columns) {
                        Seats.insert {
                            it[Seats.flightId] = flightId
                            it[Seats.seatNumber] = "$row$col"
                            it[Seats.isAvailable] = true
                            it[Seats.seatClass] = seatClass
                        }
                    }
                }
            }
        }
    }
}
