package com.nexusdrive.app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.nexusdrive.app.data.model.ProfitEstimate
import com.nexusdrive.app.data.repository.AcceptedRideRepository
import com.nexusdrive.app.data.repository.AuthRepository
import com.nexusdrive.app.data.repository.DriverSettingsRepository
import com.nexusdrive.app.data.repository.LicenseRepository
import com.nexusdrive.app.domain.usecase.CalculateProfitUseCase
import com.nexusdrive.app.ui.overlay.OverlayManager
import com.nexusdrive.app.util.CapturedTextBus
import com.nexusdrive.app.util.PackageDiscovery
import java.util.regex.Pattern

/**
 * Serviço de acessibilidade do Nexus Drive — orquestra captura,
 * cálculo de lucro, overlay e envio durável.
 *
 * Filtros de pacote (camadas):
 *  1) DECLARATIVO em `res/xml/accessibility_service_config.xml`
 *     (atributo `android:packageNames`).
 *  2) IMPERATIVO via [PackageDiscovery.supportedPackages] — segunda
 *     barreira que aceita todas as variantes conhecidas dos apps.
 */
class DriverMonitorService : AccessibilityService() {

    companion object {
        private const val TAG = "DriverMonitor"

        private val PRICE_PATTERN: Pattern = Pattern.compile(
            "R\\$\\s?([0-9]{1,3}(?:[.\\s][0-9]{3})*(?:[,.][0-9]{1,2})?)"
        )
        private val DISTANCE_PATTERN: Pattern = Pattern.compile(
            "([0-9]+(?:[.,][0-9]+)?)\\s?(km|m)\\b",
            Pattern.CASE_INSENSITIVE
        )
        private val DURATION_PATTERN: Pattern = Pattern.compile(
            "(?:([0-9]+)\\s?h(?:oras?)?\\s?)?([0-9]+)\\s?min",
            Pattern.CASE_INSENSITIVE
        )

        // Heurística: textos que indicam que o motorista aceitou.
        // Ajuste pela DebugActivity quando descobrir o vocabulário real.
        private val ACCEPT_KEYWORDS = listOf(
            "indo até", "a caminho", "rota até", "navegar",
            "iniciar viagem", "viagem iniciada", "indo buscar",
            "buscando passageiro", "trajeto até"
        )
    }

    private var overlayManager: OverlayManager? = null
    private lateinit var settings: DriverSettingsRepository
    private lateinit var ridesRepo: AcceptedRideRepository
    private lateinit var licenseRepo: LicenseRepository
    private val authRepo = AuthRepository()
    private val calculateProfit = CalculateProfitUseCase()

    private val supportedPackages: Set<String> by lazy { PackageDiscovery.supportedPackages() }

    private var foregroundDriverApp: String? = null
    private var lastEstimate: ProfitEstimate? = null
    private var lastSubmittedAt: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayManager(this)
        settings = DriverSettingsRepository(this)
        ridesRepo = AcceptedRideRepository(applicationContext)
        licenseRepo = LicenseRepository(applicationContext)
        Log.i(TAG, "DriverMonitorService conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Licença expirada: nenhum overlay, nenhum cálculo, nenhum envio.
        if (!licenseRepo.current().isUsable()) {
            overlayManager?.hide()
            return
        }

        val packageName = event.packageName?.toString()

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handleForegroundChange(packageName)
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return
        if (packageName == null || packageName !in supportedPackages) return

        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return
        try {
            analyzeOffer(root, packageName)
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private fun handleForegroundChange(packageName: String?) {
        if (packageName == null) return
        if (packageName.startsWith("com.android.systemui") ||
            packageName.startsWith("android")
        ) return

        if (packageName in supportedPackages) {
            if (foregroundDriverApp != packageName) {
                foregroundDriverApp = packageName
                Log.d(TAG, "App de motorista em primeiro plano: $packageName")
                overlayManager?.show(OverlayManager.Verdict.ANALYZING, "Analisando...")
            }
        } else if (foregroundDriverApp != null) {
            Log.d(TAG, "Saindo do app de motorista (atual: $packageName)")
            foregroundDriverApp = null
            overlayManager?.hide()
            lastEstimate = null
        }
    }

    private fun analyzeOffer(root: AccessibilityNodeInfo, packageName: String) {
        val texts = mutableListOf<String>()
        collectTexts(root, texts)
        val joined = texts.joinToString(" | ")

        if (acceptedByDriver(joined)) {
            maybeSubmitAcceptedRide(packageName)
        }

        val price = extractPrice(joined)
        val distanceKm = extractDistanceKm(joined)
        val durationMin = extractDurationMin(joined)

        // Sempre publica para a Debug screen — facilita ajuste de regex.
        CapturedTextBus.emit(
            CapturedTextBus.Snapshot(
                packageName = packageName,
                rawTexts = texts,
                parsedPriceBrl = price,
                parsedDistanceKm = distanceKm,
                parsedDurationMin = durationMin
            )
        )

        if (price == null) return

        if (distanceKm == null || durationMin == null) {
            overlayManager?.update(
                OverlayManager.Verdict.ANALYZING,
                "R$ %.2f".format(price)
            )
            return
        }

        val estimate = calculateProfit(
            rideValueBrl = price,
            totalDistanceKm = distanceKm,
            costPerKmBrl = settings.costPerKmBrl,
            estimatedDurationMin = durationMin
        )
        lastEstimate = estimate

        Log.d(
            TAG,
            "[$packageName] R$ %.2f • %.1f km • %d min => líquido R$ %.2f (R$ %.2f/h)"
                .format(price, distanceKm, durationMin, estimate.netProfitBrl, estimate.netProfitPerHour)
        )

        overlayManager?.updateFromProfit(estimate)
    }

    private fun acceptedByDriver(text: String): Boolean {
        val lower = text.lowercase()
        return ACCEPT_KEYWORDS.any { lower.contains(it) }
    }

    private fun maybeSubmitAcceptedRide(packageName: String) {
        if (!settings.sendDataEnabled) return
        val estimate = lastEstimate ?: return
        val now = System.currentTimeMillis()
        if (now - lastSubmittedAt < 60_000) return
        lastSubmittedAt = now

        ridesRepo.submit(
            // Se o motorista estiver logado, o UID da conta identifica a
            // corrida; caso contrário, cai no id anônimo local do aparelho.
            driverId = authRepo.driverUid ?: settings.driverId,
            sourceApp = packageName,
            estimate = estimate,
            costPerKmBrl = settings.costPerKmBrl
        )
        Log.i(TAG, "Corrida persistida localmente e enfileirada para upload")
    }

    private fun collectTexts(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        node ?: return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let(out::add)
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(out::add)
        for (i in 0 until node.childCount) {
            collectTexts(node.getChild(i), out)
        }
    }

    private fun extractPrice(text: String): Double? {
        // A tela de oferta pode exibir vários valores em R$ (tarifa, gorjeta,
        // total). O valor da corrida costuma ser o maior — escolhemos esse,
        // em vez do primeiro encontrado, para não casar com um valor parcial.
        val m = PRICE_PATTERN.matcher(text)
        var best: Double? = null
        while (m.find()) {
            val raw = m.group(1) ?: continue
            val value = raw.replace(" ", "").replace(".", "").replace(",", ".")
                .toDoubleOrNull() ?: continue
            if (best == null || value > best) best = value
        }
        return best
    }

    private fun extractDistanceKm(text: String): Double? {
        val m = DISTANCE_PATTERN.matcher(text)
        var totalKm = 0.0
        var found = false
        while (m.find()) {
            val raw = m.group(1) ?: continue
            val unit = m.group(2)?.lowercase() ?: continue
            val value = raw.replace(",", ".").toDoubleOrNull() ?: continue
            totalKm += if (unit == "m") value / 1000.0 else value
            found = true
        }
        return if (found) totalKm else null
    }

    private fun extractDurationMin(text: String): Int? {
        val m = DURATION_PATTERN.matcher(text)
        var totalMin = 0
        var found = false
        while (m.find()) {
            val hours = m.group(1)?.toIntOrNull() ?: 0
            val mins = m.group(2)?.toIntOrNull() ?: 0
            totalMin += hours * 60 + mins
            found = true
        }
        return if (found) totalMin else null
    }

    override fun onInterrupt() {
        Log.w(TAG, "Serviço de acessibilidade interrompido")
        overlayManager?.hide()
    }

    override fun onDestroy() {
        overlayManager?.hide()
        overlayManager = null
        super.onDestroy()
    }
}
