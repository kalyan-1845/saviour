package com.sarathi.emergency.data.repository

import android.util.Log
import com.sarathi.emergency.data.api.SarathiApi
import com.sarathi.emergency.data.models.HospitalCase
import com.sarathi.emergency.data.models.PoliceAlert
import com.sarathi.emergency.data.models.UpdateCaseStatusRequest
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

    suspend fun getPoliceAlerts(
        stationId: String?,
        stationName: String?
    ): RepoResult<List<PoliceAlert>> {
        return safeApiCall(
            call = { api.getPoliceAlerts(stationId = stationId, stationName = stationName) },
            mapper = { it.alerts }
        )
    }

    private suspend fun <TBody, TData> safeApiCall(
        call: suspend () -> Response<TBody>,
        mapper: (TBody) -> TData
    ): RepoResult<TData> {
        return try {
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
