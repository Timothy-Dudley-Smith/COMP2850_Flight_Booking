package com.flightsystem.service

import com.flightsystem.service.EncryptionService
import com.flightsystem.service.LoyaltyService
import com.flightsystem.model.Manager
import com.flightsystem.model.User
import com.flightsystem.model.Users
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.UUID



class AuthenticationService(
    private val sessionTimeout: Long = 30L
) {

    private val activeSessions: MutableMap<String, SessionData> = mutableMapOf()

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_MINUTES = 30L
    }


    private data class SessionData(
        val userId: Int,
        val isManager: Boolean,
        var lastActivity: LocalDateTime = LocalDateTime.now()
    )


    fun register(firstName: String, lastName: String, dateOfBirth: String, email: String, password: String): Result<User> {
        if (firstName.isBlank()) {
            return Result.failure(IllegalArgumentException(" First Name can not be left blank"))

        }

        if (lastName.isBlank()) {
            return Result.failure(IllegalArgumentException("Last name cannot be left blank"))
        }

        if (dateOfBirth.isBlank()) {
            return Result.failure(IllegalArgumentException("Date of birth cannot be left blank"))
        }
        if (!EMAIL_REGEX.matches(email)) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException("Password must be at least $MIN_PASSWORD_LENGTH characters"))
        }

        return transaction {
            val existing = Users.selectAll().where {
                Users.email eq email
            }.singleOrNull()

            if (existing != null) {
                return@transaction Result.failure(
                    IllegalArgumentException("An account with this email already exists")
                )
            }

            val salt = EncryptionService.generateSalt()
            val passwordHash = EncryptionService.hashPassword(password, salt)

            val inserted = Users.insert {
                it[Users.firstName] = firstName
                it[Users.lastName] = lastName
                it[Users.dateOfBirth] = dateOfBirth
                it[Users.email] = email
                it[Users.passwordHash] = passwordHash
                it[Users.salt] = salt
                it[Users.seatPreference] = "ANY"
                it[Users.accountLocked] = false
                it[Users.failedLoginAttempts] = 0
                it[Users.lockedAt] = null
                it[Users.lastLogin] = null
                it[Users.role] = "USER"
            }

            val newUserId = inserted[Users.userId]

            LoyaltyService().createLoyaltyAccount(newUserId)

            Result.success(
                User(
                    userId = newUserId,
                    firstName = firstName,
                    lastName = lastName,
                    dateOfBirth = dateOfBirth,
                    email = email,
                    passwordHash = passwordHash,
                    salt = salt
                )
            )
        }

    }

    fun registerManager(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        email: String,
        rawPassword: String
    ): Result<Manager> {
        if (firstName.isBlank()) {
            return Result.failure(IllegalArgumentException(" First Name cannot be left blank"))
        }

        if (lastName.isBlank()) {
            return Result.failure(IllegalArgumentException(" Last Name cannot be left blank"))
        }

        if (dateOfBirth.isBlank()) {
            return Result.failure(IllegalArgumentException(" Date of birth cannot be left blank"))
        }

        if (!EMAIL_REGEX.matches(email)) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }

        if (rawPassword.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }


        return transaction {
            val existing = Users.selectAll().where {
                Users.email eq email
            }.singleOrNull()

            if (existing != null) {
                return@transaction Result.failure(
                    IllegalArgumentException("An account with this email already exists")
                )
            }

            val salt = EncryptionService.generateSalt()
            val passwordHash = EncryptionService.hashPassword(rawPassword, salt)

            val inserted = Users.insert {
                it[Users.firstName] = firstName
                it[Users.lastName] = lastName
                it[Users.dateOfBirth] = dateOfBirth
                it[Users.email] = email
                it[Users.passwordHash] = passwordHash
                it[Users.salt] = salt
                it[Users.seatPreference] = "ANY"
                it[Users.accountLocked] = false
                it[Users.failedLoginAttempts] = 0
                it[Users.lockedAt] = null
                it[Users.lastLogin] = null
                it[Users.role] = "MANAGER"
            }

            val newManagerId = inserted[Users.userId]

            LoyaltyService().createLoyaltyAccount(newManagerId)

            Result.success(
                Manager(
                    userId = newManagerId,
                    firstName = firstName,
                    lastName = lastName,
                    dateOfBirth = dateOfBirth,
                    email = email,
                    passwordHash = passwordHash,
                    salt = salt
                )
            )
        }
    }


    fun login(email: String, rawPassword: String): Result<User> {
        return transaction {
            val row = Users.selectAll().where {
                Users.email eq email
            }.singleOrNull()
                ?: return@transaction Result.failure(
                    IllegalArgumentException("User not found")
                )

            if (isLocked(row)) {
                return@transaction Result.failure(
                    IllegalStateException("Account is locked. Try again later")
                )
            }


            val storedHash = row[Users.passwordHash]
            val salt = row[Users.salt]

            val isValid = EncryptionService.verifyPassword(
                inputPassword = rawPassword,
                storedHash = storedHash,
                salt = salt
            )

            if (isValid) {
                Users.update({ Users.userId eq row[Users.userId] }) {
                    it[Users.failedLoginAttempts] = 0
                    it[Users.accountLocked] = false
                    it[Users.lockedAt] = null
                    it[Users.lastLogin] = LocalDateTime.now().toString()

                }

                return@transaction Result.success(rowToUser(row))
            } else {
                val newAttempts = row[Users.failedLoginAttempts] + 1
                val shouldLock = newAttempts >= MAX_FAILED_ATTEMPTS

                Users.update({ Users.userId eq row[Users.userId] }) {
                    it[Users.failedLoginAttempts] = newAttempts
                    it[Users.accountLocked] = shouldLock
                    it[Users.lockedAt] = if (shouldLock) LocalDateTime.now().toString() else null
                }

                return@transaction Result.failure(
                    IllegalArgumentException("Invalid email or password")
                )

            }

        }

    }

    // create session
    fun createSession(user: User): String {
        val sessionId = UUID.randomUUID().toString()
        activeSessions[sessionId] = SessionData(
            userId = user.userId,
            isManager = user is Manager,
            lastActivity = LocalDateTime.now()
        )
        return sessionId
    }

    fun validateSession(sessionId: String): User? {
        val session = activeSessions[sessionId] ?: return null
        val expiryTime = session.lastActivity.plusMinutes(sessionTimeout)
        
        if (LocalDateTime.now().isAfter(expiryTime)) {
            activeSessions.remove(sessionId)
            return null
        }
        session.lastActivity = LocalDateTime.now()
        return findById(session.userId)
    }

    fun logout(sessionId: String) {
        activeSessions.remove(sessionId)
    }

    fun isManagerSession(sessionId: String): Boolean {
        val session = activeSessions[sessionId] ?: return false
        val expiryTime = session.lastActivity.plusMinutes(sessionTimeout)

        if (LocalDateTime.now().isAfter(expiryTime)) {
            activeSessions.remove(sessionId)
            return false
        }
        session.lastActivity = LocalDateTime.now()
        return session.isManager
    }


    fun resetPassword(user: User, newRawPassword: String): Result<Unit> {
        if (newRawPassword.length < MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }

        return transaction {
            val newSalt = EncryptionService.generateSalt()
            val newHash = EncryptionService.hashPassword(newRawPassword, newSalt)

            Users.update({ Users.userId eq user.userId }) {
                it[Users.passwordHash] = newHash
                it[Users.salt] = newSalt
            }

            user.updatePassword(newHash, newSalt)
            Result.success(Unit)
        }
    }

    fun findByEmail(email: String): User? {
        return transaction {
            Users.selectAll().where {
                Users.email eq email
            }.singleOrNull()?.let { rowToUser(it) }
        }
    }

    fun findById(userId: Int): User? {
        return transaction {
            Users.selectAll().where {
                Users.userId eq userId
            }.singleOrNull()?.let { rowToUser(it) }
        }
    }

    fun getAllUsers(): List<User> {
        return transaction {
            Users.selectAll().map { rowToUser(it) }
        }
    }

    private fun rowToUser(row: ResultRow): User {
        val role = row[Users.role]

        return if (role == "MANAGER") {
            Manager(
                userId = row[Users.userId],
                firstName = row[Users.firstName],
                lastName = row[Users.lastName],
                dateOfBirth = row[Users.dateOfBirth],
                email = row[Users.email],
                passwordHash = row[Users.passwordHash],
                salt = row[Users.salt]
            )
        } else {
            User(
                userId = row[Users.userId],
                firstName = row[Users.firstName],
                lastName = row[Users.lastName],
                dateOfBirth = row[Users.dateOfBirth],
                email = row[Users.email],
                passwordHash = row[Users.passwordHash],
                salt = row[Users.salt]
            )

        }

    }

    private fun isLocked(row: ResultRow): Boolean {
        val accountLocked = row[Users.accountLocked]
        val lockedAtString = row[Users.lockedAt]

        if (!accountLocked) {
            return false
        }

        if (lockedAtString == null) {
            return true
        }

        val lockedAt = LocalDateTime.parse(lockedAtString)
        val unlockTime = lockedAt.plusMinutes(LOCKOUT_MINUTES)

        if (LocalDateTime.now().isAfter(unlockTime)) {
            Users.update({ Users.userId eq row[Users.userId] }) {
                it[Users.accountLocked] = false
                it[Users.failedLoginAttempts] = 0
                it[Users.lockedAt] = null
            }
            return false
        }

        return true
    }


}



