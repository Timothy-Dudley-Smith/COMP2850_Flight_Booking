package com.flightsystem.model

import org.jetbrains.exposed.sql.Table

const val VARCHAR_LENGTH = 128

data class Airport(
    val code: String,
    val name: String,
    val city: String,
    val country: String
)

object Airports: Table() {
    val code = varchar("code", 3)
    val name = varchar("name", VARCHAR_LENGTH)
    val city = varchar("city", VARCHAR_LENGTH)
    val country = varchar("country", VARCHAR_LENGTH)

    override val primaryKey = PrimaryKey(code)
}