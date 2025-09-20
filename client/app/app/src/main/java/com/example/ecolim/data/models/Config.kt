package com.example.ecolim.data.models

import com.google.gson.annotations.SerializedName

data class ESPConfig(
    @SerializedName("soil_type")
    val soilType: String,
    @SerializedName("light_threshold")
    val lightThreshold: Int,
    @SerializedName("watering_duration")
    val wateringDuration: Int,
    @SerializedName("watering_interval")
    val wateringInterval: Int
)

data class StatsResponse(
    @SerializedName("time_range")
    val timeRange: TimeRange,
    @SerializedName("total_readings")
    val totalReadings: Int,
    val humidity: HumidityStats,
    val light: LightStats
)

data class TimeRange(
    val start: String,
    val end: String
)

data class HumidityStats(
    val average: String,
    val max: Double,
    val min: Double
)

data class LightStats(
    val average: String
)