package com.sarathi.emergency.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sarathi.emergency.data.models.*
import com.sarathi.emergency.data.repository.RepoResult
import com.sarathi.emergency.data.repository.SarathiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HospitalSelectionUiState(
    val hospitals: List<Hospital> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filterSpecialty: String = "",
    val selectedHospitalId: String? = null
)

class HospitalSelectionViewModel(
    private val repository: SarathiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HospitalSelectionUiState())
    val uiState: StateFlow<HospitalSelectionUiState> = _uiState.asStateFlow()

    init {
        fetchHospitals()
    }

    fun fetchHospitals() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // Using placeholder coords for now, in a real app these would be current location
            when (val result = repository.getHospitals(17.4426, 78.5006)) {
                is RepoResult.Success<HospitalListResponse> -> {
                    _uiState.update { it.copy(hospitals = result.data.hospitals, isLoading = false) }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.update(error = result.message, isLoading = false) }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateSpecialtyFilter(specialty: String) {
        _uiState.update { it.copy(filterSpecialty = specialty) }
    }

    fun selectHospital(hospitalId: String?) {
        _uiState.update { it.copy(selectedHospitalId = hospitalId) }
    }

    private fun HospitalSelectionUiState.update(error: String?, isLoading: Boolean): HospitalSelectionUiState {
        return copy(error = error, isLoading = isLoading)
    }
}

class HospitalSelectionViewModelFactory(
    private val repository: SarathiRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HospitalSelectionViewModel::class.java)) {
            return HospitalSelectionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
