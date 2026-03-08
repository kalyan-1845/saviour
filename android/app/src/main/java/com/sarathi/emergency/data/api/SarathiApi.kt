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

    @POST("api/emergency/sos")
    suspend fun sendSos(@Body request: SosRequest): Response<SosResponse>

    @GET("api/emergency/track")
    suspend fun trackSos(
        @Query("phone") phone: String? = null,
        @Query("tripId") tripId: String? = null
    ): Response<TrackResponse>

    // ══════════════════════════════════════
    //  DRIVER
    // ══════════════════════════════════════

    @GET("api/driver/assigned-trip")
    suspend fun getAssignedTrip(
        @Query("driverId") driverId: String
    ): Response<AssignedTripResponse>

    @POST("api/driver/select-emergency")
    suspend fun selectEmergency(@Body request: EmergencySelectRequest): Response<EmergencySelectResponse>

    @POST("api/driver/notify")
    suspend fun notifyAuthorities(@Body request: NotifyRequest): Response<NotifyResponse>

    @POST("api/driver/update-location")
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
}
