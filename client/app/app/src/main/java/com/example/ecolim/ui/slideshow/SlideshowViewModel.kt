package com.example.ecolim.ui.slideshow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecolim.data.models.WateringEvent
import com.example.ecolim.data.repository.AutoRegaderaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SlideshowViewModel : ViewModel() {

    private val repository = AutoRegaderaRepository()

    // Estados para la UI
    private val _wateringEvents = MutableStateFlow<List<WateringEvent>>(emptyList())
    val wateringEvents: StateFlow<List<WateringEvent>> = _wateringEvents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Texto para mostrar en la UI (mantenido para compatibilidad)
    private val _text = MutableLiveData<String>().apply {
        value = "Historial de Eventos de Riego"
    }
    val text: LiveData<String> = _text

    init {
        loadWateringEvents()
    }

    private fun loadWateringEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getWateringEvents(limit = 50)
                .onSuccess { events ->
                    _wateringEvents.value = events
                    clearError()
                }
                .onFailure { error ->
                    _errorMessage.value = "Error cargando eventos de riego: ${error.message}"
                }
            _isLoading.value = false
        }
    }

    fun loadEventsForDateRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getWateringEvents(limit = 100, startDate = startDate, endDate = endDate)
                .onSuccess { events ->
                    _wateringEvents.value = events
                    clearError()
                }
                .onFailure { error ->
                    _errorMessage.value = "Error cargando eventos de riego: ${error.message}"
                }
            _isLoading.value = false
        }
    }

    fun refreshEvents() {
        loadWateringEvents()
    }

    private fun clearError() {
        _errorMessage.value = null
    }

    // Métodos utilitarios para formatear datos de eventos
    fun formatDateTime(timestamp: String): String {
        return try {
            val dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        } catch (e: Exception) {
            timestamp
        }
    }

    fun formatDuration(durationSeconds: Int): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return when {
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun formatReason(reason: String): String {
        return when (reason.lowercase()) {
            "automatico" -> "Automático"
            "manual" -> "Manual"
            else -> reason.replaceFirstChar { it.uppercaseChar() }
        }
    }
}