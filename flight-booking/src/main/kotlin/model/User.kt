package com.flightsystem.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
Represents a registered user in the system.

Stores personal details, authentication data, and account state such as lockout and login tracking.
 */

@Serializable
open class User(
    val userId: Int,
    var firstName: String,
    var lastName: String,
    var dateOfBirth: String,
    var email: String,
    private var passwordHash: String,
    private var salt: String,
) {
    private var lastLogin: String? = null
    private var accountLocked: Boolean = false
    private var lockedAt: String = "00:00"
    private var failedLoginAttempts: Int = 0
    private var seatPreference = "ANY"

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_MINUTES = 30L
    }

    /**
     Updates user details after validating name fields and email format.
     */

    fun updateDetails(
        newFirstName: String,
        newLastName: String,
        newEmail: String,
    ) {
        require(newFirstName.isNotBlank()) { "First Name cannot be empty" }
        require(newLastName.isNotBlank()) { "Last Name cannot be empty" }
        require(EMAIL_REGEX.matches(newEmail)) { "Invalid email format" }

        firstName = newFirstName
        lastName = newLastName
        email = newEmail
    }

    /**
     Updates the stored password hash and salt for the user.
     */

    fun updatePassword(
        newPasswordHash: String,
        newSalt: String,
    ) {
        passwordHash = newPasswordHash
        salt = newSalt
    }

    /**
     Checks whether a given password hash matches the stored hash.

     @return true if the password is correct
     */

    fun verifyPassword(candidateHash: String): Boolean = candidateHash == passwordHash

    /**
     Returns the salt associated with the user's password.
     */

    fun getSalt(): String = salt

    /**
     Sets seat preference for the user (not currently used)
     */

    fun setSeatPreference(preference: String) {
        seatPreference = preference
    }

    fun getSeatPreference(): String = seatPreference

    /**
     Resets failed login attempts and updates last login timestamp.
     */

    fun recordLoginSuccess() {
        failedLoginAttempts = 0
        accountLocked = false
        lockedAt = "00:00"
        lastLogin = LocalDateTime.now().toString()
    }

    /**
     Increments failed login attempts and locks the account if the limit is reached.
     */

    fun recordLoginFailure() {
        failedLoginAttempts++
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            accountLocked = true
            lockedAt = LocalDateTime.now().toString()
        }
    }

    /**
     Checks whether the account is locked and unlocks it if the lockout period has expired.
     */

    fun isLocked(): Boolean {
        if (accountLocked && lockedAt != "00:00") {
            if (LocalDateTime.now().isAfter(LocalDateTime.parse(lockedAt)!!.plusMinutes(LOCKOUT_MINUTES))) {
                unlockAccount()
            }
        }

        return accountLocked
    }

    /**
     Unlocks the account and resets failed login attempts.
     */

    fun unlockAccount() {
        accountLocked = false
        failedLoginAttempts = 0
        lockedAt = "00:00"
    }

    fun getLastLogin(): LocalDateTime? = lastLogin?.let { LocalDateTime.parse(it) }
}
