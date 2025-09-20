package com.example.ecolim.data.models

import com.google.gson.annotations.SerializedName

data class WateringEvent(
    val id: Int,
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("duration_seconds")
    val durationSeconds: Int,
    val reason: String
)

data class WateringEventCreate(
    @SerializedName("duration_seconds")
    val durationSeconds: Int,
    val reason: String
)