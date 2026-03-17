package com.sarathi.emergency.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarathi.emergency.data.SessionManager
import com.sarathi.emergency.data.models.*
import com.sarathi.emergency.data.repository.RepoResult
import com.sarathi.emergency.data.repository.SarathiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DriverUiState {
    object Idle : DriverUiState()
    object Loading : DriverUiState()
    data class Assigned(val trip: AssignedTrip) : DriverUiState()
    data class Success(val message: String) : DriverUiState()
    data class AuthSuccess(val driver: Driver) : DriverUiState()
    data class Error(val message: String) : DriverUiState()
}

class DriverViewModel(
    private val repository: SarathiRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<DriverUiState>(DriverUiState.Idle)
    val uiState: StateFlow<DriverUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPollingForTrips()
    }

    fun startPollingForTrips() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                if (sessionManager.isLoggedIn()) {
                    val driverId = sessionManager.getDriverId()
                    val email = sessionManager.getDriver()?.email
                    
                    when (val result = repository.getAssignedTrip(driverId, email)) {
                        is RepoResult.Success<AssignedTripResponse> -> {
                            val trip = result.data.trip
                            if (trip != null && trip.status != "completed" && trip.status != "cancelled") {
                                if (_uiState.value !is DriverUiState.Assigned || (_uiState.value as? DriverUiState.Assigned)?.trip?.id != trip.id) {
                                    _uiState.value = DriverUiState.Assigned(trip)
                                }
                            } else {
                                if (_uiState.value !is DriverUiState.Idle) {
                                    _uiState.value = DriverUiState.Idle
                                }
                            }
                        }
                        is RepoResult.Error -> {
                            // Silently fail polling
                        }
                    }
                }
                delay(3000) // Poll for new assignments every 3 seconds for speed
            }
        }
    }

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _uiState.value = DriverUiState.Loading
            when (val result = repository.driverLogin(request)) {
                is RepoResult.Success<LoginResponse> -> {
                    if (result.data.success && result.data.driver != null) {
                        sessionManager.saveDriverSession(result.data.driver)
                        sessionManager.saveAuthToken(result.data.token ?: "")
                        _uiState.value = DriverUiState.AuthSuccess(result.data.driver)
                    } else {
                        _uiState.value = DriverUiState.Error(result.data.message ?: "Login failed")
                    }
                }
                is RepoResult.Error -> {
                    _uiState.value = DriverUiState.Error(result.message)
                }
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _uiState.value = DriverUiState.Loading
            when (val result = repository.driverRegister(request)) {
                is RepoResult.Success<RegisterResponse> -> {
                    if (result.data.success && result.data.driver != null) {
                        sessionManager.saveDriverSession(result.data.driver)
                        sessionManager.saveAuthToken(result.data.token ?: "")
                        _uiState.value = DriverUiState.AuthSuccess(result.data.driver)
                    } else {
                        _uiState.value = DriverUiState.Error(result.data.message ?: "Registration failed")
                    }
                }
                is RepoResult.Error -> {
                    _uiState.value = DriverUiState.Error(result.message)
                }
            }
        }
    }

    fun updateTripStatus(tripId: String, status: String? = null, stage: String? = null) {
        val driverId = sessionManager.getDriverId()
        viewModelScope.launch {
            _uiState.value = DriverUiState.Loading
            val request = DriverUpdateRequest(
                driverId = driverId,
                tripId = tripId,
                status = status,
                stage = stage
            )

            when (val result = repository.updateDriverState(request)) {
                is RepoResult.Success -> {
                    _uiState.value = DriverUiState.Success(result.data.message ?: "Status updated")
                    // If assigned trip comes back in response, update state
                    result.data.trip?.let {
                        // Map AssignedTrip back from result context if needed, 
                        // but usually the next poll will refresh it.
                    }
                    startPollingForTrips() // Resume polling to get latest state
                }
                is RepoResult.Error -> {
                    _uiState.value = DriverUiState.Error(result.message)
                }
            }
        }
    }

    fun logout() {
        sessionManager.logout()
        _uiState.value = DriverUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

class DriverViewModelFactory(
    private val repository: SarathiRepository,
    private val sessionManager: SessionManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: java.lang.Class<T>): T {
        if (modelClass.isAssignableFrom(DriverViewModel::class.java)) {
            return DriverViewModel(repository, sessionManager) as T
        }
        throw java.lang.IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
