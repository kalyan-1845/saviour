package com.sarathi.emergency.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.models.*
import com.sarathi.emergency.data.repository.RepoResult
import com.sarathi.emergency.data.repository.SarathiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveRouteUiState(
    val tripId: String? = null,
    val status: String = "En Route to Patient",
    val etaMinutes: Int = 7,
    val trafficLevel: String = "Normal",
    val aiAnalysis: String? = null,
    val isAnalyzing: Boolean = false,
    val notifyLoading: Boolean = false,
    val notifyResult: NotifyResponse? = null,
    val destinationLat: Double = 17.4550,
    val destinationLng: Double = 78.4700,
    val destinationName: String = "Emergency Location",
    val error: String? = null
)

class ActiveRouteViewModel(
    private val repository: SarathiRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveRouteUiState())
    val uiState: StateFlow<ActiveRouteUiState> = _uiState.asStateFlow()

    init {
        // Initialize with mission data if available
        val sosId = sessionManager.getSimulatedSOS()
        _uiState.value = _uiState.value.copy(tripId = sosId)
    }

    fun analyzeRoute(lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            val request = GroqRequest(
                origin = "$lat,$lng",
                destination = "${_uiState.value.destinationLat},${_uiState.value.destinationLng}",
                trafficData = mapOf("level" to _uiState.value.trafficLevel)
            )

            when (val result = repository.analyzeRoute(request)) {
                is RepoResult.Success<GroqResponse> -> {
                    _uiState.value = _uiState.value.copy(
                        aiAnalysis = result.data.analysis,
                        isAnalyzing = false
                    )
                }
                is RepoResult.Error -> {
                    Log.e("ActiveRouteVM", "Analysis failed: ${result.message}")
                    _uiState.value = _uiState.value.copy(isAnalyzing = false)
                }
            }
        }
    }

    fun updateMissionStatus(newStatus: String) {
        _uiState.value = _uiState.value.copy(status = newStatus)
        sessionManager.updateSimulatedMissionStatus(newStatus)
    }

    fun notifyAuthorities(lat: Double, lng: Double) {
        val tripId = _uiState.value.tripId ?: "active"
        val driverId = sessionManager.getDriverId()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(notifyLoading = true)
            val request = NotifyRequest(
                tripId = tripId,
                driverId = driverId,
                latitude = lat,
                longitude = lng
            )

            when (val result = repository.notifyAuthorities(request)) {
                is RepoResult.Success<NotifyResponse> -> {
                    _uiState.value = _uiState.value.copy(
                        notifyResult = result.data,
                        notifyLoading = false
                    )
                }
                is RepoResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        notifyLoading = false,
                        error = "Authority notification failed"
                    )
                }
            }
        }
    }

    fun setDestination(lat: Double, lng: Double, name: String) {
        _uiState.value = _uiState.value.copy(
            destinationLat = lat,
            destinationLng = lng,
            destinationName = name
        )
    }
}

class ActiveRouteViewModelFactory(
    private val repository: SarathiRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActiveRouteViewModel::class.java)) {
            return ActiveRouteViewModel(repository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
