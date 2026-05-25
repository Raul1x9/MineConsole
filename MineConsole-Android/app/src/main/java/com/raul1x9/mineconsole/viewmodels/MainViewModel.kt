package com.raul1x9.mineconsole.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raul1x9.mineconsole.data.ServerDatabase
import com.raul1x9.mineconsole.models.ServerProfile
import com.raul1x9.mineconsole.security.SecurityHelper
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

    fun setBiometricsEnabled(enabled: Boolean) {
        biometricsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("biometrics_enabled", enabled).apply()
    }

    fun setTailscaleConnected(connected: Boolean) {
        tailscaleConnected.value = connected
        sharedPrefs.edit().putBoolean("tailscale_connected", connected).apply()
    }

    fun addServer(name: String, ip: String, port: Int, pass: String) {
        viewModelScope.launch {
            val keychainKey = "mineconsole.password.${UUID.randomUUID()}"
            
            // Save encrypted password
            securityHelper.saveString(keychainKey, pass)

            // Save server metadata
            val newProfile = ServerProfile(
                name = name,
                ip = ip,
                rconPort = port,
                keychainKey = keychainKey
            )
            serverDao.insertServer(newProfile)
        }
    }

    fun deleteServer(server: ServerProfile) {
        viewModelScope.launch {
            // Delete encrypted password
            securityHelper.delete(server.keychainKey)

            // Delete server metadata
            serverDao.deleteServer(server)
        }
    }
}
