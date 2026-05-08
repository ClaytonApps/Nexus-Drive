package com.nexusdrive.app.data.model

data class RideOffer(
    val sourceApp: String,
    val priceBrl: Double,
    val distanceKm: Double,
    val durationMin: Int,
    val pickupDistanceKm: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    val pricePerKm: Double get() = if (distanceKm > 0) priceBrl / distanceKm else 0.0
    val pricePerMin: Double get() = if (durationMin > 0) priceBrl / durationMin else 0.0
}
