package com.sarathi.emergency.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.sarathi.emergency.data.models.Driver

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sarathi_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_DRIVER = "current_driver"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_DRIVER_ID = "driver_id"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_HOSPITAL_ID = "hospital_id"
        private const val KEY_POLICE_STATION_ID = "police_station_id"
        private const val KEY_SIMULATED_SOS_ID = "sim_sos_id"
        private const val KEY_SIMULATED_SOS_TYPE = "sim_sos_type"
        private const val KEY_SIMULATED_SOS_STATUS = "sim_sos_status"
        private const val KEY_SIMULATED_SOS_LAT = "sim_sos_lat"
        private const val KEY_SIMULATED_SOS_LNG = "sim_sos_lng"
        private const val KEY_SIMULATED_ETA = "sim_sos_eta"
    }

    fun saveDriverSession(driver: Driver) {
        prefs.edit()
            .putString(KEY_DRIVER, gson.toJson(driver))
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_DRIVER_ID, driver._id)
            .apply()
    }

    fun saveAuthToken(token: String?) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token ?: "")
            .apply()
    }

    fun getAuthToken(): String {
        return prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
    }

    fun saveHospitalId(hospitalId: String) {
        prefs.edit().putString(KEY_HOSPITAL_ID, hospitalId).apply()
    }

    fun getHospitalId(): String {
        return prefs.getString(KEY_HOSPITAL_ID, "") ?: ""
    }

    fun savePoliceStationId(stationId: String) {
        prefs.edit().putString(KEY_POLICE_STATION_ID, stationId).apply()
    }

    fun getPoliceStationId(): String {
        return prefs.getString(KEY_POLICE_STATION_ID, "") ?: ""
    }

    fun getDriver(): Driver? {
        val json = prefs.getString(KEY_DRIVER, null) ?: return null
        return try {
            gson.fromJson(json, Driver::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getDriverId(): String {
        return prefs.getString(KEY_DRIVER_ID, "") ?: ""
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    /**
     * Store entire simulated mission state for perfectly synchronized demos across all dashboards.
     */
    fun saveSimulatedMission(id: String, type: String, status: String, lat: Double, lng: Double, eta: Int = 5) {
        prefs.edit()
            .putString(KEY_SIMULATED_SOS_ID, id)
            .putString(KEY_SIMULATED_SOS_TYPE, type)
            .putString(KEY_SIMULATED_SOS_STATUS, status)
            .putFloat(KEY_SIMULATED_SOS_LAT, lat.toFloat())
            .putFloat(KEY_SIMULATED_SOS_LNG, lng.toFloat())
            .putInt(KEY_SIMULATED_ETA, eta)
            .apply()
    }

    fun getSimulatedSOS(): String? = prefs.getString(KEY_SIMULATED_SOS_ID, null)
    fun getSimulatedSOSType(): String = prefs.getString(KEY_SIMULATED_SOS_TYPE, "medical") ?: "medical"
    fun getSimulatedSOSStatus(): String = prefs.getString(KEY_SIMULATED_SOS_STATUS, "pending") ?: "pending"
    fun getSimulatedSOSLat(): Double = prefs.getFloat(KEY_SIMULATED_SOS_LAT, 0f).toDouble()
    fun getSimulatedSOSLng(): Double = prefs.getFloat(KEY_SIMULATED_SOS_LNG, 0f).toDouble()
    fun getSimulatedETA(): Int = prefs.getInt(KEY_SIMULATED_ETA, 5)

    fun updateSimulatedMissionStatus(status: String) {
        prefs.edit().putString(KEY_SIMULATED_SOS_STATUS, status).apply()
    }

    fun clearSimulatedSOS() {
        prefs.edit()
            .remove(KEY_SIMULATED_SOS_ID)
            .remove(KEY_SIMULATED_SOS_TYPE)
            .remove(KEY_SIMULATED_SOS_STATUS)
            .remove(KEY_SIMULATED_SOS_LAT)
            .remove(KEY_SIMULATED_SOS_LNG)
            .remove(KEY_SIMULATED_ETA)
            .apply()
    }
}
