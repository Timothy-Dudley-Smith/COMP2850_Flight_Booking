package com.flightsystem.model

import org.jetbrains.exposed.sql.Table

/**
Database table for tracking promo code usage.

Uses a composite primary key of user ID and promo code to prevent duplicate usage.
 */

object PromoCodeUsages : Table("promo_code_usages") {
    val userId = reference("user_id", Users.userId)
    val promoCode = reference("promo_code", PromoCodes.code)
    val usedAt = varchar("used_at", 50)

    override val primaryKey = PrimaryKey(userId, promoCode)
}
