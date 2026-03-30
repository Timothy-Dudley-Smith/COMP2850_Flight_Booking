package com.flightsystem.service

import com.flightsystem.model.EncryptionService
import com.flightsystem.model.Manager
import com.flightsystem.model.User
import java.time.LocalDateTime
import java.util.UUID


class AuthenticationService(
    private val userRepository: UserRepository,
    private val sessionTimeout: Long = 30L
) {


    private val activeSessions: MutableMap<String, SessionData> = mutableMapOf()

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MIN_PASSWORD_LENGTH = 8
    }


    private data class SessionData(
        val userId: String,
        val isManager, Boolean,
        val lastActivity: LocalDateTime.now()
    )


    fun register(name: String, email: String, password: String): Result<User> {
        if (name.isBlank())
            return Result.failure(IllegalArgumentException("Name can not be left blank"))

        if (!EMAIL_REGEX.matches(email))
            return Result.failure(IllegalArgumentException("Invalid email format"))

        if (password.length < MIN_PASSWORD_LENGTH)
            return Result.failure(IllegalArgumentException("Password must be at least $MIN_PASSWORD_LENGTH characters"))

        if (userRepository.findByEmail(email) != null)
            return Result.failure(IllegalArgumentException("An account with this email already exists"))

        val salt = EncryptionService.generateSalt()
        val passwordHash = EncryptionService.hashPassword(password, salt)

        val user = User(
            userId = UUID.randomUUID().toString(),
            name = name,
            email = email.
            passwordHash = passwordHash,
            salt = salt

        )

        userRepository.save(user)
        return Result.success(user)

    }
}