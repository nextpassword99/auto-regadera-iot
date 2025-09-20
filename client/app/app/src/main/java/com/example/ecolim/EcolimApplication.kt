package com.example.ecolim

import android.app.Application
import com.example.ecolim.data.network.ApiClient
import com.example.ecolim.data.preferences.ServerConfigManager

class EcolimApplication : Application() {

    lateinit var serverConfigManager: ServerConfigManager
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Inicializar configuración del servidor
        serverConfigManager = ServerConfigManager(this)
        
        // Inicializar cliente API con la configuración
        ApiClient.initialize(serverConfigManager)
    }
}