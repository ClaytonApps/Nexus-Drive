package app.nexus.mobile.ui.debug

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.nexus.mobile.R
import app.nexus.mobile.util.CapturedTextBus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Tela de diagnóstico em tempo real.
 *
 * Mostra:
 *  - Pacote em primeiro plano detectado.
 *  - Valores extraídos pela regex (R$, km, min).
 *  - Lista bruta dos textos lidos da árvore — é com isso que você
 *    ajusta a regex `PRICE_PATTERN`/`DISTANCE_PATTERN`/`DURATION_PATTERN`
 *    e as `ACCEPT_KEYWORDS` no [app.nexus.mobile.service.DriverMonitorService].
 *
 * Como usar:
 *  1. Abra esta tela.
 *  2. Em outro app aberto (Uber/99), provoque um evento (abrir oferta).
 *  3. Volte aqui — o último snapshot estará na tela.
 */
class DebugActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        val summary = findViewById<TextView>(R.id.debug_summary)
        val raw = findViewById<TextView>(R.id.debug_raw)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CapturedTextBus.flow.collectLatest { snap ->
                    summary.text = buildString {
                        appendLine("pkg: ${snap.packageName}")
                        appendLine("R$: ${snap.parsedPriceBrl ?: "-"}")
                        appendLine("km: ${snap.parsedDistanceKm ?: "-"}")
                        appendLine("min: ${snap.parsedDurationMin ?: "-"}")
                    }
                    raw.text = snap.rawTexts.joinToString("\n") { "• $it" }
                }
            }
        }
    }
}
