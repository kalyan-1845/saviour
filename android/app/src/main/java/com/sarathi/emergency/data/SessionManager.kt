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
    }

    fun saveDriverSession(driver: Driver) {
        prefs.edit()
            .putString(KEY_DRIVER, gson.toJson(driver))
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_DRIVER_ID, driver._id)
            .apply()
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
}
