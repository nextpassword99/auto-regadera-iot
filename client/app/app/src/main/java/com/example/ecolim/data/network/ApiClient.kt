package com.example.ecolim.data.network

import com.example.ecolim.data.preferences.ServerConfigManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var serverConfigManager: ServerConfigManager? = null
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        .create()

    private var retrofit: Retrofit? = null

    val apiService: ApiService
        get() {
            if (retrofit == null) {
                initializeRetrofit()
            }
            return retrofit!!.create(ApiService::class.java)
        }

    fun initialize(configManager: ServerConfigManager) {
        serverConfigManager = configManager
        initializeRetrofit()
    }

    fun updateServerConfig(configManager: ServerConfigManager) {
        serverConfigManager = configManager
        initializeRetrofit()
    }

    private fun initializeRetrofit() {
        val baseUrl = serverConfigManager?.getServerUrl() ?: ApiService.DEFAULT_BASE_URL
        val finalUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        retrofit = Retrofit.Builder()
            .baseUrl(finalUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun getCurrentServerUrl(): String {
        return serverConfigManager?.getServerUrl() ?: ApiService.DEFAULT_BASE_URL
    }
}