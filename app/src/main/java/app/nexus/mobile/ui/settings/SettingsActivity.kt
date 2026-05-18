package app.nexus.mobile.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import app.nexus.mobile.R
import app.nexus.mobile.data.repository.AuthRepository
import app.nexus.mobile.data.repository.DriverSettingsRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: DriverSettingsRepository
    private val authRepo = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings = DriverSettingsRepository(this)
        bindAccountSection()

        val costField = findViewById<TextInputEditText>(R.id.edit_cost_per_km)
        val sendSwitch = findViewById<MaterialSwitch>(R.id.switch_send_data)

        costField.setText("%.2f".format(settings.costPerKmBrl))
        sendSwitch.isChecked = settings.sendDataEnabled

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val raw = costField.text?.toString()?.replace(',', '.')
            val parsed = raw?.toDoubleOrNull()
            if (parsed != null && parsed > 0) {
                settings.costPerKmBrl = parsed
            }
            settings.sendDataEnabled = sendSwitch.isChecked
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Mostra a conta logada e o botão de sair — apenas quando o Firebase
     * está configurado e há um motorista autenticado. Após o logout, a
     * [app.nexus.mobile.ui.main.MainActivity] detecta a ausência de
     * sessão e leva de volta ao login.
     */
    private fun bindAccountSection() {
        val section = findViewById<View>(R.id.account_section)
        if (!authRepo.isAvailable || !authRepo.isSignedIn) {
            section.visibility = View.GONE
            return
        }
        section.visibility = View.VISIBLE
        findViewById<TextView>(R.id.account_email).text =
            getString(R.string.account_signed_in_as, authRepo.currentUser?.email.orEmpty())
        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            authRepo.signOut()
            finish()
        }
    }
}
