package com.sarathi.emergency.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.sarathi.emergency.BuildConfig

object ExternalActionHandler {
    private const val TAG = "ExternalActionHandler"

    private fun launchIntent(context: Context, intent: Intent, blockedMessage: String): Boolean {
        if (!BuildConfig.ALLOW_EXTERNAL_APPS) {
            Toast.makeText(context, blockedMessage, Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Blocked external action: ${intent.action}, data=${intent.data}")
            return false
        }

        return try {
            val canHandle = intent.resolveActivity(context.packageManager) != null
            if (!canHandle) {
                Toast.makeText(context, "No compatible app found", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "No handler for action=${intent.action}, data=${intent.data}")
                false
            } else {
                context.startActivity(intent)
                true
            }
        } catch (error: Exception) {
            Toast.makeText(context, "Unable to open external app", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "External action failed", error)
            false
        }
    }

    fun openUrl(context: Context, url: String, blockedMessage: String = "External browser disabled in app mode"): Boolean {
        return launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)), blockedMessage)
    }

    fun dial(context: Context, phone: String, blockedMessage: String = "External dialer disabled in app mode"): Boolean {
        return launchIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")), blockedMessage)
    }

    fun sendSms(context: Context, body: String, blockedMessage: String = "External SMS app disabled in app mode"): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", body)
        }
        return launchIntent(context, intent, blockedMessage)
    }

    fun openNavigation(context: Context, latitude: Double, longitude: Double): Boolean {
        val navIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$latitude,$longitude&mode=d"))
            .apply { setPackage("com.google.android.apps.maps") }

        val openedGoogleMaps = launchIntent(
            context,
            navIntent,
            blockedMessage = "External navigation disabled in app mode"
        )
        if (openedGoogleMaps) return true

        return openUrl(
            context,
            "https://maps.google.com/maps?daddr=$latitude,$longitude",
            blockedMessage = "External navigation disabled in app mode"
        )
    }
}
