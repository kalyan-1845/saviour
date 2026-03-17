package com.sarathi.emergency.data.repository

import android.util.Log
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.models.*
import retrofit2.Response

sealed class RepoResult<out T> {
    data class Success<T>(val data: T) : RepoResult<T>()
    data class Error(val message: String, val code: Int? = null, val throwable: Throwable? = null) : RepoResult<Nothing>()
}

class SarathiRepository(
    private val api: SarathiApi
) {
    companion object {
        private const val TAG = "SarathiRepository"
    }

    // ─── AUTH ───

    suspend fun driverLogin(request: LoginRequest): RepoResult<LoginResponse> {
        return safeApiCall(
            call = { api.driverLogin(request) },
            mapper = { it }
        )
    }

    suspend fun driverRegister(request: RegisterRequest): RepoResult<RegisterResponse> {
        return safeApiCall(
            call = { api.driverRegister(request) },
            mapper = { it }
        )
    }

    // ─── SOS ───

    suspend fun sendSos(request: SosRequest): RepoResult<SosResponse> {
        return safeApiCall(
            call = { api.sendSos(request) },
            mapper = { it }
        )
    }

    suspend fun trackSos(phone: String?, tripId: String?): RepoResult<TrackResponse> {
        return safeApiCall(
            call = { api.trackSos(phone, tripId) },
            mapper = { it }
        )
    }

    // ─── DRIVER ───

    suspend fun getAssignedTrip(driverId: String?, email: String?): RepoResult<AssignedTripResponse> {
        return safeApiCall(
            call = { api.getAssignedTrip(driverId, email) },
            mapper = { it }
        )
    }

    suspend fun updateDriverState(request: DriverUpdateRequest): RepoResult<DriverUpdateResponse> {
        return safeApiCall(
            call = { api.updateDriver(request) },
            mapper = { it }
        )
    }

    suspend fun notifyAuthorities(request: NotifyRequest): RepoResult<NotifyResponse> {
        return safeApiCall(
            call = { api.notifyAuthorities(request) },
            mapper = { it }
        )
    }

    // ─── HOSPITALS ───
    
    suspend fun getHospitals(lat: Double, lng: Double): RepoResult<HospitalListResponse> {
        return safeApiCall(
            call = { api.getHospitals(latitude = lat, longitude = lng) },
            mapper = { it }
        )
    }

    suspend fun getHospitalCases(
        hospitalId: String?,
        hospitalName: String?
    ): RepoResult<List<HospitalCase>> {
        return safeApiCall(
            call = { api.getHospitalCases(hospitalId = hospitalId, hospitalName = hospitalName) },
            mapper = { it.cases }
        )
    }

    suspend fun updateHospitalCaseStatus(request: UpdateCaseStatusRequest): RepoResult<Unit> {
        return safeApiCall(
            call = { api.updateHospitalCaseStatus(request) },
            mapper = { Unit }
        )
    }

    // ─── POLICE ───

    suspend fun getPoliceAlerts(
        stationId: String?,
        stationName: String?
    ): RepoResult<List<PoliceAlert>> {
        return safeApiCall(
            call = { api.getPoliceAlerts(stationId = stationId, stationName = stationName) },
            mapper = { it.alerts }
        )
    }

    suspend fun analyzeRoute(request: GroqRequest): RepoResult<GroqResponse> {
        return safeApiCall(
            call = { api.analyzeRoute(request) },
            mapper = { it }
        )
    }

    // ─── UTILS ───

    private suspend fun <TBody, TData> safeApiCall(
        call: suspend () -> Response<TBody>,
        mapper: (TBody) -> TData
    ): RepoResult<TData> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    RepoResult.Success(mapper(body))
                } else {
                    RepoResult.Error("Empty response body", response.code())
                }
            } else {
                val message = response.errorBody()?.string()?.take(200) ?: "API request failed"
                Log.w(TAG, "API failure ${response.code()}: $message")
                RepoResult.Error(message = message, code = response.code())
            }
        } catch (error: Exception) {
            Log.e(TAG, "API exception", error)
            RepoResult.Error(message = error.message ?: "Network error", throwable = error)
        }
    }
}
