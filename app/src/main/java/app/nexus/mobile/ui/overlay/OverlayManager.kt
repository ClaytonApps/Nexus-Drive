package app.nexus.mobile.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import app.nexus.mobile.R
import app.nexus.mobile.data.model.ProfitEstimate

/**
 * Gerencia o ciclo de vida da janela flutuante (overlay) do Nexus.
 *
 * Usa o [WindowManager] do sistema para adicionar uma view do tipo
 * `TYPE_APPLICATION_OVERLAY`, que pode ser desenhada por cima de
 * qualquer outro app — exatamente o que precisamos para mostrar o
 * "semáforo" durante uma corrida no Uber/99.
 *
 * Estado visual:
 *  - [Verdict.GOOD]      -> fundo verde    (lucro alto, aceitar)
 *  - [Verdict.BAD]       -> fundo vermelho (lucro baixo, recusar)
 *  - [Verdict.MEDIUM]    -> fundo amarelo  (lucro intermediário, decisão do motorista)
 *  - [Verdict.ANALYZING] -> fundo amarelo  (ainda calculando)
 *
 * IMPORTANTE: a permissão `SYSTEM_ALERT_WINDOW` precisa ter sido
 * concedida pelo usuário em "Configurações → Apps especiais →
 * Sobreposição". Use [canDrawOverlays] para verificar antes de chamar
 * [show].
 */
class OverlayManager(private val context: Context) {

    enum class Verdict { GOOD, BAD, MEDIUM, ANALYZING }

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private var rootContainer: LinearLayout? = null
    private var labelView: TextView? = null
    private var valueView: TextView? = null

    /** True enquanto a view estiver anexada ao WindowManager. */
    val isShowing: Boolean get() = overlayView != null

    /**
     * Anexa o overlay à tela, se ainda não estiver visível e se a
     * permissão de sobreposição estiver concedida.
     */
    fun show(initialVerdict: Verdict = Verdict.ANALYZING, valueText: String = "Analisando...") {
        if (isShowing) {
            update(initialVerdict, valueText)
            return
        }
        if (!canDrawOverlays(context)) return

        val view = LayoutInflater.from(context)
            .inflate(R.layout.overlay_view, null, false)

        rootContainer = view.findViewById(R.id.overlay_root)
        labelView = view.findViewById(R.id.overlay_label)
        valueView = view.findViewById(R.id.overlay_value)

        val params = buildLayoutParams()
        attachDragBehavior(view, params)

        windowManager.addView(view, params)
        overlayView = view

        update(initialVerdict, valueText)
    }

    /**
     * Atualiza cor de fundo e textos sem recriar a view.
     * Pode ser chamado livremente; se o overlay não estiver visível,
     * a chamada é ignorada.
     */
    fun update(verdict: Verdict, valueText: String? = null) {
        val container = rootContainer ?: return
        container.setBackgroundResource(
            when (verdict) {
                Verdict.GOOD -> R.drawable.bg_overlay_green
                Verdict.BAD -> R.drawable.bg_overlay_red
                Verdict.MEDIUM -> R.drawable.bg_overlay_yellow
                Verdict.ANALYZING -> R.drawable.bg_overlay_yellow
            }
        )
        labelView?.text = when (verdict) {
            Verdict.GOOD -> "BOA"
            Verdict.BAD -> "RUIM"
            Verdict.MEDIUM -> "MÉDIO"
            Verdict.ANALYZING -> "ANALISANDO"
        }
        valueText?.let { valueView?.text = it }
    }

    /** Remove o overlay da tela, se estiver visível. */
    fun hide() {
        val view = overlayView ?: return
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // View já não está anexada — ignorar com segurança.
        }
        overlayView = null
        rootContainer = null
        labelView = null
        valueView = null
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        // A partir do Android 8 (Oreo) é obrigatório usar
        // TYPE_APPLICATION_OVERLAY. Em versões anteriores, o tipo
        // legado TYPE_PHONE ainda é aceito.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            // FLAG_NOT_FOCUSABLE: o overlay não rouba foco do app de baixo.
            // FLAG_NOT_TOUCH_MODAL: toques fora da view passam pro app.
            // FLAG_LAYOUT_NO_LIMITS: permite posicionar nas bordas.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 240
        }
    }

    /**
     * Permite arrastar o balão pela tela com o dedo.
     * Implementação mínima — sem snapping nas bordas.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragBehavior(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    if (overlayView != null) {
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Atualiza o overlay com base em um [ProfitEstimate] já calculado.
     * Decide o verdict (verde/amarelo/vermelho) por R$/hora líquido —
     * a métrica mais relevante para o motorista.
     */
    fun updateFromProfit(estimate: ProfitEstimate) {
        val perHour = estimate.netProfitPerHour
        val perKm = estimate.netProfitPerKm
        val verdict = when {
            perHour >= GOOD_HOURLY_THRESHOLD_BRL -> Verdict.GOOD
            perHour <= BAD_HOURLY_THRESHOLD_BRL -> Verdict.BAD
            else -> Verdict.MEDIUM
        }
        // Linha 1 mantém a sigla; usamos o "valor" para mostrar o
        // dado mais acionável: lucro líquido por hora estimado.
        val display = "R$ %.0f/h • %.2f/km".format(perHour, perKm)
        update(verdict, display)
    }

    companion object {
        /** Acima desse R$/hora líquido, marcamos como BOA (verde). */
        private const val GOOD_HOURLY_THRESHOLD_BRL = 35.0
        /** Abaixo desse R$/hora líquido, marcamos como RUIM (vermelho). */
        private const val BAD_HOURLY_THRESHOLD_BRL = 20.0

        /** Verifica se o usuário concedeu a permissão de sobreposição. */
        fun canDrawOverlays(context: Context): Boolean =
            Settings.canDrawOverlays(context)
    }
}
