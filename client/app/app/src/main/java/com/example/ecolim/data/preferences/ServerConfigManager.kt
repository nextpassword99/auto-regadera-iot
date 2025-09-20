package com.example.ecolim.data.preferences

import android.content.Context
import android.content.SharedPreferences

class ServerConfigManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "server_config"
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        
        // Valores por defecto
        const val DEFAULT_IP = "192.168.18.21"
        const val DEFAULT_PORT = "8000"
    }

    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverIp: String
        get() = sharedPreferences.getString(KEY_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
        set(value) = sharedPreferences.edit().putString(KEY_SERVER_IP, value).apply()

    var serverPort: String
        get() = sharedPreferences.getString(KEY_SERVER_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        set(value) = sharedPreferences.edit().putString(KEY_SERVER_PORT, value).apply()

    fun getServerUrl(): String {
        return "http://$serverIp:$serverPort"
    }

    fun getWebSocketUrl(): String {
        return "ws://$serverIp:$serverPort"
    }

    fun resetToDefaults() {
        serverIp = DEFAULT_IP
        serverPort = DEFAULT_PORT
    }

    fun isValidConfig(): Boolean {
        return serverIp.isNotBlank() && serverPort.isNotBlank() && 
               serverPort.toIntOrNull() != null
    }
}