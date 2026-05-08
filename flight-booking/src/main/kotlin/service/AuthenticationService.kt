package com.flightsystem.flightservice

import com.flightsystem.model.AccountStatus
import com.flightsystem.model.Manager
import com.flightsystem.model.User
import com.flightsystem.model.Users
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
Handles user authentication, registration, session management, and OTP verification.

Includes login security such as account lockout and password validation.
 */

class AuthenticationService(
    private val sessionTimeout: Long = 30L,
) {
    private val activeSessions: MutableMap<String, SessionData> = mutableMapOf()

    private data class OtpData(
        val userId: Int,
        val otp: String,
        val expiry: LocalDateTime,
    )

    // used for the otp maps user to an otp and expiry time
    private val pendingOtps: MutableMap<String, OtpData> = ConcurrentHashMap()
    // maps user email to otp

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_MINUTES = 30L
    }

    private data class SessionData(
        val userId: Int,
        val isManager: Boolean,
        var lastActivity: LocalDateTime = LocalDateTime.now(),
    )

    /**
     Registers a new user account after validating input and hashing the password.

     Also creates a loyalty account for the user.
     */

    fun register(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        email: String,
        password: String,
    ): Result<User> {
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
            val existing =
                Users
                    .selectAll()
                    .where {
                        Users.email eq email
                    }.singleOrNull()

            if (existing != null) {
                return@transaction Result.failure(
                    IllegalArgumentException("An account with this email already exists"),
                )
            }

            val salt = EncryptionService.generateSalt()
            val passwordHash = EncryptionService.hashPassword(password, salt)

            val inserted =
                Users.insert {
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
                    it[Users.status] = AccountStatus.ACTIVE
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
                    salt = salt,
                ),
            )
        }
    }

    /**
     Registers a new manager account with elevated privileges.
     */

    fun registerManager(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        email: String,
        rawPassword: String,
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
            val existing =
                Users
                    .selectAll()
                    .where {
                        Users.email eq email
                    }.singleOrNull()

            if (existing != null) {
                return@transaction Result.failure(
                    IllegalArgumentException("An account with this email already exists"),
                )
            }

            val salt = EncryptionService.generateSalt()
            val passwordHash = EncryptionService.hashPassword(rawPassword, salt)

            val inserted =
                Users.insert {
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
                    it[Users.status] = AccountStatus.ACTIVE
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
                    salt = salt,
                ),
            )
        }
    }

    /**
     Authenticates a user using email and password.

     Handles account lockout, failed attempts, and updates last login time.
     */

    fun login(
        email: String,
        rawPassword: String,
    ): Result<User> {
        return transaction {
            val row =
                Users
                    .selectAll()
                    .where {
                        Users.email eq email
                    }.singleOrNull()
                    ?: return@transaction Result.failure(
                        IllegalArgumentException("User not found"),
                    )

            if (isLocked(row)) {
                return@transaction Result.failure(
                    IllegalStateException("Account is locked. Try again later"),
                )
            }

            val status = row[Users.status]

            if (status != AccountStatus.ACTIVE) {
                return@transaction Result.failure(IllegalStateException("Account is not Active"))
            }

            val storedHash = row[Users.passwordHash]
            val salt = row[Users.salt]

            val isValid =
                EncryptionService.verifyPassword(
                    inputPassword = rawPassword,
                    storedHash = storedHash,
                    salt = salt,
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
                    IllegalArgumentException("Invalid email or password"),
                )
            }
        }
    }

    /**
     Creates a session for an authenticated user.

     @return session ID
     */

    fun createSession(user: User): String {
        val sessionId = UUID.randomUUID().toString()
        activeSessions[sessionId] =
            SessionData(
                userId = user.userId,
                isManager = user is Manager,
                lastActivity = LocalDateTime.now(),
            )
        return sessionId
    }

    /**
     Generates a one-time password (OTP) for additional verification.
     */

    fun createOtpChallenge(user: User): String {
        val otp = (100000..999999).random().toString()
        pendingOtps[user.email] = OtpData(user.userId, otp, LocalDateTime.now().plusMinutes(5))
        return otp
    }

    /**
     Verifies a submitted OTP and ensures it has not expired.
     */

    fun verifyOtp(
        email: String,
        otp: String,
    ): Result<User> {
        val data = pendingOtps.remove(email)
        // remove the email mapping so the otp the user entered is the only thing left

        if (data == null) {
            return Result.failure(IllegalArgumentException("Invalid or expired OTP"))
        }

        if (LocalDateTime.now().isAfter(data.expiry)) {
            return Result.failure(IllegalArgumentException("OTP has expired"))
        }
        // if user took more then 5 mins to enter otp

        if (data.otp != otp) {
            return Result.failure(IllegalArgumentException("Incorrect OTP"))
        }
        // if inputted otp doesnt match actual otp

        val user = findById(data.userId)

        if (user == null) {
            return Result.failure(IllegalArgumentException("User not found"))
        }

        return Result.success(user)
    }

    /**
     Validates a session and checks expiry and account status.

     @return User if session is valid, otherwise null
     */

    fun validateSession(sessionId: String): User? {
        val session = activeSessions[sessionId] ?: return null
        val expiryTime = session.lastActivity.plusMinutes(sessionTimeout)

        if (LocalDateTime.now().isAfter(expiryTime)) {
            activeSessions.remove(sessionId)
            return null
        }

        val userRow =
            transaction {
                Users.selectAll().where { Users.userId eq session.userId }.singleOrNull()
            }

        if (userRow == null) {
            activeSessions.remove(sessionId)
        }

        val status = userRow?.get(Users.status)

        if (status != AccountStatus.ACTIVE) {
            activeSessions.remove(sessionId)
            return null
        }

        session.lastActivity = LocalDateTime.now()
        return findById(session.userId)
    }

    /**
     Removes an active session.
     */

    fun logout(sessionId: String) {
        activeSessions.remove(sessionId)
    }

    /**
     Checks whether a session belongs to a manager user.
     */

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

    /**
     Resets a user's password by generating a new salt and hash.
     */

    fun resetPassword(
        user: User,
        newRawPassword: String,
    ): Result<Unit> {
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

    /**
     Finds a user by email.

     @return User or null if not found
     */

    fun findByEmail(email: String): User? =
        transaction {
            Users
                .selectAll()
                .where {
                    Users.email eq email
                }.singleOrNull()
                ?.let { rowToUser(it) }
        }

    /**
     Finds a user by ID.

     @return User or null if not found
     */

    fun findById(userId: Int): User? =
        transaction {
            Users
                .selectAll()
                .where {
                    Users.userId eq userId
                }.singleOrNull()
                ?.let { rowToUser(it) }
        }

    /**
     * Retrieves all users from the database.
     */

    fun getAllUsers(): List<User> =
        transaction {
            Users.selectAll().map { rowToUser(it) }
        }

    /**
     * Converts a database row into either a User or Manager object.
     */

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
                salt = row[Users.salt],
            )
        } else {
            User(
                userId = row[Users.userId],
                firstName = row[Users.firstName],
                lastName = row[Users.lastName],
                dateOfBirth = row[Users.dateOfBirth],
                email = row[Users.email],
                passwordHash = row[Users.passwordHash],
                salt = row[Users.salt],
            )
        }
    }

    /**
     * Checks whether an account is locked and unlocks it if the lockout time has expired.
     */

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

    /**
     * Creates a default manager account if one does not already exist.
     */

    fun setDefaultManager(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        email: String,
        rawPassword: String,
    ) {
        transaction {
            val existing =
                Users
                    .selectAll()
                    .where {
                        Users.email eq email
                    }.singleOrNull()

            if (existing != null) {
                return@transaction
            }

            val salt = EncryptionService.generateSalt()
            val passwordHash = EncryptionService.hashPassword(rawPassword, salt)

            val inserted =
                Users.insert {
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
                    it[Users.status] = AccountStatus.ACTIVE
                }

            val newManagerId = inserted[Users.userId]
            LoyaltyService().createLoyaltyAccount(newManagerId)
        }
    }
}
