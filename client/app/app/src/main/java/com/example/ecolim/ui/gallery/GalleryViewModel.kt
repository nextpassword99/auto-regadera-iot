package com.example.ecolim.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecolim.data.models.SensorReading
import com.example.ecolim.data.models.StatsResponse
import com.example.ecolim.data.repository.AutoRegaderaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GalleryViewModel : ViewModel() {

    private val repository = AutoRegaderaRepository()

    // Estados para la UI
    private val _chartData = MutableStateFlow<List<SensorReading>>(emptyList())
    val chartData: StateFlow<List<SensorReading>> = _chartData.asStateFlow()

    private val _statsData = MutableStateFlow<StatsResponse?>(null)
    val statsData: StateFlow<StatsResponse?> = _statsData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Texto para mostrar en la UI (mantenido para compatibilidad)
    private val _text = MutableLiveData<String>().apply {
        value = "Historial y Estadísticas"
    }
    val text: LiveData<String> = _text

    init {
        loadLast24HoursData()
    }

    private fun loadLast24HoursData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getLast24HoursData()
                .onSuccess { readings ->
                    _chartData.value = readings
                    clearError()
                }
                .onFailure { error ->
                    _errorMessage.value = "Error cargando datos del gráfico: ${error.message}"
                }
            _isLoading.value = false
        }
    }

    fun loadStatsForDateRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getStats(startDate, endDate)
                .onSuccess { stats ->
                    _statsData.value = stats
                    clearError()
                }
                .onFailure { error ->
                    _errorMessage.value = "Error cargando estadísticas: ${error.message}"
                }
            _isLoading.value = false
        }
    }

    fun refreshChartData() {
        loadLast24HoursData()
    }

    private fun clearError() {
        _errorMessage.value = null
    }

    // Métodos utilitarios para formatear datos del gráfico
    fun getHumidityDataPoints(): List<Pair<String, Float>> {
        return _chartData.value.map { reading ->
            val time = formatTimestamp(reading.timestamp)
            Pair(time, reading.humidity.toFloat())
        }
    }

    fun getLightDataPoints(): List<Pair<String, Float>> {
        return _chartData.value.map { reading ->
            val time = formatTimestamp(reading.timestamp)
            Pair(time, reading.light.toFloat())
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            // Asumiendo formato ISO: "2024-01-01T12:00:00"
            val dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            timestamp.substring(0, minOf(5, timestamp.length))
        }
    }
}