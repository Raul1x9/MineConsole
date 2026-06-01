package com.raul1x9.mineconsole.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ConsoleLogManager {
    private val _logs = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val logs: StateFlow<Map<String, List<String>>> = _logs.asStateFlow()

    fun getLogs(serverId: String): List<String> {
        return _logs.value[serverId] ?: emptyList()
    }

    @Synchronized
    fun addLog(serverId: String, message: String) {
        val currentLogs = _logs.value.toMutableMap()
        val serverLogs = currentLogs[serverId]?.toMutableList() ?: mutableListOf()
        serverLogs.add(message)
        
        // Keep only last 1000 lines to prevent memory issues
        if (serverLogs.size > 1000) {
            serverLogs.removeAt(0)
        }

        currentLogs[serverId] = serverLogs
        _logs.value = currentLogs
    }

    @Synchronized
    fun clearLogs(serverId: String) {
        val currentLogs = _logs.value.toMutableMap()
        currentLogs[serverId] = emptyList()
        _logs.value = currentLogs
    }
}
