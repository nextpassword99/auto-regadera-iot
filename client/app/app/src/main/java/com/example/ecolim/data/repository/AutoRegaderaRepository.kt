package com.example.ecolim.data.repository

import android.util.Log
import com.example.ecolim.data.models.*
import com.example.ecolim.data.network.ApiClient
import com.example.ecolim.data.network.WebSocketClient
import com.example.ecolim.data.preferences.ServerConfigManager
import kotlinx.coroutines.flow.SharedFlow
import retrofit2.Response

class AutoRegaderaRepository(private val serverConfigManager: ServerConfigManager) {
    companion object {
        private const val TAG = "AutoRegaderaRepository"
    }

    private val apiService = ApiClient.apiService
    private val webSocketClient = WebSocketClient(serverConfigManager)

    // Flows para datos en tiempo real
    val sensorDataFlow: SharedFlow<SensorReading> = webSocketClient.sensorDataFlow
    val connectionStatusFlow: SharedFlow<Boolean> = webSocketClient.connectionStatusFlow

    // Métodos de WebSocket
    fun connectWebSocket() {
        webSocketClient.connect()
    }

    fun disconnectWebSocket() {
        webSocketClient.disconnect()
    }

    fun reconnectWebSocket() {
        webSocketClient.reconnect()
    }

    // Métodos para lecturas de sensores
    suspend fun getLatestSensorReading(): Result<SensorReading> {
        return try {
            val response = apiService.getLatestSensorReading()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo última lectura: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getSensorReadings(
        skip: Int = 0,
        limit: Int = 100,
        startDate: String? = null,
        endDate: String? = null
    ): Result<List<SensorReading>> {
        return try {
            val response = apiService.getSensorReadings(skip, limit, startDate, endDate)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo lecturas: ${e.message}")
            Result.failure(e)
        }
    }

    // Métodos para eventos de riego
    suspend fun getWateringEvents(
        skip: Int = 0,
        limit: Int = 50,
        startDate: String? = null,
        endDate: String? = null
    ): Result<List<WateringEvent>> {
        return try {
            val response = apiService.getWateringEvents(skip, limit, startDate, endDate)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo eventos de riego: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun createWateringEvent(event: WateringEventCreate): Result<WateringEvent> {
        return try {
            val response = apiService.createWateringEvent(event)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creando evento de riego: ${e.message}")
            Result.failure(e)
        }
    }

    // Método para estadísticas
    suspend fun getStats(startDate: String, endDate: String): Result<StatsResponse> {
        return try {
            val response = apiService.getStats(startDate, endDate)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo estadísticas: ${e.message}")
            Result.failure(e)
        }
    }

    // Método utilitario para obtener datos de las últimas 24 horas
    suspend fun getLast24HoursData(): Result<List<SensorReading>> {
        val endDate = java.time.LocalDateTime.now().toString()
        val startDate = java.time.LocalDateTime.now().minusHours(24).toString()
        return getSensorReadings(limit = 200, startDate = startDate, endDate = endDate)
    }
}