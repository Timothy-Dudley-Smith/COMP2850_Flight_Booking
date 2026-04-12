package com.flightsystem.model

import org.jetbrains.exposed.sql.Table

// one loyalty account per user with a current points balance 
data class LoyaltyAccount(
    val userId: Int,
    val loyaltyPoints: Int
)

// store the account owner and current loyalty points balance
object LoyaltyAccounts : Table() {
    val userId = reference("userId", Users.userId)
    val loyaltyPoints = integer("loyaltyPoints")

    // each user can only have 1 loyalty account
    override val primaryKey = PrimaryKey(userId)
}


