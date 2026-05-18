package com.nexusdrive.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.nexusdrive.app.R
import com.nexusdrive.app.data.model.License
import com.nexusdrive.app.data.repository.AuthRepository
import com.nexusdrive.app.data.repository.LicenseRepository
import com.nexusdrive.app.ui.admin.AdminPinActivity
import com.nexusdrive.app.ui.auth.LoginActivity
import com.nexusdrive.app.ui.debug.DebugActivity
import com.nexusdrive.app.ui.license.LicenseGateActivity
import com.nexusdrive.app.ui.onboarding.OnboardingActivity
import com.nexusdrive.app.ui.settings.SettingsActivity
import com.nexusdrive.app.util.PermissionUtils
import com.nexusdrive.app.viewmodel.MainViewModel
import java.util.Date
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var licenseRepo: LicenseRepository
    private val authRepo = AuthRepository()

    /** Tap-counter na logo libera a área admin. */
    private var titleTapCount = 0
    private var lastTitleTapAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        licenseRepo = LicenseRepository(this)

        findViewById<TextView>(R.id.title).setOnClickListener { handleTitleTap() }

        findViewById<Button>(R.id.btn_view_license).setOnClickListener {
            startActivity(Intent(this, LicenseGateActivity::class.java))
        }
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btn_debug).setOnClickListener {
            startActivity(Intent(this, DebugActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        val accessibilityOk = PermissionUtils.isAccessibilityServiceEnabled(this)
        val overlayOk = PermissionUtils.canDrawOverlays(this)
        if (!accessibilityOk || !overlayOk) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // Login do motorista — exigido apenas quando o Firebase está
        // configurado neste build. Sem Firebase, esta etapa é ignorada.
        if (authRepo.isAvailable && !authRepo.isSignedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        findViewById<TextView>(R.id.status_accessibility)
            .text = getString(R.string.status_accessibility_granted)
        findViewById<TextView>(R.id.status_overlay)
            .text = getString(R.string.status_overlay_granted)
        viewModel.checkRequiredPermissions(this)

        // Acesso (teste/licença): sincroniza com o Firestore da conta
        // antes de decidir o gate, pois o estado pode ter mudado em
        // outro aparelho. Se expirou, joga direto pro paywall.
        lifecycleScope.launch {
            licenseRepo.sync(authRepo.driverUid)
            when (val license = licenseRepo.current()) {
                is License.Expired ->
                    startActivity(Intent(this@MainActivity, LicenseGateActivity::class.java))
                else -> renderLicenseBanner(license)
            }
        }
    }

    private fun renderLicenseBanner(license: License) {
        val banner = findViewById<TextView>(R.id.license_banner)
        banner.text = when (license) {
            is License.Trial -> {
                val hours = TimeUnit.MILLISECONDS.toHours(license.remainingMillis())
                getString(R.string.license_status_trial, hours)
            }
            is License.Active -> {
                val days = TimeUnit.MILLISECONDS.toDays(license.remainingMillis())
                val date = DateFormat.getDateFormat(this).format(Date(license.expiresAtEpochMs))
                getString(R.string.license_status_active, days, date)
            }
            License.Expired -> getString(R.string.license_status_expired)
        }
    }

    /**
     * 7 toques rápidos na logo abrem o gate de PIN da área admin.
     * Reseta se houver mais de 1.5s entre toques.
     */
    private fun handleTitleTap() {
        val now = System.currentTimeMillis()
        if (now - lastTitleTapAt > 1500) titleTapCount = 0
        lastTitleTapAt = now
        titleTapCount++
        if (titleTapCount >= 7) {
            titleTapCount = 0
            startActivity(Intent(this, AdminPinActivity::class.java))
        }
    }
}
