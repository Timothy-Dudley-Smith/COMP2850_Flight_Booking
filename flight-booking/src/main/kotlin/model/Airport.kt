package com.flightsystem.model

import org.jetbrains.exposed.v1.core.Table

data class Airport(
    val code: String,
    val name: String,
    val city: String,
    val country: String
)

object Airports: Table() {

}