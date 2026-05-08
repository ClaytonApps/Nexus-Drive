package com.nexusdrive.app.data.repository

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.edit
import com.nexusdrive.app.BuildConfig
import com.nexusdrive.app.data.model.License
import com.nexusdrive.app.domain.license.ActivationCode
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Persiste e calcula o estado da licença.
 *
 * Origem do "primeiro uso":
 *  - Usamos `PackageManager.firstInstallTime`, que sobrevive a
 *    "Limpar dados" (reinstalação realmente reseta — limitação
 *    aceita do MVP sem backend).
 *
 * Anti-replay de códigos:
 *  - Cada código já consumido vai para uma blocklist em prefs;
 *    digitar o mesmo código de novo no mesmo aparelho não estende
 *    a licença além do limite contratado pelo motorista.
 */
class LicenseRepository(private val context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val secret: String = BuildConfig.LICENSE_SECRET

    /** Retorna o estado corrente da licença. */
    fun current(now: Long = System.currentTimeMillis()): License {
        val activeUntil = prefs.getLong(KEY_ACTIVE_UNTIL, 0L)
        if (activeUntil > now) return License.Active(activeUntil)

        val trialEnds = trialEndsAt()
        if (trialEnds > now) return License.Trial(trialEnds)

        return License.Expired
    }

    /**
     * Tenta resgatar um código. Retorna mensagem de erro ou `null` em caso de sucesso.
     * Em sucesso, a licença passa a `Active`.
     */
    fun redeem(rawCode: String, now: Long = System.currentTimeMillis()): RedeemResult {
        val verification = ActivationCode.verify(rawCode, secret)
        when (verification) {
            ActivationCode.Verification.MalformedFormat -> return RedeemResult.MalformedFormat
            ActivationCode.Verification.UnknownVersion -> return RedeemResult.UnknownVersion
            ActivationCode.Verification.InvalidSignature -> return RedeemResult.InvalidSignature
            is ActivationCode.Verification.Valid -> {
                val codeHash = sha256(rawCode.uppercase().trim())
                val used = prefs.getStringSet(KEY_USED_CODES, emptySet()) ?: emptySet()
                if (codeHash in used) return RedeemResult.AlreadyUsed

                // Estende a partir do MAIOR entre agora e a expiração atual,
                // pra que renovações antecipadas não percam dias.
                val current = prefs.getLong(KEY_ACTIVE_UNTIL, 0L)
                val base = if (current > now) current else now
                val newExpiry = base + TimeUnit.DAYS.toMillis(verification.validForDays.toLong())

                prefs.edit {
                    putLong(KEY_ACTIVE_UNTIL, newExpiry)
                    putStringSet(KEY_USED_CODES, used + codeHash)
                }
                return RedeemResult.Success(verification.validForDays, newExpiry)
            }
        }
    }

    private fun trialEndsAt(): Long {
        val cached = prefs.getLong(KEY_FIRST_LAUNCH, 0L)
        if (cached > 0L) return cached + TRIAL_MILLIS

        val installTime = runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .firstInstallTime
        }.getOrDefault(System.currentTimeMillis())

        prefs.edit { putLong(KEY_FIRST_LAUNCH, installTime) }
        return installTime + TRIAL_MILLIS
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    sealed class RedeemResult {
        data class Success(val daysAdded: Int, val newExpiryEpochMs: Long) : RedeemResult()
        data object MalformedFormat : RedeemResult()
        data object UnknownVersion : RedeemResult()
        data object InvalidSignature : RedeemResult()
        data object AlreadyUsed : RedeemResult()
    }

    companion object {
        private const val PREFS_NAME = "nexus_license"
        private const val KEY_FIRST_LAUNCH = "first_launch_epoch"
        private const val KEY_ACTIVE_UNTIL = "active_until_epoch"
        private const val KEY_USED_CODES = "used_codes"
        private val TRIAL_MILLIS = TimeUnit.DAYS.toMillis(2)
    }
}
