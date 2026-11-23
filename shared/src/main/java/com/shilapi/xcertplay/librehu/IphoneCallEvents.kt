package com.shilapi.xcertplay.librehu

/**
 * LibreHU: a new call of the iPhone (iAP2 CallStateUpdate), seen by the controller and handed by the app module to
 * LibreHU-service (front LED blink). Nothing about the call (number, name) goes with it.
 */
object IphoneCallEvents {
    @Volatile
    var listener: (() -> Unit)? = null

    fun newCall() {
        listener?.invoke()
    }
}
