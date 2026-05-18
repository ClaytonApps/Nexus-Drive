package app.nexus.mobile.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.textfield.TextInputEditText
import app.nexus.mobile.R
import java.security.MessageDigest

/**
 * Gate de PIN para a área administrativa.
 *
 * - Primeiro acesso: usuário define o PIN (mínimo 4 dígitos).
 *   O hash SHA-256 é guardado em SharedPreferences.
 * - Acessos seguintes: usuário digita o PIN; só passa se o hash bater.
 *
 * Usar PIN simples é suficiente porque a área admin não tem dados
 * de outros usuários — só gera códigos. O segredo de assinatura
 * (`LICENSE_SECRET`) é a chave criptográfica de verdade.
 */
class AdminPinActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_pin)

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        val isFirstSetup = storedHash == null

        findViewById<TextView>(R.id.pin_subtitle).text = getString(
            if (isFirstSetup) R.string.admin_pin_setup else R.string.admin_pin_enter
        )

        val pinField = findViewById<TextInputEditText>(R.id.edit_pin)
        findViewById<Button>(R.id.btn_submit).setOnClickListener {
            val pin = pinField.text?.toString().orEmpty()
            if (pin.length < 4) {
                Toast.makeText(this, R.string.admin_pin_too_short, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val hash = sha256(pin)
            if (isFirstSetup) {
                prefs.edit { putString(KEY_PIN_HASH, hash) }
                openAdmin()
            } else if (hash == storedHash) {
                openAdmin()
            } else {
                Toast.makeText(this, R.string.admin_pin_wrong, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAdmin() {
        startActivity(Intent(this, AdminActivity::class.java))
        finish()
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val PREFS = "nexus_admin"
        private const val KEY_PIN_HASH = "pin_hash"
    }
}
