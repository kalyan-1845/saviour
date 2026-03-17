package com.sarathi.emergency.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarathi.emergency.data.models.SosRequest
import com.sarathi.emergency.data.models.SosResponse
import com.sarathi.emergency.data.models.TrackResponse
import com.sarathi.emergency.data.repository.RepoResult
import com.sarathi.emergency.data.repository.SarathiRepository
import com.sarathi.emergency.util.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SosUiState {
    object Idle : SosUiState()
    object Loading : SosUiState()
    data class Active(val response: SosResponse) : SosUiState()
    data class Tracking(val response: TrackResponse) : SosUiState()
    data class Error(val message: String) : SosUiState()
}

class SOSViewModel(
    private val repository: SarathiRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<SosUiState>(SosUiState.Idle)
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private var trackingJob: Job? = null

    init {
        fetchLocation()
    }

    fun fetchLocation() {
        locationHelper.getLastLocation { location ->
            _currentLocation.value = location
        }
    }

    fun triggerSos(phone: String, emergencyType: String = "medical") {
        val location = _currentLocation.value
        if (location == null) {
            _uiState.value = SosUiState.Error("Location not available. Please ensure GPS is on.")
            return
        }

        viewModelScope.launch {
            _uiState.value = SosUiState.Loading
            val request = SosRequest(
                phone = phone,
                latitude = location.latitude,
                longitude = location.longitude,
                emergencyType = emergencyType
            )

            when (val result = repository.sendSos(request)) {
                is RepoResult.Success -> {
                    _uiState.value = SosUiState.Active(result.data)
                    startTracking(result.data.tripId)
                }
                is RepoResult.Error -> {
                    _uiState.value = SosUiState.Error(result.message)
                }
            }
        }
    }

    private fun startTracking(tripId: String) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            while (true) {
                when (val result = repository.trackSos(null, tripId)) {
                    is RepoResult.Success -> {
                        _uiState.value = SosUiState.Tracking(result.data)
                        if (result.data.trip?.status == "completed" || result.data.trip?.status == "cancelled") {
                            break
                        }
                    }
                    is RepoResult.Error -> {
                        // Log error but continue polling
                    }
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun reset() {
        trackingJob?.cancel()
        _uiState.value = SosUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
    }
}

class SOSViewModelFactory(
    private val repository: SarathiRepository,
    private val locationHelper: LocationHelper
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: java.lang.Class<T>): T {
        if (modelClass.isAssignableFrom(SOSViewModel::class.java)) {
            return SOSViewModel(repository, locationHelper) as T
        }
        throw java.lang.IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
