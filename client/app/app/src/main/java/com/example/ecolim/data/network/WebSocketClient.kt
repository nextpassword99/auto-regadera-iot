package com.example.ecolim.data.network

import android.util.Log
import com.example.ecolim.data.models.SensorReading
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import okio.ByteString

class WebSocketClient {
    companion object {
        private const val TAG = "WebSocketClient"
        private const val SERVER_URL = "ws://192.168.18.21:8000" // Cambia por tu IP del servidor
        private const val WS_UI_FEED_PATH = "/ws/ui-feed"
    }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private val gson = Gson()

    // SharedFlow para emitir datos en tiempo real
    private val _sensorDataFlow = MutableSharedFlow<SensorReading>()
    val sensorDataFlow: SharedFlow<SensorReading> = _sensorDataFlow

    private val _connectionStatusFlow = MutableSharedFlow<Boolean>()
    val connectionStatusFlow: SharedFlow<Boolean> = _connectionStatusFlow

    fun connect() {
        val request = Request.Builder()
            .url("$SERVER_URL$WS_UI_FEED_PATH")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "🟢 Conectado al WebSocket del servidor")
                _connectionStatusFlow.tryEmit(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "📨 Mensaje recibido: $text")
                try {
                    val sensorReading = gson.fromJson(text, SensorReading::class.java)
                    _sensorDataFlow.tryEmit(sensorReading)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parseando JSON: ${e.message}")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "📨 Mensaje binario recibido: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "🔴 Cerrando conexión: $code / $reason")
                _connectionStatusFlow.tryEmit(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "🔴 Conexión cerrada: $code / $reason")
                _connectionStatusFlow.tryEmit(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "❌ Error en WebSocket: ${t.message}")
                _connectionStatusFlow.tryEmit(false)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Desconexión manual")
        webSocket = null
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    // Método para reconectar automáticamente
    fun reconnect() {
        disconnect()
        connect()
    }
}