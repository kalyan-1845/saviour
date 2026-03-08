package com.sarathi.emergency.data.models

data class Driver(
    val _id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val licenseNumber: String = "",
    val vehicleNumber: String = "",
    val isAvailable: Boolean = true,
    val currentLocation: DriverLocation? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class DriverLocation(
    val type: String = "Point",
    val coordinates: List<Double> = emptyList()
)

data class Hospital(
    val _id: String = "",
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String = "",
    val email: String? = null,
    val specialties: List<String> = emptyList(),
    val bedsAvailable: Int = 0,
    val totalBeds: Int = 0,
    val type: String = "private",
    val isEmergencyAvailable: Boolean = true,
    val ambulanceCount: Int = 1,
    val rating: Double? = null,
    val city: String = "",
    val zone: String? = null,
    val distance: Double? = null
)

data class EmergencyTrip(
    val _id: String = "",
    val id: String = "",
    val driverId: String? = null,
    val userId: String? = null,
    val emergencyType: String = "",
    val pickupLocation: TripLocation? = null,
    val dropoffLocation: TripLocation? = null,
    val hospitalId: String? = null,
    val hospitalName: String? = null,
    val policeStationId: String? = null,
    val policeStationName: String? = null,
    val status: String = "pending",
    val hospitalCaseStatus: String? = null,
    val estimatedTime: Int? = null,
    val actualTime: Int? = null,
    val distance: Double? = null,
    val createdAt: String = "",
    val completedAt: String? = null
)

data class TripLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null
)
