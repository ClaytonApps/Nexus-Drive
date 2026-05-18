package app.nexus.mobile.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Autenticação de motoristas via Firebase Authentication (e-mail/senha).
 *
 * O login serve para **identificar o motorista** — o UID da conta passa
 * a ser o `driverId` enviado com o histórico de corridas. Ele não é o
 * controle de acesso do app: quem libera o uso continua sendo a licença
 * ([LicenseRepository]).
 *
 * Tolerância a "Firebase não configurado":
 *  - O `firebase-auth` só inicializa se o app foi compilado com um
 *    `google-services.json` válido. Sem ele, [FirebaseAuth.getInstance]
 *    lança exceção. Capturamos isso e expomos [isAvailable] = false —
 *    o app então roda normalmente, apenas sem a etapa de login.
 */
class AuthRepository {

    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.w(TAG, "Firebase não configurado neste build — login desativado", e)
        null
    }

    /** True se este build tem Firebase configurado (google-services.json presente). */
    val isAvailable: Boolean get() = auth != null

    val currentUser: FirebaseUser? get() = auth?.currentUser

    val isSignedIn: Boolean get() = currentUser != null

    /** UID da conta logada, ou `null` se não houver login disponível/feito. */
    val driverUid: String? get() = currentUser?.uid

    suspend fun signIn(email: String, password: String): AuthResult {
        val a = auth ?: return AuthResult.Unavailable
        return runCatching { a.signInWithEmailAndPassword(email.trim(), password).await() }
            .fold(
                onSuccess = { AuthResult.Success },
                onFailure = { AuthResult.Error(it.message ?: "Falha ao entrar") }
            )
    }

    suspend fun signUp(email: String, password: String): AuthResult {
        val a = auth ?: return AuthResult.Unavailable
        return runCatching { a.createUserWithEmailAndPassword(email.trim(), password).await() }
            .fold(
                onSuccess = { AuthResult.Success },
                onFailure = { AuthResult.Error(it.message ?: "Falha ao criar conta") }
            )
    }

    suspend fun sendPasswordReset(email: String): AuthResult {
        val a = auth ?: return AuthResult.Unavailable
        return runCatching { a.sendPasswordResetEmail(email.trim()).await() }
            .fold(
                onSuccess = { AuthResult.Success },
                onFailure = { AuthResult.Error(it.message ?: "Falha ao enviar e-mail") }
            )
    }

    fun signOut() {
        auth?.signOut()
    }

    sealed class AuthResult {
        data object Success : AuthResult()
        /** Firebase não foi configurado neste build. */
        data object Unavailable : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
