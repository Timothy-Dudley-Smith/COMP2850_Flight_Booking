package com.flightsystem.model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

/**
Represents a promotional discount code.

Stores the code value, discount type, discount amount, and whether the code is active.
 */

@Serializable
data class PromoCode(
    val code: String,
    val discountType: String,
    val discountValue: Double,
    val isActive: Boolean,
)

/**
Database table for storing promotional discount codes.

Used by checkout to apply percentage or fixed-value discounts.
 */

object PromoCodes : Table() {
    val code = varchar("code", 50)
    val discountType = varchar("discountType", 20)
    val discountValue = double("discountValue")
    val isActive = bool("isActive").default(true)

    override val primaryKey = PrimaryKey(code)
}
