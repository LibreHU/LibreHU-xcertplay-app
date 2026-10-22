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
 * unit's real Bluetooth and Wi-Fi addresses, which Android hides from ordinary apps. Read at start (waited for, up to
 * 1.5 s, so that the first session has the name) and on every change; [current] is null without the service, and the app then keeps its own settings. The last
 * description read is kept (preferences) and given at once by [start]: the session settings built right after the
 * start already carry the car's name, before the provider answers.
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
        /** MediaTek decoder low-latency mode (LibreHU-service, CarPlay page; on when the service does not say). */
        val videoLowLatency: Boolean = true,
    ) {
        val rightHandDrive: Boolean get() = driverSide.equals("right", ignoreCase = true)
    }

    @Volatile
    var current: Info? = null
        private set

    private var observing = false
    private const val PREFS = "librehu_vehicle"
    private const val SYNC_READ_MS = 1500L

    fun start(context: Context) {
        val app = context.applicationContext
        if (current == null) current = load(app)
        apply(current)
        // Read now, before the session settings are built: done in the background, the first session after the
        // car was named (LibreHU Launcher's first setup, LibreHU-service) still went out as "xcertplay". A local
        // provider query, a few milliseconds; the background refresh stays for a slow start of the service.
        val t = Thread({ refresh(app) }, "librehu-vehicle")
        t.start()
        runCatching { t.join(SYNC_READ_MS) }
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
        val read = try {
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
                    videoLowLatency = col("carplay_video_low_latency") != "0",
                )
            }
        } catch (error: Exception) {
            // No service, or one signed with another key: the last description stays.
            Log.i(TAG, "read: ${error.message}")
            return
        }
        current = read
        apply(read)
        save(context, read)
    }

    private fun load(context: Context): Info? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = p.getString("name", null) ?: return null
        return Info(
            name = name,
            make = p.getString("make", "").orEmpty(),
            model = p.getString("model", "").orEmpty(),
            driverSide = p.getString("driver_side", "").orEmpty(),
            bluetoothMac = p.getString("bt_mac", "").orEmpty(),
            wifiMac = p.getString("wifi_mac", "").orEmpty(),
            videoLowLatency = p.getBoolean("video_low_latency", true),
        )
    }

    /** What the description changes in the media pipeline. */
    private fun apply(info: Info?) {
        com.shilapi.xcertplay.media.AndroidMediaSink.vendorLowLatency = info?.videoLowLatency ?: true
    }

    private fun save(
        context: Context,
        info: Info?,
    ) {
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (info == null) {
            e.clear()
        } else {
            e
                .putString("name", info.name)
                .putString("make", info.make)
                .putString("model", info.model)
                .putString("driver_side", info.driverSide)
                .putString("bt_mac", info.bluetoothMac)
                .putString("wifi_mac", info.wifiMac)
                .putBoolean("video_low_latency", info.videoLowLatency)
        }
        e.apply()
    }
}
