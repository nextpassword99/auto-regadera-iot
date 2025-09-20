package com.example.ecolim.data.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class SensorReading(
    val id: Int,
    val timestamp: String,
    val humidity: Double,
    val light: Double,
    @SerializedName("pump_status")
    val pumpStatus: Boolean,
    val mode: String,
    @SerializedName("soil_type")
    val soilType: String
)

data class SensorReadingCreate(
    val humidity: Double,
    val light: Double,
    @SerializedName("pump_status")
    val pumpStatus: Boolean,
    val mode: String,
    @SerializedName("soil_type")
    val soilType: String
)