package com.nexusdrive.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.nexusdrive.app.R
import com.nexusdrive.app.data.repository.AuthRepository
import com.nexusdrive.app.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * Tela de login do motorista (Firebase Authentication).
 *
 * Alterna entre dois modos no mesmo layout: ENTRAR (login) e CRIAR CONTA
 * (cadastro). Só é exibida quando o Firebase está configurado e ninguém
 * está logado — quem decide isso é a [MainActivity].
 */
class LoginActivity : AppCompatActivity() {

    private enum class Mode { SIGN_IN, SIGN_UP }

    private val authRepo = AuthRepository()

    private lateinit var subtitle: TextView
    private lateinit var emailField: TextInputEditText
    private lateinit var passwordField: TextInputEditText
    private lateinit var primaryButton: MaterialButton
    private lateinit var toggleButton: MaterialButton
    private lateinit var forgotButton: MaterialButton
    private lateinit var progress: ProgressBar

    private var mode = Mode.SIGN_IN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Defensivo: sem Firebase não há o que autenticar — segue para o app.
        if (!authRepo.isAvailable || authRepo.isSignedIn) {
            goToMain()
            return
        }

        subtitle = findViewById(R.id.login_subtitle)
        emailField = findViewById(R.id.edit_email)
        passwordField = findViewById(R.id.edit_password)
        primaryButton = findViewById(R.id.btn_primary)
        toggleButton = findViewById(R.id.btn_toggle_mode)
        forgotButton = findViewById(R.id.btn_forgot)
        progress = findViewById(R.id.login_progress)

        primaryButton.setOnClickListener { submit() }
        toggleButton.setOnClickListener {
            mode = if (mode == Mode.SIGN_IN) Mode.SIGN_UP else Mode.SIGN_IN
            renderMode()
        }
        forgotButton.setOnClickListener { resetPassword() }

        renderMode()
    }

    private fun renderMode() {
        when (mode) {
            Mode.SIGN_IN -> {
                subtitle.setText(R.string.login_subtitle_login)
                primaryButton.setText(R.string.login_btn_signin)
                toggleButton.setText(R.string.login_toggle_to_register)
            }
            Mode.SIGN_UP -> {
                subtitle.setText(R.string.login_subtitle_register)
                primaryButton.setText(R.string.login_btn_signup)
                toggleButton.setText(R.string.login_toggle_to_login)
            }
        }
        forgotButton.visibility = if (mode == Mode.SIGN_IN) View.VISIBLE else View.GONE
    }

    private fun submit() {
        val email = emailField.text?.toString().orEmpty().trim()
        val password = passwordField.text?.toString().orEmpty()
        if (!isEmailValid(email)) {
            toast(getString(R.string.login_error_email))
            return
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            toast(getString(R.string.login_error_password))
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = when (mode) {
                Mode.SIGN_IN -> authRepo.signIn(email, password)
                Mode.SIGN_UP -> authRepo.signUp(email, password)
            }
            setLoading(false)
            when (result) {
                AuthRepository.AuthResult.Success -> goToMain()
                AuthRepository.AuthResult.Unavailable ->
                    toast(getString(R.string.login_unavailable))
                is AuthRepository.AuthResult.Error -> toast(result.message)
            }
        }
    }

    private fun resetPassword() {
        val email = emailField.text?.toString().orEmpty().trim()
        if (!isEmailValid(email)) {
            toast(getString(R.string.login_error_email))
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            val result = authRepo.sendPasswordReset(email)
            setLoading(false)
            when (result) {
                AuthRepository.AuthResult.Success ->
                    toast(getString(R.string.login_reset_sent, email))
                AuthRepository.AuthResult.Unavailable ->
                    toast(getString(R.string.login_unavailable))
                is AuthRepository.AuthResult.Error -> toast(result.message)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        primaryButton.isEnabled = !loading
        toggleButton.isEnabled = !loading
        forgotButton.isEnabled = !loading
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun isEmailValid(email: String): Boolean =
        email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
