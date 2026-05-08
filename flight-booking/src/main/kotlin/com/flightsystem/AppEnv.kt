package com.flightsystem

import io.github.cdimascio.dotenv.dotenv

object AppEnv {
    private val dotenv =
        dotenv {
            ignoreIfMissing = true
        }

    fun require(key: String): String = dotenv[key] ?: System.getenv(key) ?: error("$key is not defined")
}
