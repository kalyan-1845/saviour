package com.sarathi.emergency.data.api

import com.sarathi.emergency.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface SarathiApi {

    // ══════════════════════════════════════
    //  AUTH
    // ══════════════════════════════════════

    @POST("api/auth/driver-login")
    suspend fun driverLogin(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/driver-register")
    suspend fun driverRegister(@Body request: RegisterRequest): Response<RegisterResponse>

    // ══════════════════════════════════════
    //  EMERGENCY / SOS
    // ══════════════════════════════════════

    @POST("api/sos")
    suspend fun sendSos(@Body request: SosRequest): Response<SosResponse>

    @GET("api/emergency/track")
    suspend fun trackSos(
        @Query("phone") phone: String? = null,
        @Query("tripId") tripId: String? = null
    ): Response<TrackResponse>

    // ══════════════════════════════════════
    //  DRIVER
    // ══════════════════════════════════════

    @GET("api/driver/trips")
    suspend fun getAssignedTrip(
        @Query("driverId") driverId: String? = null,
        @Query("email") email: String? = null
    ): Response<AssignedTripResponse>

    @POST("api/driver/update")
    suspend fun updateDriver(@Body request: DriverUpdateRequest): Response<DriverUpdateResponse>

    @POST("api/driver/notify")
    suspend fun notifyAuthorities(@Body request: NotifyRequest): Response<NotifyResponse>

    @POST("api/driver/update")
    suspend fun updateDriverLocation(@Body request: DriverLocationRequest): Response<GenericResponse>

    // ══════════════════════════════════════
    //  HOSPITALS
    // ══════════════════════════════════════

    @GET("api/hospitals")
    suspend fun getHospitals(
        @Query("specialization") specialization: String? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null
    ): Response<HospitalListResponse>

    @GET("api/hospital/cases")
    suspend fun getHospitalCases(
        @Query("hospitalId") hospitalId: String? = null,
        @Query("hospitalName") hospitalName: String? = null
    ): Response<HospitalCaseResponse>

    @POST("api/hospital/update")
    suspend fun updateHospitalCaseStatus(@Body request: UpdateCaseStatusRequest): Response<UpdateCaseStatusResponse>

    // ══════════════════════════════════════
    //  POLICE
    // ══════════════════════════════════════

    @GET("api/police/alerts")
    suspend fun getPoliceAlerts(
        @Query("stationId") stationId: String? = null,
        @Query("stationName") stationName: String? = null
    ): Response<PoliceAlertResponse>

    // ══════════════════════════════════════
    //  AI ANALYSIS
    // ══════════════════════════════════════

    @POST("api/groq")
    suspend fun analyzeRoute(@Body request: GroqRequest): Response<GroqResponse>
}
