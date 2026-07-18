package com.raul1x9.mineconsole.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raul1x9.mineconsole.data.ServerDatabase
import com.raul1x9.mineconsole.models.ServerProfile
import com.raul1x9.mineconsole.security.SecurityHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ServerDatabase.getInstance(application)
    private val serverDao = db.serverDao()
    private val securityHelper = SecurityHelper.getInstance(application)
    private val sharedPrefs = application.getSharedPreferences("mineconsole_settings", Context.MODE_PRIVATE)

    val servers: StateFlow<List<ServerProfile>> = serverDao.getAllServersFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var biometricsEnabled = mutableStateOf(sharedPrefs.getBoolean("biometrics_enabled", false))
        private set

    var tailscaleConnected = mutableStateOf(sharedPrefs.getBoolean("tailscale_connected", true))
        private set

    var appTheme = mutableStateOf(sharedPrefs.getString("app_theme", "System") ?: "System")
        private set

    var appAccentColor = mutableStateOf(sharedPrefs.getString("app_accent_color", "Green") ?: "Green")
        private set

    var currentScreen = mutableStateOf("dashboard")
    var selectedServer = mutableStateOf<ServerProfile?>(null)

    fun setBiometricsEnabled(enabled: Boolean) {
        biometricsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("biometrics_enabled", enabled).apply()
    }

    fun setTailscaleConnected(connected: Boolean) {
        tailscaleConnected.value = connected
        sharedPrefs.edit().putBoolean("tailscale_connected", connected).apply()
    }

    fun setAppTheme(theme: String) {
        appTheme.value = theme
        sharedPrefs.edit().putString("app_theme", theme).apply()
    }

    fun setAppAccentColor(color: String) {
        appAccentColor.value = color
        sharedPrefs.edit().putString("app_accent_color", color).apply()
    }

    fun addServer(name: String, ip: String, port: Int, pass: String, connectionType: String = "RCON", useTLS: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val keychainKey = "mineconsole.password.${UUID.randomUUID()}"
            
            // Save encrypted password
            securityHelper.saveString(keychainKey, pass)

            // Save server metadata
            val newProfile = ServerProfile(
                name = name,
                ip = ip,
                rconPort = port,
                keychainKey = keychainKey,
                connectionType = connectionType,
                useTLS = useTLS
            )
            serverDao.insertServer(newProfile)
        }
    }

    fun deleteServer(server: ServerProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete encrypted password
            securityHelper.delete(server.keychainKey)

            // Delete server metadata
            serverDao.deleteServer(server)
        }
    }
}
