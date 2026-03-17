package com.sarathi.emergency.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sarathi.emergency.data.models.PoliceAlert
import com.sarathi.emergency.data.repository.RepoResult
import com.sarathi.emergency.data.repository.SarathiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PoliceDashboardUiState(
    val alerts: List<PoliceAlert> = emptyList(),
    val loading: Boolean = false,
    val lastRefreshLabel: String = "Not refreshed yet",
    val error: String? = null
)

class PoliceDashboardViewModel(
    private val repository: SarathiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PoliceDashboardUiState())
    val uiState: StateFlow<PoliceDashboardUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    fun startPolling(stationId: String?, stationName: String?, intervalMs: Long = 8000L) {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                refresh(stationId, stationName)
                delay(intervalMs)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun refresh(stationId: String?, stationName: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = repository.getPoliceAlerts(stationId, stationName)) {
                is RepoResult.Success<List<PoliceAlert>> -> {
                    _uiState.value = _uiState.value.copy(
                        alerts = result.data,
                        loading = false,
                        lastRefreshLabel = "Updated now"
                    )
                }

                is RepoResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = result.message,
                        lastRefreshLabel = "Refresh failed"
                    )
                }
            }
        }
    }
}

class PoliceDashboardViewModelFactory(
    private val repository: SarathiRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PoliceDashboardViewModel::class.java)) {
            return PoliceDashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
