package com.nexusdrive.app.data.model

/** Estado atual da licença do app. */
sealed class License {
    /** Período de teste gratuito (2 dias a partir do primeiro login). */
    data class Trial(val expiresAtEpochMs: Long) : License()

    /** Acesso pago, somando códigos de ativação de 30 dias. */
    data class Active(val expiresAtEpochMs: Long) : License()

    /** Trial e acesso pago expirados — app bloqueado até inserir código. */
    data object Expired : License()

    fun remainingMillis(now: Long = System.currentTimeMillis()): Long = when (this) {
        is Trial -> (expiresAtEpochMs - now).coerceAtLeast(0L)
        is Active -> (expiresAtEpochMs - now).coerceAtLeast(0L)
        Expired -> 0L
    }

    fun isUsable(now: Long = System.currentTimeMillis()): Boolean = when (this) {
        is Trial -> now < expiresAtEpochMs
        is Active -> now < expiresAtEpochMs
        Expired -> false
    }
}
