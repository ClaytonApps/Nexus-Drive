package com.nexusdrive.app.data.repository

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nexusdrive.app.BuildConfig
import com.nexusdrive.app.data.model.License
import com.nexusdrive.app.domain.license.ActivationCode
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Estado de acesso do app, vinculado à CONTA do motorista.
 *
 * Modelo de negócio:
 *  - Teste grátis de 2 dias, contado a partir do primeiro login da conta.
 *  - Depois disso o app bloqueia até resgatar um código de ativação.
 *  - Cada código adiciona 30 dias (mensalidade de R$ 7,80).
 *
 * Fonte da verdade x cache:
 *  - O Firestore (coleção `entitlements`, doc = UID) guarda o início do
 *    teste, a validade paga e os códigos já usados — assim reinstalar o
 *    app ou limpar dados não reseta nada.
 *  - As SharedPreferences locais espelham esses valores para que
 *    [current] seja SÍNCRONO: o serviço de acessibilidade consulta a
 *    licença a cada evento e o app precisa funcionar offline.
 *  - [sync] reconcilia o cache com o Firestore; chame ao abrir o app,
 *    ao abrir o paywall e ao conectar o serviço de acessibilidade.
 *
 * Sem Firebase configurado (ex.: build de CI sem `google-services.json`),
 * cai no modo legado: o teste é contado a partir da data de instalação
 * no aparelho e os códigos valem apenas localmente.
 */
class LicenseRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secret: String = BuildConfig.LICENSE_SECRET

    private val firestore: FirebaseFirestore? by lazy {
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    /** Estado corrente da licença — leitura síncrona do cache local. */
    fun current(now: Long = System.currentTimeMillis()): License {
        val paidUntil = prefs.getLong(KEY_ACTIVE_UNTIL, 0L)
        if (paidUntil > now) return License.Active(paidUntil)

        val trialStart = prefs.getLong(KEY_TRIAL_START, 0L)
        if (trialStart > 0L) {
            val trialEnds = trialStart + TRIAL_MILLIS
            if (trialEnds > now) return License.Trial(trialEnds)
        }
        return License.Expired
    }

    /**
     * Reconcilia o cache local com o Firestore para a conta [uid].
     * Se [uid] for nulo (sem login/Firebase), usa o modo legado por
     * data de instalação. Seguro de chamar offline: nesse caso mantém
     * o que já estiver em cache.
     */
    suspend fun sync(uid: String?) {
        if (uid == null) {
            ensureLegacyTrialStart()
            return
        }
        val db = firestore ?: run {
            ensureLegacyTrialStart()
            return
        }
        val docRef = db.collection(COLLECTION).document(uid)

        val snapshot = runCatching { docRef.get().await() }.getOrNull()
        if (snapshot == null) {
            // Offline ou erro de rede — mantém o cache e garante um teste local.
            ensureLegacyTrialStart()
            return
        }

        if (!snapshot.exists()) {
            // Primeiro login desta conta: começa o teste de 2 dias agora.
            val now = System.currentTimeMillis()
            docRef.set(
                mapOf(
                    FIELD_TRIAL_START to now,
                    FIELD_PAID_UNTIL to 0L,
                    FIELD_USED_CODES to emptyList<String>()
                )
            ) // sem await: o SDK persiste localmente e sincroniza sozinho
            writeCache(now, 0L, emptySet())
        } else {
            val trialStart = snapshot.getLong(FIELD_TRIAL_START) ?: System.currentTimeMillis()
            val paidUntil = snapshot.getLong(FIELD_PAID_UNTIL) ?: 0L
            val usedCodes = (snapshot.get(FIELD_USED_CODES) as? List<*>)
                ?.filterIsInstance<String>()?.toSet() ?: emptySet()
            writeCache(trialStart, paidUntil, usedCodes)
        }
    }

    /**
     * Resgata um código de ativação. Verifica o HMAC localmente, grava
     * o novo vencimento no cache (o acesso é liberado na hora) e replica
     * para o Firestore da conta [uid], se houver.
     */
    fun redeem(
        rawCode: String,
        uid: String?,
        now: Long = System.currentTimeMillis()
    ): RedeemResult {
        return when (val verification = ActivationCode.verify(rawCode, secret)) {
            ActivationCode.Verification.MalformedFormat -> RedeemResult.MalformedFormat
            ActivationCode.Verification.UnknownVersion -> RedeemResult.UnknownVersion
            ActivationCode.Verification.InvalidSignature -> RedeemResult.InvalidSignature
            is ActivationCode.Verification.Valid -> {
                val codeHash = sha256(rawCode.uppercase().trim())
                val used = prefs.getStringSet(KEY_USED_CODES, emptySet()) ?: emptySet()
                if (codeHash in used) return RedeemResult.AlreadyUsed

                // Renovação antecipada não perde dias: estende a partir do
                // maior entre agora e a expiração atual.
                val currentPaid = prefs.getLong(KEY_ACTIVE_UNTIL, 0L)
                val base = if (currentPaid > now) currentPaid else now
                val newExpiry = base + TimeUnit.DAYS.toMillis(verification.validForDays.toLong())

                prefs.edit {
                    putLong(KEY_ACTIVE_UNTIL, newExpiry)
                    putStringSet(KEY_USED_CODES, used + codeHash)
                }
                if (uid != null) {
                    firestore?.collection(COLLECTION)?.document(uid)?.set(
                        mapOf(
                            FIELD_PAID_UNTIL to newExpiry,
                            FIELD_USED_CODES to FieldValue.arrayUnion(codeHash)
                        ),
                        SetOptions.merge()
                    )
                }
                RedeemResult.Success(verification.validForDays, newExpiry)
            }
        }
    }

    private fun writeCache(trialStart: Long, paidUntil: Long, usedCodes: Set<String>) {
        prefs.edit {
            putLong(KEY_TRIAL_START, trialStart)
            putLong(KEY_ACTIVE_UNTIL, paidUntil)
            putStringSet(KEY_USED_CODES, usedCodes)
        }
    }

    /** Modo legado/offline: teste contado a partir da instalação no aparelho. */
    private fun ensureLegacyTrialStart() {
        if (prefs.getLong(KEY_TRIAL_START, 0L) > 0L) return
        val installTime = runCatching {
            appContext.packageManager
                .getPackageInfo(appContext.packageName, 0)
                .firstInstallTime
        }.getOrDefault(System.currentTimeMillis())
        prefs.edit { putLong(KEY_TRIAL_START, installTime) }
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
        private const val KEY_TRIAL_START = "trial_start_epoch"
        private const val KEY_ACTIVE_UNTIL = "active_until_epoch"
        private const val KEY_USED_CODES = "used_codes"

        private const val COLLECTION = "entitlements"
        private const val FIELD_TRIAL_START = "trialStartedAt"
        private const val FIELD_PAID_UNTIL = "paidUntil"
        private const val FIELD_USED_CODES = "usedCodes"

        private val TRIAL_MILLIS = TimeUnit.DAYS.toMillis(2)
    }
}
