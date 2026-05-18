package app.nexus.mobile.util

import android.content.Context
import android.content.pm.PackageManager

/**
 * Identifica em runtime quais apps de motorista o usuário tem instalados.
 *
 * Pacotes Uber/99 mudaram historicamente; manter uma lista de candidatos
 * e verificar instalação evita ban silencioso quando o nome muda.
 */
object PackageDiscovery {

    /** Candidatos conhecidos da Uber Driver. */
    private val UBER_CANDIDATES = listOf(
        "com.ubercab.driver",
        "com.ubercab.driver.debug"
    )

    /** Candidatos conhecidos da 99 Driver. */
    private val NINETYNINE_CANDIDATES = listOf(
        "com.taxis99.driver",
        "com.taxis99",
        "br.com.taxis99.driver"
    )

    /**
     * Candidato conhecido do inDrive. É um app unificado: o mesmo APK
     * atende motorista e passageiro (o papel é trocado dentro do app).
     */
    private val INDRIVE_CANDIDATES = listOf(
        "sinet.startup.inDriver"
    )

    /** Todos os candidatos suportados. */
    private val ALL_CANDIDATES = UBER_CANDIDATES + NINETYNINE_CANDIDATES + INDRIVE_CANDIDATES

    /** Retorna apenas os pacotes que estão de fato instalados no aparelho. */
    fun installedDriverApps(context: Context): Set<String> {
        val pm = context.packageManager
        return ALL_CANDIDATES.filter { isInstalled(pm, it) }.toSet()
    }

    /**
     * Lista que o serviço de acessibilidade usa em runtime.
     * Inclui *todos* os candidatos (não só instalados) para que o
     * filtro funcione mesmo se o usuário instalar Uber/99 depois.
     */
    fun supportedPackages(): Set<String> = ALL_CANDIDATES.toSet()

    private fun isInstalled(pm: PackageManager, pkg: String): Boolean = try {
        pm.getPackageInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
