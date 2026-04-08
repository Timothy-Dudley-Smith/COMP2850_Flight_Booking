
package com.flightsystem.service


import com.flightsystem.service.EncryptionService
import com.flightsystem.model.Manager
import com.flightsystem.model.User
import java.time.LocalDateTime
import java.util.UUID


class AuthenticationService(
    private val sessionTimeout: Long = 30L
) {

    private val users: MutableList<User> = mutableListOf()
    private val activeSessions: MutableMap<String, SessionData> = mutableMapOf()

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MIN_PASSWORD_LENGTH = 8
    }


    private data class SessionData(
        val userId: String,
        val isManager: Boolean,
        val lastActivity: LocalDateTime = LocalDateTime.now()
    )


    fun register(name: String, email: String, password: String): Result<User> {
        if (name.isBlank())
            return Result.failure(IllegalArgumentException("Name can not be left blank"))

        if (!EMAIL_REGEX.matches(email))
            return Result.failure(IllegalArgumentException("Invalid email format"))

        if (password.length < MIN_PASSWORD_LENGTH)
            return Result.failure(IllegalArgumentException("Password must be at least $MIN_PASSWORD_LENGTH characters"))

        if (findByEmail(email) != null) {
            return Result.failure(
                IllegalArgumentException("An account with this email already exists")
            )

        }

        val salt = EncryptionService.generateSalt()
        val passwordHash = EncryptionService.hashPassword(password, salt)

        val user = User(
            userId = UUID.randomUUID().toString(),
            name = name,
            email = email,
            passwordHash = passwordHash,
            salt = salt

        )

        users.add(user)
        return Result.success(user)

    }

    fun registerManager(
        name: String,
        email: String,
        rawPassword: String
    ): Result<Manager> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be left blank"))
        }

        if (!EMAIL_REGEX.matches(email)) {
            return Result.failure(IllegalArgumentException("Email cannot be left blank"))
        }

        if (rawPassword.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }

        if (users.any {it.email.equals(email, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("An account with this email already exists"))
        }

        val salt = EncryptionService.generateSalt()
        val passwordHash = EncryptionService.hashPassword(rawPassword, salt)

        val manager = Manager(
            userId = UUID.randomUUID().toString(),
            name = name,
            email = email,
            passwordHash = passwordHash,
            salt = salt
        )

        users.add(manager)
        return Result.success(manager)
    }

    fun login(
        email: String,
        rawPassword: String
    ): Result<User> {
        val user = users.find {it.email.equals(email, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("User not found"))

        if (user.isLocked()) {
            return Result.failure(IllegalStateException("Account is locked. Try again"))
        }

        val candidateHash = EncryptionService.hashPassword(rawPassword, user.getSalt())

        return if (user.verifyPassword(candidateHash)) {
            user.recordLoginSuccess()
            Result.success(user)
        } else {
            user.recordLoginFailure()
            Result.failure(IllegalArgumentException("Invalid email or password"))
        }
    }

    fun resetPassword(
        user: User,
        newRawPassword: String
    ): Result<Unit> {
        if (newRawPassword.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }

        val newSalt = EncryptionService.generateSalt()
        val newHash = EncryptionService.hashPassword(newRawPassword, newSalt)

        user.updatePassword(newHash, newSalt)
        return Result.success(Unit)
    }

    fun findByEmail(email: String): User? {
        return users.find {it.email.equals(email, ignoreCase = true) }
    }

    fun findById(userId: String): User? {
        return users.find {it.userId == userId }
    }

    fun getAllUsers(): List<User> {
        return users.toList()
    }
}

