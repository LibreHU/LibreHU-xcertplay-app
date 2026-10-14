package com.shilapi.xcertplay.librehu

import com.shilapi.xcertplay.transport.IphoneDeviceInfo

/**
 * LibreHU: the connected iPhone's details, filled by the wired controller (Lockdown queries, iAP2 notifications)
 * and read by the app module, which hands them to LibreHU-service. Null when no iPhone is connected.
 */
object IphoneInfoBus {
    @Volatile
    var current: IphoneDeviceInfo? = null
        private set

    @Volatile
    var listener: ((IphoneDeviceInfo?) -> Unit)? = null

    /** Merges [update] into what is known (null fields keep the previous value). */
    @Synchronized
    fun merge(update: IphoneDeviceInfo) {
        val old = current ?: IphoneDeviceInfo()
        val next =
            IphoneDeviceInfo(
                name = update.name ?: old.name,
                productType = update.productType ?: old.productType,
                iosVersion = update.iosVersion ?: old.iosVersion,
                buildVersion = update.buildVersion ?: old.buildVersion,
                bluetoothAddress = update.bluetoothAddress ?: old.bluetoothAddress,
                batteryLevel = update.batteryLevel ?: old.batteryLevel,
                charging = update.charging ?: old.charging,
                externalPower = update.externalPower ?: old.externalPower,
                cycleCount = update.cycleCount ?: old.cycleCount,
                temperatureC = update.temperatureC ?: old.temperatureC,
                designCapacityMah = update.designCapacityMah ?: old.designCapacityMah,
                maxCapacityMah = update.maxCapacityMah ?: old.maxCapacityMah,
                voltageMv = update.voltageMv ?: old.voltageMv,
                currentMa = update.currentMa ?: old.currentMa,
            )
        current = next
        listener?.invoke(next)
    }

    @Synchronized
    fun clear() {
        if (current == null) return
        current = null
        listener?.invoke(null)
    }
}
