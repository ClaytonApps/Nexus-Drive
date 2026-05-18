package app.nexus.mobile.data.repository

import android.content.Context
import androidx.core.content.edit

/**
 * Persiste preferências do motorista — hoje apenas o custo por km.
 * Usa SharedPreferences direto para manter zero dependências; pode
 * ser migrado para DataStore quando necessário.
 */
class DriverSettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Custo operacional por km (R$). Default conservador: R$ 0,80/km. */
    var costPerKmBrl: Double
        get() = prefs.getFloat(KEY_COST_PER_KM, DEFAULT_COST_PER_KM.toFloat()).toDouble()
        set(value) = prefs.edit { putFloat(KEY_COST_PER_KM, value.toFloat()) }

    /** Se true, corridas aceitas são enviadas ao backend. Opt-in. */
    var sendDataEnabled: Boolean
        get() = prefs.getBoolean(KEY_SEND_DATA, false)
        set(value) = prefs.edit { putBoolean(KEY_SEND_DATA, value) }

    /** Identificador anônimo do motorista, gerado uma vez localmente. */
    val driverId: String
        get() {
            val existing = prefs.getString(KEY_DRIVER_ID, null)
            if (existing != null) return existing
            val fresh = java.util.UUID.randomUUID().toString()
            prefs.edit { putString(KEY_DRIVER_ID, fresh) }
            return fresh
        }

    companion object {
        private const val PREFS_NAME = "nexus_drive_settings"
        private const val KEY_COST_PER_KM = "cost_per_km_brl"
        private const val KEY_SEND_DATA = "send_data_enabled"
        private const val KEY_DRIVER_ID = "driver_id"
        private const val DEFAULT_COST_PER_KM = 0.80
    }
}
