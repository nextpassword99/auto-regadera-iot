package com.example.ecolim.data.network

import com.example.ecolim.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    companion object {
        const val BASE_URL = "http://192.168.18.21:8000" // Cambia por tu IP del servidor
        const val DEFAULT_BASE_URL = "http://192.168.18.21:8000/" // URL por defecto con slash final
    }

    // Endpoints de lecturas de sensores
    @POST("/api/v1/readings/")
    suspend fun createSensorReading(@Body reading: SensorReadingCreate): Response<SensorReading>

    @GET("/api/v1/readings/")
    suspend fun getSensorReadings(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<List<SensorReading>>

    @GET("/api/v1/readings/latest/")
    suspend fun getLatestSensorReading(): Response<SensorReading>

    // Endpoints de eventos de riego
    @POST("/api/v1/watering-events/")
    suspend fun createWateringEvent(@Body event: WateringEventCreate): Response<WateringEvent>

    @GET("/api/v1/watering-events/")
    suspend fun getWateringEvents(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<List<WateringEvent>>

    // Endpoint de estadísticas
    @GET("/api/v1/stats/")
    suspend fun getStats(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Response<StatsResponse>
}