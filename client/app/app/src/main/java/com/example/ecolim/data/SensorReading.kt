package com.example.ecolim.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Clase para datos de sensores extendida para el gráfico
data class ChartSensorReading(
    val timestamp: String,
    val humidity: Float,
    val lightLevel: Float
)