package com.nexusdrive.app.viewmodel

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class PermissionState(
    val overlayGranted: Boolean = false,
    val accessibilityGranted: Boolean = false
)

class MainViewModel : ViewModel() {

    private val _permissions = MutableLiveData(PermissionState())
    val permissions: LiveData<PermissionState> = _permissions

    fun checkRequiredPermissions(context: Context) {
        _permissions.value = PermissionState(
            overlayGranted = Settings.canDrawOverlays(context),
            accessibilityGranted = isAccessibilityEnabled(context)
        )
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(context.packageName)
    }
}
