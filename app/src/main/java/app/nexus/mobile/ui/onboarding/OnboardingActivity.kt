package app.nexus.mobile.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import app.nexus.mobile.R
import app.nexus.mobile.ui.main.MainActivity
import app.nexus.mobile.util.PermissionUtils

/**
 * Onboarding obrigatório com disclosure prominente das duas permissões
 * sensíveis. Atende aos requisitos do Google Play para apps que usam
 * `BIND_ACCESSIBILITY_SERVICE` fora do propósito original (acessibilidade
 * para usuários com deficiência).
 *
 * Regras seguidas:
 *  - Disclosure fica ANTES da solicitação da permissão.
 *  - Texto explica claramente: por que precisamos, o que lemos, o que
 *    NÃO lemos, e como o usuário pode revogar.
 *  - O botão de "conceder" só leva às configurações do sistema; nunca
 *    tentamos conceder a permissão programaticamente.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var statusAccessibility: TextView
    private lateinit var statusOverlay: TextView
    private lateinit var btnContinue: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        findViewById<TextView>(R.id.disclosure_accessibility_body).text =
            Html.fromHtml(getString(R.string.disclosure_accessibility_body), Html.FROM_HTML_MODE_COMPACT)
        findViewById<TextView>(R.id.disclosure_overlay_body).text =
            Html.fromHtml(getString(R.string.disclosure_overlay_body), Html.FROM_HTML_MODE_COMPACT)
        findViewById<TextView>(R.id.disclosure_data_body).text =
            Html.fromHtml(getString(R.string.disclosure_data_body), Html.FROM_HTML_MODE_COMPACT)

        statusAccessibility = findViewById(R.id.status_accessibility)
        statusOverlay = findViewById(R.id.status_overlay)
        btnContinue = findViewById(R.id.btn_continue)

        findViewById<Button>(R.id.btn_grant_accessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btn_grant_overlay).setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
        btnContinue.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val accessibilityOk = PermissionUtils.isAccessibilityServiceEnabled(this)
        val overlayOk = PermissionUtils.canDrawOverlays(this)

        statusAccessibility.text = getString(
            if (accessibilityOk) R.string.status_accessibility_granted
            else R.string.status_accessibility_missing
        )
        statusAccessibility.setTextColor(
            getColor(if (accessibilityOk) R.color.nexus_accent else R.color.nexus_warning)
        )

        statusOverlay.text = getString(
            if (overlayOk) R.string.status_overlay_granted
            else R.string.status_overlay_missing
        )
        statusOverlay.setTextColor(
            getColor(if (overlayOk) R.color.nexus_accent else R.color.nexus_warning)
        )

        btnContinue.isEnabled = accessibilityOk && overlayOk
    }
}
