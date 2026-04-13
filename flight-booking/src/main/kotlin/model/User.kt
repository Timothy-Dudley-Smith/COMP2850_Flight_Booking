package com.flightsystem.model

import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable

open class User(
    val userId: Int,
    var firstName: String,
    var lastName:  String,
    var dateOfBirth: String,
    var email: String,
    private var passwordHash: String,
    private var salt: String
) {


    private var lastLogin: String = "00:00"
    private var accountLocked: Boolean = false
    private var lockedAt: String = "00:00"
    private var failedLoginAttempts: Int = 0
    private var seatPreference = "ANY"

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_MINUTES = 30L


    }

    fun updateDetails(newFirstName: String, newLastName: String, newEmail: String) {
        require(newFirstName.isNotBlank()) {"First Name cannot be empty"}
        require(newLastName.isNotBlank()) {"Last Name cannot be empty"}
        require(EMAIL_REGEX.matches(newEmail)) {"Invalid email format"}

        firstName = newFirstName
        lastName = newLastName
        email = newEmail
    }

    fun updatePassword(newPasswordHash: String, newSalt: String) {
        passwordHash = newPasswordHash
        salt = newSalt
    }

    fun verifyPassword(candidateHash: String): Boolean {
        return candidateHash == passwordHash
    }

    fun getSalt(): String {
        return salt
    }

    fun setSeatPreference(preference: String) {
        seatPreference = preference
    }

    fun getSeatPreference(): String = seatPreference

    fun recordLoginSuccess() {
        failedLoginAttempts = 0
        accountLocked = false
        lockedAt = "00:00"
        lastLogin = LocalDateTime.now().toString()
    }

    fun recordLoginFailure() {
        failedLoginAttempts++
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS ) {
            accountLocked = true
            lockedAt = LocalDateTime.now().toString()
        }
    }

    fun isLocked(): Boolean {
        if (accountLocked && lockedAt != "00:00") {
            if (LocalDateTime.now().isAfter(LocalDateTime.parse(lockedAt)!!.plusMinutes(LOCKOUT_MINUTES))) {
                unlockAccount()
            }
        }

        return accountLocked
    }

    fun unlockAccount() {
        accountLocked = false
        failedLoginAttempts = 0
        lockedAt = "00:00"
    }

    fun getLastLogin(): LocalDateTime = LocalDateTime.parse(lastLogin)



}

