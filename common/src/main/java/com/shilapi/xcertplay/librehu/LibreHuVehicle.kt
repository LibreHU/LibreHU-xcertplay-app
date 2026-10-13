package com.shilapi.xcertplay.librehu

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * The car as LibreHU-service describes it (`content://org.librehu.service.vehicle/info`, signature permission
 * `org.librehu.permission.VEHICLE`): its name (shown on the iPhone), make and model, driver's side, and the head
 * unit's real Bluetooth and Wi-Fi addresses, which Android hides from ordinary apps. Read off the main thread at start
 * and on every change; [current] is null without the service, and the app then keeps its own settings.
 */
object LibreHuVehicle {
    private const val TAG = "LibreHU-Vehicle"
    private val URI: Uri = Uri.parse("content://org.librehu.service.vehicle/info")
    private val MAC = Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")

    data class Info(
        val name: String,
        val make: String,
        val model: String,
        /** left or right. */
        val driverSide: String,
        val bluetoothMac: String,
        val wifiMac: String,
    ) {
        val rightHandDrive: Boolean get() = driverSide.equals("right", ignoreCase = true)
    }

    @Volatile
    var current: Info? = null
        private set

    private var observing = false

    fun start(context: Context) {
        val app = context.applicationContext
        Thread({ refresh(app) }, "librehu-vehicle").start()
        if (observing || app.packageManager.resolveContentProvider(URI.authority!!, 0) == null) return
        observing = true
        try {
            app.contentResolver.registerContentObserver(
                URI,
                false,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        Thread({ refresh(app) }, "librehu-vehicle").start()
                    }
                },
            )
        } catch (error: SecurityException) {
            Log.w(TAG, "observer: ${error.message}")
        }
    }

    private fun refresh(context: Context) {
        current = try {
            context.contentResolver.query(URI, null, null, null, null)?.use { c ->
                if (!c.moveToFirst()) return@use null
                fun col(name: String) = c.getColumnIndex(name).takeIf { it >= 0 }?.let { c.getString(it) }.orEmpty().trim()
                Info(
                    name = col("name"),
                    make = col("make"),
                    model = col("model"),
                    driverSide = col("driver_side"),
                    bluetoothMac = col("bt_mac").takeIf { MAC.matches(it) }.orEmpty(),
                    wifiMac = col("wifi_mac").takeIf { MAC.matches(it) }.orEmpty(),
                )
            }
        } catch (error: Exception) {
            // No service, or one signed with another key.
            Log.i(TAG, "read: ${error.message}")
            null
        }
    }
}
