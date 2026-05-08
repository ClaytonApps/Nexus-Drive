package com.nexusdrive.app.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.nexusdrive.app.BuildConfig
import com.nexusdrive.app.R
import com.nexusdrive.app.domain.license.ActivationCode
import java.util.Date

/**
 * Área administrativa — gera códigos de 30 dias para venda.
 *
 * Os códigos gerados ficam em prefs locais para o admin recopiar
 * caso precise reenviar para o cliente. Como o código é
 * autocontido (HMAC), ele é válido em qualquer instalação do app.
 */
class AdminActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var generatedView: TextView
    private lateinit var historyView: TextView
    private var lastGenerated: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        generatedView = findViewById(R.id.generated_code)
        historyView = findViewById(R.id.history_list)

        findViewById<Button>(R.id.btn_generate).setOnClickListener {
            val code = ActivationCode.generate(
                validForDays = 30,
                secret = BuildConfig.LICENSE_SECRET
            )
            lastGenerated = code
            generatedView.text = code
            appendHistory(code)
        }

        findViewById<Button>(R.id.btn_copy).setOnClickListener {
            val code = lastGenerated ?: return@setOnClickListener
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Nexus Drive", code))
            Toast.makeText(this, R.string.admin_copied, Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_share).setOnClickListener {
            val code = lastGenerated ?: return@setOnClickListener
            val message = getString(R.string.admin_share_template, code)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.admin_share)))
        }

        renderHistory()
    }

    private fun appendHistory(code: String) {
        val timestamp = System.currentTimeMillis()
        val current = prefs.getStringSet(KEY_HISTORY, emptySet()) ?: emptySet()
        val entry = "$timestamp|$code"
        prefs.edit { putStringSet(KEY_HISTORY, current + entry) }
        renderHistory()
    }

    private fun renderHistory() {
        val entries = (prefs.getStringSet(KEY_HISTORY, emptySet()) ?: emptySet())
            .mapNotNull {
                val parts = it.split("|", limit = 2)
                if (parts.size != 2) null
                else parts[0].toLongOrNull()?.let { ts -> ts to parts[1] }
            }
            .sortedByDescending { it.first }
            .take(20)

        historyView.text = if (entries.isEmpty()) {
            getString(R.string.admin_history_empty)
        } else {
            entries.joinToString("\n") { (ts, code) ->
                val when_ = DateFormat.getDateFormat(this).format(Date(ts)) +
                    " " + DateFormat.getTimeFormat(this).format(Date(ts))
                "$when_  $code"
            }
        }
    }

    companion object {
        private const val PREFS = "nexus_admin"
        private const val KEY_HISTORY = "history"
    }
}
