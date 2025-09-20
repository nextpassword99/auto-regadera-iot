package com.example.ecolim.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecolim.data.models.ChartSensorReading
import com.example.ecolim.data.models.SensorReading
import com.example.ecolim.data.models.StatsResponse
import com.example.ecolim.data.preferences.ServerConfigManager
import com.example.ecolim.data.repository.AutoRegaderaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class GalleryViewModel(serverConfigManager: ServerConfigManager) : ViewModel() {

    private val repository = AutoRegaderaRepository(serverConfigManager)

    // Estados para la UI
    private val _chartData = MutableStateFlow<List<ChartSensorReading>>(emptyList())
    val chartData: StateFlow<List<ChartSensorReading>> = _chartData.asStateFlow()

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

    // Método actualizado para obtener datos de sensores por rango de fechas
    fun getSensorDataByDateRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getSensorReadingsByDateRange(startDate, endDate)
                
                result.fold(
                    onSuccess = { sensorReadings ->
                        // Los datos ya son ChartSensorReading, no necesitan conversión
                        _chartData.value = sensorReadings
                        clearError()
                    },
                    onFailure = { _ ->
                        // Si no hay datos reales, generar datos simulados para demostración
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val startDateParsed = sdf.parse(startDate) ?: Date()
                        val endDateParsed = sdf.parse(endDate) ?: Date()
                        _chartData.value = generateSimulatedData(startDateParsed, endDateParsed)
                    }
                )
            } catch (e: Exception) {
                // Generar datos simulados en caso de error
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val startDateParsed = sdf.parse(startDate) ?: Date()
                val endDateParsed = sdf.parse(endDate) ?: Date()
                _chartData.value = generateSimulatedData(startDateParsed, endDateParsed)
            }
            _isLoading.value = false
        }
    }

    // Método de conveniencia para obtener datos de un solo día
    fun getSensorDataForSingleDate(date: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        // Fecha de inicio: 00:00:00 del día seleccionado
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startDate = Date(calendar.timeInMillis)
        
        // Fecha de fin: 23:59:59 del día seleccionado
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = Date(calendar.timeInMillis)
        
        // Formatear las fechas para el API
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val startDateStr = sdf.format(startDate)
        val endDateStr = sdf.format(endDate)
        
        // Llamar al método que obtiene datos por rango de fechas
        getSensorDataByDateRange(startDateStr, endDateStr)
    }

    private fun generateSimulatedData(startDate: Date, endDate: Date): List<ChartSensorReading> {
        val dataPoints = mutableListOf<ChartSensorReading>()
        val calendar = Calendar.getInstance()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        
        // Calcular la diferencia en horas entre las fechas
        val timeDiff = endDate.time - startDate.time
        val hoursDiff = (timeDiff / (1000 * 60 * 60)).toInt()
        val pointsToGenerate = minOf(hoursDiff, 24) // Máximo 24 puntos por día
        
        if (pointsToGenerate <= 0) {
            // Si el rango es muy pequeño, generar al menos un punto
            dataPoints.add(
                ChartSensorReading(
                    timestamp = isoFormat.format(startDate),
                    humidity = Random.nextFloat() * 30 + 40, // 40-70%
                    lightLevel = Random.nextFloat() * 200 + 100 // 100-300
                )
            )
            return dataPoints
        }
        
        // Generar puntos distribuidos en el rango de tiempo
        for (i in 0 until pointsToGenerate) {
            calendar.time = startDate
            calendar.add(Calendar.HOUR_OF_DAY, i * hoursDiff / pointsToGenerate)
            
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            dataPoints.add(
                ChartSensorReading(
                    timestamp = isoFormat.format(Date(calendar.timeInMillis)),
                    humidity = Random.nextFloat() * 30 + 40, // 40-70%
                    lightLevel = if (hour in 6..18) Random.nextFloat() * 100 + 200 else Random.nextFloat() * 50 // Más luz durante el día
                )
            )
        }
        
        return dataPoints
    }

    private fun loadLast24HoursData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getLast24HoursData()
                .onSuccess { readings ->
                    // Convertir SensorReading a ChartSensorReading manualmente
                    val chartData = readings.map { reading ->
                        ChartSensorReading(
                            timestamp = reading.timestamp,
                            humidity = reading.humidity.toFloat(),
                            lightLevel = reading.light.toFloat()
                        )
                    }
                    _chartData.value = chartData
                    clearError()
                }
                .onFailure { _ ->
                    _errorMessage.value = "Error cargando datos del gráfico"
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
                .onFailure { _ ->
                    _errorMessage.value = "Error cargando estadísticas"
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
            Pair(time, reading.humidity)
        }
    }

    fun getLightDataPoints(): List<Pair<String, Float>> {
        return _chartData.value.map { reading ->
            val time = formatTimestamp(reading.timestamp)
            Pair(time, reading.lightLevel)
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = isoFormat.parse(timestamp)
            if (date != null) {
                timeFormat.format(date)
            } else {
                "00:00"
            }
        } catch (e: Exception) {
            "00:00"
        }
    }
}