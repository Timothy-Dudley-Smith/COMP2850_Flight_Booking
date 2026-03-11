package com.flightsystem.model

import org.jetbrains.exposed.sql.Table

data class User(
    val userId: Int,
    val name: String,
    val email: String,
    val password: String
)

object Users : Table() {
    val userId = integer("user_id").autoIncrement()
    val name = varchar("user_name", VARCHAR_LENGTH)
    val email = varchar("email", VARCHAR_LENGTH)
    val password = varchar("password", VARCHAR_LENGTH)

    override val primaryKey = PrimaryKey(userId)
}