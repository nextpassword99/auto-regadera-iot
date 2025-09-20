package com.example.ecolim.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecolim.data.models.SensorReading
import com.example.ecolim.data.repository.AutoRegaderaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class HomeViewModel : ViewModel() {

    private val repository = AutoRegaderaRepository()

    // Estados para la UI
    private val _currentSensorData = MutableStateFlow<SensorReading?>(null)
    val currentSensorData: StateFlow<SensorReading?> = _currentSensorData.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Texto para mostrar en la UI (mantenido para compatibilidad)
    private val _text = MutableLiveData<String>().apply {
        value = "Dashboard de Auto-Regadera"
    }
    val text: LiveData<String> = _text

    init {
        connectToWebSocket()
        loadLatestData()
    }

    private fun connectToWebSocket() {
        viewModelScope.launch {
            // Conectar WebSocket
            repository.connectWebSocket()

            // Observar datos en tiempo real
            repository.sensorDataFlow.collect { sensorData ->
                _currentSensorData.value = sensorData
                clearError()
            }
        }

        viewModelScope.launch {
            // Observar estado de conexión
            repository.connectionStatusFlow.collect { isConnected ->
                _isConnected.value = isConnected
            }
        }
    }

    private fun loadLatestData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getLatestSensorReading()
                .onSuccess { reading ->
                    _currentSensorData.value = reading
                    clearError()
                }
                .onFailure { error ->
                    _errorMessage.value = "Error cargando datos: ${error.message}"
                }
            _isLoading.value = false
        }
    }

    fun refreshData() {
        loadLatestData()
    }

    fun reconnectWebSocket() {
        repository.reconnectWebSocket()
    }

    private fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectWebSocket()
    }

    // Métodos utilitarios para formatear datos
    fun getHumidityText(): String {
        return _currentSensorData.value?.humidity?.toInt()?.toString() ?: "---"
    }

    fun getLightText(): String {
        return _currentSensorData.value?.light?.toInt()?.toString() ?: "---"
    }

    fun getPumpStatusText(): String {
        return if (_currentSensorData.value?.pumpStatus == true) "Activa" else "Inactiva"
    }

    fun getModeText(): String {
        return _currentSensorData.value?.mode?.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        } ?: "---"
    }

    fun getSoilTypeText(): String {
        return "Tipo de suelo: ${_currentSensorData.value?.soilType?.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        } ?: "---"}"
    }

    fun getConnectionStatusText(): String {
        return if (_isConnected.value) "Conectado" else "Desconectado"
    }
}