package com.flightsystem.service

import com.flightsystem.model.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

// service for creating loyalty accounts and managing loyalty points 
class LoyaltyService {
    fun createLoyaltyAccount(userId: Int): LoyaltyAccount {
        return transaction {
            val existingRow = LoyaltyAccounts.selectAll().where {
                LoyaltyAccounts.userId eq userId
            }.singleOrNull()
            // if user has exising acc, ret existing one instead of creating a duplicate 
            if (existingRow != null) {
                return@transaction LoyaltyAccount(
                    userId = existingRow[LoyaltyAccounts.userId],
                    loyaltyPoints = existingRow[LoyaltyAccounts.loyaltyPoints]
                )
            }
            // new loyalty accounts start with a zero points balance 
            LoyaltyAccounts.insert {
                it[LoyaltyAccounts.userId] = userId
                it[LoyaltyAccounts.loyaltyPoints] = 0
            }
            LoyaltyAccount(
                userId = userId,
                loyaltyPoints = 0
            )
        }
    }

    // ret the loyalty acc for une user, or null if not exist
    fun getLoyaltyAccount(userId: Int): LoyaltyAccount? {
        return transaction {
            val row = LoyaltyAccounts.selectAll().where {
                LoyaltyAccounts.userId eq userId
            }.singleOrNull()
            if (row == null) {
                return@transaction null
            }
            LoyaltyAccount(
                userId = row[LoyaltyAccounts.userId],
                loyaltyPoints = row[LoyaltyAccounts.loyaltyPoints]
            )
        }
    }

    // add earned points to an existing loyalty account
    fun addPoints(userId: Int, points: Int): Boolean {
        return transaction {
            if (points <= 0) {
                return@transaction false
            }
            val accountRow = LoyaltyAccounts.selectAll().where {
                LoyaltyAccounts.userId eq userId
            }.singleOrNull()
            if (accountRow == null) {
                return@transaction false
            }
            val newBalance = accountRow[LoyaltyAccounts.loyaltyPoints] + points
            LoyaltyAccounts.update({
                LoyaltyAccounts.userId eq userId
            }) {
                it[loyaltyPoints] = newBalance
            }
            return@transaction true 
        }
    }

    // subtract redeemed points only if the account has enough balance 
    fun redeemPoints(userId: Int, points: Int): Boolean {
        return transaction {
            if (points <= 0) {
                return@transaction false
            }
            val accountRow = LoyaltyAccounts.selectAll().where {
                LoyaltyAccounts.userId eq userId
            }.singleOrNull()
            if (accountRow == null) {
                return@transaction false
            }
            val currentBalance = accountRow[LoyaltyAccounts.loyaltyPoints] 
            if (currentBalance < points) {
                return@transaction false
            }
            val newBalance = currentBalance - points
            LoyaltyAccounts.update({
                LoyaltyAccounts.userId eq userId
            }) {
                it[loyaltyPoints] = newBalance
            }
            return@transaction true
        }
    }

    // calculate discounted price from redeemed points without changing the db balacnce 
    fun applyDiscount(originalPrice: Double, pointsToRedeem: Int): Double {
        if (originalPrice < 0.0 || pointsToRedeem < 0) {
            throw IllegalArgumentException("Price and points must not be negative")
        }
        val discountAmount = pointsToRedeem / 100.0
        val discountedPrice = originalPrice - discountAmount
        return if (discountedPrice < 0.0) 0.0 else discountedPrice
    }
}



