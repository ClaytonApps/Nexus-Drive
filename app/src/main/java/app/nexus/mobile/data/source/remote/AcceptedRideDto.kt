package app.nexus.mobile.data.source.remote

import com.google.gson.annotations.SerializedName

/**
 * DTO enviado ao backend quando o motorista aceita uma corrida.
 * Mantemos apenas dados financeiros e de trajeto — sem PII do passageiro.
 */
data class AcceptedRideDto(
    @SerializedName("driver_id") val driverId: String,
    @SerializedName("source_app") val sourceApp: String,
    @SerializedName("ride_value_brl") val rideValueBrl: Double,
    @SerializedName("total_distance_km") val totalDistanceKm: Double,
    @SerializedName("estimated_duration_min") val estimatedDurationMin: Int,
    @SerializedName("cost_per_km_brl") val costPerKmBrl: Double,
    @SerializedName("net_profit_brl") val netProfitBrl: Double,
    @SerializedName("net_profit_per_hour") val netProfitPerHour: Double,
    @SerializedName("net_profit_per_km") val netProfitPerKm: Double,
    @SerializedName("accepted_at_epoch_ms") val acceptedAtEpochMs: Long
)

data class AcceptedRideResponse(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String
)
