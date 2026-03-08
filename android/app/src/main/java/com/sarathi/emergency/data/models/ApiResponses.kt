package com.sarathi.emergency.data.models

// ─── Auth Requests ───
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val phone: String,
    val licenseNumber: String,
    val vehicleNumber: String,
    val password: String
)

// ─── Auth Responses ───
data class LoginResponse(
    val success: Boolean = false,
    val message: String = "",
    val driver: Driver? = null,
    val error: String? = null
)

data class RegisterResponse(
    val success: Boolean = false,
    val message: String = "",
    val driver: Driver? = null,
    val error: String? = null
)

// ─── SOS Request / Response ───
data class SosRequest(
    val phone: String,
    val latitude: Double,
    val longitude: Double,
    val emergencyType: String = "medical"
)

data class SosResponse(
    val success: Boolean = false,
    val tripId: String = "",
    val status: String = "",
    val etaMinutes: Int = 0,
    val message: String? = null,
    val hospital: SosHospital? = null,
    val policeStation: SosPoliceStation? = null,
    val driver: SosDriver? = null,
    val error: String? = null
)

data class SosHospital(
    val name: String = "",
    val distanceKm: Double = 0.0,
    val phone: String = "",
    val mapUrl: String? = null
)

data class SosPoliceStation(
    val name: String = "",
    val distanceKm: Double = 0.0,
    val phone: String = "",
    val mapUrl: String? = null
)

data class SosDriver(
    val id: String? = null,
    val fullName: String = "",
    val phone: String = "",
    val vehicleNumber: String = ""
)

// ─── Track Response ───
data class TrackResponse(
    val success: Boolean = false,
    val message: String = "",
    val trip: TrackTrip? = null,
    val driver: TrackDriver? = null,
    val error: String? = null
)

data class TrackTrip(
    val id: String = "",
    val status: String = "",
    val emergencyType: String = "",
    val estimatedTime: Int? = null,
    val hospitalName: String? = null,
    val policeStationName: String? = null,
    val pickupMapUrl: String? = null,
    val hospitalMapUrl: String? = null
)

data class TrackDriver(
    val fullName: String = "",
    val phone: String = "",
    val vehicleNumber: String = "",
    val currentLocation: TrackDriverLocation? = null
)

data class TrackDriverLocation(
    val mapUrl: String? = null
)

// ─── Hospital API ───
data class HospitalListResponse(
    val success: Boolean = false,
    val hospitals: List<Hospital> = emptyList(),
    val error: String? = null
)

// ─── Driver assigned trip ───
data class AssignedTripResponse(
    val success: Boolean = false,
    val trip: AssignedTrip? = null,
    val error: String? = null
)

data class AssignedTrip(
    val id: String = "",
    val status: String = "",
    val emergencyType: String = "",
    val hospitalCaseStatus: String? = null,
    val estimatedTime: Int? = null,
    val user: AssignedTripUser? = null,
    val fullName: String? = null,
    val phone: String? = null,
    val pickupLocation: TripLocation? = null,
    val dropoffLocation: TripLocation? = null,
    val hospitalName: String? = null,
    val policeStationName: String? = null,
    val mapUrl: String? = null,
    val hospitalMapUrl: String? = null
)

data class AssignedTripUser(
    val fullName: String? = null,
    val phone: String? = null
)

// ─── Emergency select ───
data class EmergencySelectRequest(
    val driverId: String,
    val emergencyType: String,
    val latitude: Double,
    val longitude: Double
)

data class EmergencySelectResponse(
    val success: Boolean = false,
    val trip: EmergencyTrip? = null,
    val message: String? = null,
    val error: String? = null
)

// ─── Notify ───
data class NotifyRequest(
    val tripId: String,
    val driverId: String,
    val latitude: Double,
    val longitude: Double
)

data class NotifyResponse(
    val success: Boolean = false,
    val hospital: NotifyHospital? = null,
    val police: NotifyPolice? = null,
    val live: NotifyLive? = null,
    val error: String? = null
)

data class NotifyHospital(
    val name: String = "",
    val message: String = ""
)

data class NotifyPolice(
    val stations: List<NotifyStation> = emptyList(),
    val message: String = ""
)

data class NotifyStation(
    val name: String = "",
    val phone: String = "",
    val jurisdiction: String = ""
)

data class NotifyLive(
    val driverLocationMap: String = "",
    val etaMinutes: Int? = null
)

// ─── Location Update ───
data class DriverLocationRequest(
    val driverId: String,
    val latitude: Double,
    val longitude: Double,
    val status: String = "active"
)

data class GenericResponse(
    val success: Boolean = false,
    val message: String? = null
)

// ─── Generic Error ───
data class ApiError(
    val error: String = ""
)
