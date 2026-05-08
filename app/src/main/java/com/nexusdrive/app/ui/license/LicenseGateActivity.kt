package com.nexusdrive.app.ui.license

import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.nexusdrive.app.R
import com.nexusdrive.app.data.model.License
import com.nexusdrive.app.data.repository.LicenseRepository
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Tela de paywall: aparece quando trial e licença ativa expiraram.
 * Também é acessível pelo botão "Ver licença" para renovar antes do
 * vencimento.
 */
class LicenseGateActivity : AppCompatActivity() {

    private lateinit var repo: LicenseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license_gate)
        repo = LicenseRepository(this)

        val status = findViewById<TextView>(R.id.license_status)
        val codeField = findViewById<TextInputEditText>(R.id.edit_code)

        renderStatus(status)

        findViewById<Button>(R.id.btn_redeem).setOnClickListener {
            val code = codeField.text?.toString().orEmpty()
            when (val result = repo.redeem(code)) {
                is LicenseRepository.RedeemResult.Success -> {
                    val date = DateFormat.getDateFormat(this)
                        .format(Date(result.newExpiryEpochMs))
                    Toast.makeText(
                        this,
                        getString(R.string.license_redeem_success, result.daysAdded, date),
                        Toast.LENGTH_LONG
                    ).show()
                    codeField.setText("")
                    renderStatus(status)
                    finish()
                }
                LicenseRepository.RedeemResult.AlreadyUsed ->
                    toast(R.string.license_error_already_used)
                LicenseRepository.RedeemResult.InvalidSignature,
                LicenseRepository.RedeemResult.UnknownVersion ->
                    toast(R.string.license_error_invalid)
                LicenseRepository.RedeemResult.MalformedFormat ->
                    toast(R.string.license_error_malformed)
            }
        }
    }

    private fun renderStatus(view: TextView) {
        val state = repo.current()
        view.text = when (state) {
            is License.Trial -> {
                val hours = TimeUnit.MILLISECONDS.toHours(state.remainingMillis())
                getString(R.string.license_status_trial, hours)
            }
            is License.Active -> {
                val days = TimeUnit.MILLISECONDS.toDays(state.remainingMillis())
                val date = DateFormat.getDateFormat(this).format(Date(state.expiresAtEpochMs))
                getString(R.string.license_status_active, days, date)
            }
            License.Expired -> getString(R.string.license_status_expired)
        }
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }
}
