package com.flightsystem.service


import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

// Will be used for securely handling user passwords

// First method will generate a random salt
// Second method will hash passwords using SHA-256
// Third method will verify passwords during login


object EncryptionService {
    private const val HASH_ALGORITHM = "SHA-256"
    private const val SALT_LENGTH = 16



    fun generateSalt(): String {
        val random = SecureRandom()

        // Creating a byte array for the salt
        val saltBytes = ByteArray(SALT_LENGTH)


        // Filling the array with random values
        random.nextBytes(saltBytes)


        // Convert salt to a Base64 string so we can store it
        return Base64.getEncoder().encodeToString(saltBytes)

    }

    fun hashPassword(password: String, salt: String): String {

        // Get SHA-256 hashing instance
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)

        // Combine password with salt
        val saltedPassword = password + salt


        // Perform hashing
        val hashBytes = digest.digest(saltedPassword.toByteArray())

        // Convert byte array to hexadecimal string which is our final encrypted password
        return hashBytes.joinToString("") { byte -> "%02x".format(byte)}


    }

    fun verifyPassword(inputPassword: String, storedHash: String, salt: String): Boolean {

        val inputHash = hashPassword(inputPassword, salt)
        // Comparing the hashes
        return inputHash == storedHash
    }



}