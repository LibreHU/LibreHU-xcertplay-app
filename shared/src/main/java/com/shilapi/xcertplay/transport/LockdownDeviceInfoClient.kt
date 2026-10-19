package com.shilapi.xcertplay.transport

/**
 * LibreHU: what the paired iPhone tells about itself, through the Lockdown session the wired CarPlay link already
 * pairs (same requests as libimobiledevice / pymobiledevice3):
 * - `GetValue` (all keys): name, model identifier, iOS version, Bluetooth address;
 * - `GetValue` in the `com.apple.mobile.battery` domain: charge level, charging, external power;
 * - the `com.apple.mobile.diagnostics_relay` service, `IORegistry` request for the `IOPMPowerSource` class
 *   (pymobiledevice3 `DiagnosticsService.get_battery`): cycle count, temperature, capacities, voltage, current.
 * Each part is optional: what the phone refuses is left out. Blocking; runs off the main thread.
 */
class LockdownDeviceInfoClient(
    private val host: Iap2UsbMuxHost,
) {
    fun read(
        pairRecord: LockdownPairRecord,
        label: String,
    ): IphoneDeviceInfo {
        val lockdownConnection = host.connect(Iap2UsbMuxHost.LOCKDOWN_PORT, STEP_TIMEOUT_MILLIS)
        val secure =
            LockdownPlistChannel(lockdownConnection).use { plaintext ->
                val response =
                    plaintext.request(
                        dict(
                            "Label" to text(label),
                            "Request" to text("StartSession"),
                            "HostID" to text(pairRecord.hostId),
                            "SystemBUID" to text(pairRecord.systemBuid),
                        ),
                        STEP_TIMEOUT_MILLIS,
                    )
                if (response.entries["Error"] != null) throw IphoneUsbException.Protocol("Lockdown StartSession refused")
                val ssl = (response.entries["EnableSessionSSL"] as? LockdownPlistValue.Boolean)?.value ?: false
                val stream =
                    if (ssl) {
                        TlsDuplexChannel.open(plaintext.detach(), pairRecord, STEP_TIMEOUT_MILLIS)
                    } else {
                        plaintext.detach()
                    }
                LockdownPlistChannel(stream)
            }
        secure.use { lockdown ->
            val all = getValue(lockdown, label, null, null)
            val battery = getValue(lockdown, label, BATTERY_DOMAIN, null)
            val power = runCatching { powerSource(lockdown, pairRecord, label) }.getOrNull()
            return IphoneDeviceInfo(
                name = all?.string("DeviceName"),
                productType = all?.string("ProductType"),
                iosVersion = all?.string("ProductVersion"),
                buildVersion = all?.string("BuildVersion"),
                bluetoothAddress = all?.string("BluetoothAddress"),
                batteryLevel = battery?.long("BatteryCurrentCapacity")?.toInt() ?: power?.long("CurrentCapacity")?.toInt(),
                charging = battery?.bool("BatteryIsCharging") ?: power?.bool("IsCharging"),
                externalPower = battery?.bool("ExternalConnected") ?: power?.bool("ExternalConnected"),
                cycleCount = power?.long("CycleCount")?.toInt(),
                // IOKit reports hundredths of a degree Celsius.
                temperatureC = power?.number("Temperature")?.let { it / 100.0 },
                designCapacityMah = power?.long("DesignCapacity")?.toInt(),
                maxCapacityMah = (power?.long("AppleRawMaxCapacity") ?: power?.long("NominalChargeCapacity"))?.toInt(),
                voltageMv = (power?.long("AppleRawBatteryVoltage") ?: power?.long("Voltage"))?.toInt(),
                currentMa = (power?.long("InstantAmperage") ?: power?.long("Amperage"))?.toInt(),
            )
        }
    }

    private fun getValue(
        lockdown: LockdownPlistChannel,
        label: String,
        domain: String?,
        key: String?,
    ): LockdownPlistValue.Dictionary? {
        val entries = linkedMapOf<String, LockdownPlistValue>("Label" to text(label), "Request" to text("GetValue"))
        if (domain != null) entries["Domain"] = text(domain)
        if (key != null) entries["Key"] = text(key)
        val response = runCatching { lockdown.request(LockdownPlistValue.Dictionary(entries), STEP_TIMEOUT_MILLIS) }.getOrNull()
        return response?.entries?.get("Value") as? LockdownPlistValue.Dictionary
    }

    /** `IORegistry` of the `IOPMPowerSource` class through the diagnostics relay. */
    private fun powerSource(
        lockdown: LockdownPlistChannel,
        pairRecord: LockdownPairRecord,
        label: String,
    ): LockdownPlistValue.Dictionary? {
        val started =
            lockdown.request(
                dict("Label" to text(label), "Request" to text("StartService"), "Service" to text(DIAGNOSTICS_SERVICE)),
                STEP_TIMEOUT_MILLIS,
            )
        val port = (started.entries["Port"] as? LockdownPlistValue.Integer)?.value?.toInt() ?: return null
        if (port !in 1..0xffff) return null
        val ssl = (started.entries["EnableServiceSSL"] as? LockdownPlistValue.Boolean)?.value ?: false
        val connection = host.connect(port, STEP_TIMEOUT_MILLIS)
        val stream = if (ssl) TlsDuplexChannel.open(connection, pairRecord, STEP_TIMEOUT_MILLIS) else connection
        LockdownPlistChannel(stream).use { relay ->
            val response =
                relay.request(dict("Request" to text("IORegistry"), "EntryClass" to text("IOPMPowerSource")), STEP_TIMEOUT_MILLIS)
            runCatching { relay.send(dict("Request" to text("Goodbye"))) }
            val diagnostics = response.entries["Diagnostics"] as? LockdownPlistValue.Dictionary ?: return null
            return diagnostics.entries["IORegistry"] as? LockdownPlistValue.Dictionary
        }
    }

    private fun LockdownPlistValue.Dictionary.string(key: String) = (entries[key] as? LockdownPlistValue.Text)?.value

    private fun LockdownPlistValue.Dictionary.long(key: String) =
        when (val v = entries[key]) {
            is LockdownPlistValue.Integer -> v.value
            is LockdownPlistValue.Real -> v.value.toLong()
            else -> null
        }

    private fun LockdownPlistValue.Dictionary.number(key: String) =
        when (val v = entries[key]) {
            is LockdownPlistValue.Integer -> v.value.toDouble()
            is LockdownPlistValue.Real -> v.value
            else -> null
        }

    private fun LockdownPlistValue.Dictionary.bool(key: String) = (entries[key] as? LockdownPlistValue.Boolean)?.value

    private fun dict(vararg entries: Pair<String, LockdownPlistValue>) = LockdownPlistValue.Dictionary(linkedMapOf(*entries))

    private fun text(value: String) = LockdownPlistValue.Text(value)

    private companion object {
        const val STEP_TIMEOUT_MILLIS = 5_000L
        const val BATTERY_DOMAIN = "com.apple.mobile.battery"
        const val DIAGNOSTICS_SERVICE = "com.apple.mobile.diagnostics_relay"
    }
}

/** LibreHU: the iPhone's details; null = not given by the phone. */
data class IphoneDeviceInfo(
    val name: String? = null,
    /** Apple model identifier, e.g. iPhone15,2. */
    val productType: String? = null,
    val iosVersion: String? = null,
    val buildVersion: String? = null,
    val bluetoothAddress: String? = null,
    /** Percent. */
    val batteryLevel: Int? = null,
    val charging: Boolean? = null,
    val externalPower: Boolean? = null,
    val cycleCount: Int? = null,
    val temperatureC: Double? = null,
    val designCapacityMah: Int? = null,
    val maxCapacityMah: Int? = null,
    val voltageMv: Int? = null,
    val currentMa: Int? = null,
    /** True over USB (wired CarPlay), false over Bluetooth / Wi-Fi (wireless CarPlay). */
    val wired: Boolean? = null,
)
