package com.sarathi.emergency.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sarathi.emergency.data.models.HospitalCase
import com.sarathi.emergency.data.models.UpdateCaseStatusRequest
import com.sarathi.emergency.data.repository.RepoResult
import com.sarathi.emergency.data.repository.SarathiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HospitalDashboardUiState(
    val cases: List<HospitalCase> = emptyList(),
    val loading: Boolean = false,
    val lastRefreshLabel: String = "Not refreshed yet",
    val error: String? = null
)

class HospitalDashboardViewModel(
    private val repository: SarathiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HospitalDashboardUiState())
    val uiState: StateFlow<HospitalDashboardUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    fun startPolling(hospitalId: String?, hospitalName: String?, intervalMs: Long = 3000L) {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                refresh(hospitalId, hospitalName)
                delay(intervalMs)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun refresh(hospitalId: String?, hospitalName: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            when (val result = repository.getHospitalCases(hospitalId, hospitalName)) {
                is RepoResult.Success<List<HospitalCase>> -> {
                    _uiState.value = _uiState.value.copy(
                        cases = result.data,
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

    fun updateCaseStatus(caseId: String, newStatus: String) {
        viewModelScope.launch {
            when (
                repository.updateHospitalCaseStatus(
                    UpdateCaseStatusRequest(tripId = caseId, hospitalCaseStatus = newStatus)
                )
            ) {
                is RepoResult.Success<Unit> -> {
                    _uiState.value = _uiState.value.copy(
                        cases = _uiState.value.cases.map {
                            if (it.id == caseId) it.copy(hospitalCaseStatus = newStatus) else it
                        }
                    )
                }

                is RepoResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = "Failed to update case status")
                }
            }
        }
    }
}

class HospitalDashboardViewModelFactory(
    private val repository: SarathiRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HospitalDashboardViewModel::class.java)) {
            return HospitalDashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
