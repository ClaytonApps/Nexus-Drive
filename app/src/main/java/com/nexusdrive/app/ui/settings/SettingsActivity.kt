package com.nexusdrive.app.ui.settings

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.nexusdrive.app.R
import com.nexusdrive.app.data.repository.DriverSettingsRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: DriverSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings = DriverSettingsRepository(this)

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
}
