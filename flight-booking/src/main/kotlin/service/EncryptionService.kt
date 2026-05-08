package com.flightsystem.flightservice

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
Provides utilities for password hashing and verification.

Uses SHA-256 with a random salt for security.
 */

object EncryptionService {
    private const val HASH_ALGORITHM = "SHA-256"
    private const val SALT_LENGTH = 16

    /**
     Generates a random salt for password hashing.

     @return Base64 encoded salt
     */

    fun generateSalt(): String {
        val random = SecureRandom()

        // Creating a byte array for the salt
        val saltBytes = ByteArray(SALT_LENGTH)

        // Filling the array with random values
        random.nextBytes(saltBytes)

        // Convert salt to a Base64 string so we can store it
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    /**
     Hashes a password combined with a salt.

     @return SHA-256 hash as a hexadecimal string
     */

    fun hashPassword(
        password: String,
        salt: String,
    ): String {
        // Get SHA-256 hashing instance
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)

        // Combine password with salt
        val saltedPassword = password + salt

        // Perform hashing
        val hashBytes = digest.digest(saltedPassword.toByteArray())

        // Convert byte array to hexadecimal string which is our final encrypted password
        return hashBytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    /**
     Verifies a password by comparing the computed hash with the stored hash.
     */

    fun verifyPassword(
        inputPassword: String,
        storedHash: String,
        salt: String,
    ): Boolean {
        val inputHash = hashPassword(inputPassword, salt)
        // Comparing the hashes
        return inputHash == storedHash
    }
}
