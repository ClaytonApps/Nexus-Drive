package com.nexusdrive.app.util

import android.content.Context
import android.provider.Settings
import com.nexusdrive.app.service.DriverMonitorService

object PermissionUtils {

    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    /**
     * Detecta se o nosso `DriverMonitorService` está habilitado nas
     * configurações de acessibilidade. A string retornada pelo
     * `Settings.Secure` tem o formato:
     *   "com.pacote/.NomeServico:com.outro/.OutroServico"
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val target = "${context.packageName}/${DriverMonitorService::class.java.name}"
        return flat.split(':').any { it.equals(target, ignoreCase = true) }
    }
}
