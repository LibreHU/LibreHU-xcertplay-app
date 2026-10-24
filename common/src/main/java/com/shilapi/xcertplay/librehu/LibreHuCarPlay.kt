package com.shilapi.xcertplay.librehu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * LibreHU fork: CarPlay with the other LibreHU apps.
 *
 * - **Theme**: light / dark and accent of LibreHU Launcher (`content://org.librehu.launcher.theme/theme`, broadcast
 *   `org.librehu.action.THEME_CHANGED`) for the app's screens ([theme]).
 * - **Day / night of CarPlay**: LibreHU-service's choice from the headlights ([ACTION_SET_NIGHT_MODE], `state` =
 *   night / day / auto), else the launcher's dark mode, else Android's ([night]).
 * - **Session state** for LibreHU-service and the launcher ([ACTION_SESSION_STATE], `state` = connecting / active /
 *   ended / failed, `detail`).
 * - **The iPhone's details** for LibreHU-service only ([ACTION_PHONE_INFO]): name, model, iOS, Bluetooth address,
 *   battery level / charging / external power, cycle count, temperature, capacities, voltage, current (Lockdown and
 *   iAP2, see `IphoneInfoBus`), `wired` (USB, else wireless); `connected` = false when the iPhone is gone.
 *
 * The service's commands are accepted only from apps holding the signature permission
 * `<applicationId>.permission.CONFIGURE` (signed with the LibreHU key).
 */
object LibreHuCarPlay {
    private const val TAG = "LibreHU-CarPlay"
    private val THEME_URI: Uri = Uri.parse("content://org.librehu.launcher.theme/theme")
    private const val THEME_CHANGED = "org.librehu.action.THEME_CHANGED"
    const val ACTION_SET_NIGHT_MODE = "org.librehu.carplay.action.SET_NIGHT_MODE"
    const val ACTION_SESSION_STATE = "org.librehu.carplay.SESSION_STATE"
    const val ACTION_PHONE_INFO = "org.librehu.carplay.PHONE_INFO"
    private const val SERVICE_PACKAGE = "org.librehu.service"

    /**
     * Launcher theme: dark and accent (ARGB); null without LibreHU Launcher. [retro]: the launcher's retro palette
     * (VFD amber, green LCD, cassette: background, surface, surface high, accent, on accent, text, text dim), null for
     * its modern look; the screens then take those colours and the segment display font.
     */
    data class LauncherTheme(
        val dark: Boolean,
        val accent: Int,
        val retro: List<Int>? = null,
    )

    private val RETRO_COLORS = listOf("background", "surface", "surface_high", "accent", "on_accent", "text", "text_dim")

    private val _theme = MutableStateFlow<LauncherTheme?>(null)
    val theme: StateFlow<LauncherTheme?> = _theme.asStateFlow()

    /** Night from LibreHU-service (headlights); null = not decided by the service. */
    private val _serviceNight = MutableStateFlow<Boolean?>(null)
    val serviceNight: StateFlow<Boolean?> = _serviceNight.asStateFlow()

    private var started = false

    /** CarPlay's day / night: the service's, else the launcher's, else Android's ([systemDark]). */
    fun night(systemDark: Boolean): Boolean = _serviceNight.value ?: _theme.value?.dark ?: systemDark

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        val main = Handler(Looper.getMainLooper())
        IphoneInfoBus.listener = { info -> reportPhone(app, info) }
        if (app.packageManager.resolveContentProvider(THEME_URI.authority!!, 0) != null) {
            Thread({ readTheme(app) }, "librehu-theme").start()
            runCatching {
                app.contentResolver.registerContentObserver(
                    THEME_URI,
                    false,
                    object : ContentObserver(main) {
                        override fun onChange(selfChange: Boolean) {
                            Thread({ readTheme(app) }, "librehu-theme").start()
                        }
                    },
                )
            }
            register(
                app,
                object : BroadcastReceiver() {
                    override fun onReceive(
                        c: Context,
                        intent: Intent,
                    ) {
                        if (intent.hasExtra("dark") && intent.hasExtra("accent")) {
                            val retro =
                                intent
                                    .getStringExtra("style")
                                    ?.takeIf { it.startsWith("retro") && RETRO_COLORS.all { k -> intent.hasExtra(k) } }
                                    ?.let { RETRO_COLORS.map { k -> intent.getIntExtra(k, 0) } }
                            _theme.value = LauncherTheme(intent.getBooleanExtra("dark", true), intent.getIntExtra("accent", 0), retro)
                        }
                    }
                },
                IntentFilter(THEME_CHANGED),
                null,
            )
        }
        register(
            app,
            object : BroadcastReceiver() {
                override fun onReceive(
                    c: Context,
                    intent: Intent,
                ) {
                    _serviceNight.value =
                        when (intent.getStringExtra("state")) {
                            "night" -> true
                            "day" -> false
                            else -> null
                        }
                    Log.i(TAG, "night from LibreHU-service: ${_serviceNight.value}")
                }
            },
            IntentFilter(ACTION_SET_NIGHT_MODE),
            "${app.packageName}.permission.CONFIGURE",
        )
    }

    /** The iPhone's details to LibreHU-service (its package only: name and Bluetooth address are personal). */
    private fun reportPhone(
        context: Context,
        info: com.shilapi.xcertplay.transport.IphoneDeviceInfo?,
    ) {
        val i = Intent(ACTION_PHONE_INFO).setPackage(SERVICE_PACKAGE).putExtra("connected", info != null)
        if (info != null) {
            info.name?.let { i.putExtra("name", it) }
            info.productType?.let { i.putExtra("model", it) }
            info.iosVersion?.let { i.putExtra("os", it) }
            info.buildVersion?.let { i.putExtra("build", it) }
            info.bluetoothAddress?.let { i.putExtra("bt", it) }
            info.batteryLevel?.let { i.putExtra("level", it) }
            info.charging?.let { i.putExtra("charging", it) }
            info.externalPower?.let { i.putExtra("external_power", it) }
            info.cycleCount?.let { i.putExtra("cycles", it) }
            info.temperatureC?.let { i.putExtra("temperature", it) }
            info.designCapacityMah?.let { i.putExtra("design_mah", it) }
            info.maxCapacityMah?.let { i.putExtra("max_mah", it) }
            info.voltageMv?.let { i.putExtra("voltage_mv", it) }
            info.currentMa?.let { i.putExtra("current_ma", it) }
            info.wired?.let { i.putExtra("wired", it) }
        }
        runCatching { context.sendBroadcast(i) }
    }

    /** Tells LibreHU-service and the launcher where the CarPlay session is. */
    fun reportSession(
        context: Context,
        state: String,
        detail: String = "",
    ) {
        runCatching {
            context.applicationContext.sendBroadcast(
                Intent(ACTION_SESSION_STATE).putExtra("state", state).putExtra("detail", detail.take(200)),
            )
        }
    }

    private fun readTheme(context: Context) {
        try {
            context.contentResolver.query(THEME_URI, null, null, null, null)?.use { c ->
                if (!c.moveToFirst()) return
                val dark = c.getColumnIndex("dark").takeIf { it >= 0 }?.let { c.getInt(it) != 0 } ?: true
                val accent = c.getColumnIndex("accent").takeIf { it >= 0 }?.let { c.getInt(it) } ?: return
                val retro =
                    c.getColumnIndex("style").takeIf { it >= 0 }?.let { c.getString(it) }?.takeIf { it.startsWith("retro") }?.let {
                        RETRO_COLORS.map { k -> c.getColumnIndex(k).takeIf { i -> i >= 0 }?.let { i -> c.getInt(i) } ?: return@let null }
                    }
                _theme.value = LauncherTheme(dark, accent, retro)
            }
        } catch (e: Exception) {
            Log.w(TAG, "theme: ${e.message}")
        }
    }

    private fun register(
        context: Context,
        receiver: BroadcastReceiver,
        filter: IntentFilter,
        permission: String?,
    ) {
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, permission, null, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter, permission, null)
        }
    }
}
