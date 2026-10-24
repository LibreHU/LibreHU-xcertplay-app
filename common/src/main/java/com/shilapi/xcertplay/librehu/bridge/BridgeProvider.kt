package com.shilapi.xcertplay.librehu.bridge

import android.content.Context
import com.shilapi.xcertplay.AirPlayPersistence
import com.shilapi.xcertplay.airplay.AirPlayDisplaySettings
import com.shilapi.xcertplay.airplay.AirPlayPhysicalSizeBasis
import com.shilapi.xcertplay.airplay.CarPlayDisplayScale
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.media.MainMediaAudioBuffer
import com.shilapi.xcertplay.media.MicrophoneGain
import com.shilapi.xcertplay.orchestration.ManualHotspotBand
import com.shilapi.xcertplay.orchestration.ManualHotspotSecurity
import com.shilapi.xcertplay.orchestration.MfiTarget
import com.shilapi.xcertplay.orchestration.WirelessHotspotMode

/**
 * CarPlay's settings for LibreHU's phone app (through LibreHU-service's bridge, [BridgeKitProvider]): picture (scale,
 * frame rate, HEVC, screen size, bars, right-hand drive), sound, wireless CarPlay and its hotspot, authentication
 * chip, car identity, start and logs. The same store as the app's settings ([AirPlayPersistence]); taken into account
 * at the next CarPlay session. Not here: secrets (remote MFi token, hotspot passphrase, local certificate files),
 * the safe area (drawn on the screen) and the AirPlay icon.
 */
class BridgeProvider : BridgeKitProvider() {
    override fun pages(context: Context): List<KitPage> {
        val p = AirPlayPersistence
        val c = context
        return listOf(
            page(c, "picture", c.getString(R.string.bridge_cp_picture), "carplay") {
                slider("scale", s(R.string.bridge_cp_scale), p.loadDisplayScaleTenths(c), CarPlayDisplayScale.MIN_TENTHS, CarPlayDisplayScale.MAX_TENTHS, display = {
                    CarPlayDisplayScale.label(it)
                }) { p.saveDisplayScaleTenths(c, it) }
                slider("fps", s(R.string.bridge_cp_fps), p.loadFps(c), AirPlayDisplaySettings.MIN_FPS, AirPlayDisplaySettings.MAX_FPS, AirPlayDisplaySettings.FPS_STEP, "i/s") {
                    p.saveFps(c, it)
                }
                switch("hevc", "HEVC (H.265)", p.loadHevcEnabled(c)) { p.saveHevcEnabled(c, it) }
                switch("hevc_sw", s(R.string.bridge_cp_hevc_sw), p.loadHevcSoftwareDecoderEnabled(c)) { p.saveHevcSoftwareDecoderEnabled(c, it) }
                slider(
                    "width_mm",
                    s(R.string.bridge_cp_width),
                    p.loadWidthPhysicalMm(c),
                    AirPlayDisplaySettings.MIN_WIDTH_PHYSICAL_MM,
                    AirPlayDisplaySettings.MAX_WIDTH_PHYSICAL_MM,
                    AirPlayDisplaySettings.WIDTH_PHYSICAL_MM_STEP,
                    "mm",
                ) { p.saveWidthPhysicalMm(c, it) }
                pick("size_basis", s(R.string.bridge_cp_size_basis), p.loadPhysicalSizeBasis(c), label = {
                    s(if (it == AirPlayPhysicalSizeBasis.WIDTH) R.string.bridge_cp_width_basis else R.string.bridge_cp_height_basis)
                }) { p.savePhysicalSizeBasis(c, it) }
                switch("rhd", s(R.string.bridge_cp_rhd), p.loadRightHandDrive(c)) { p.saveRightHandDrive(c, it) }
                switch("hide_top", s(R.string.bridge_cp_hide_top), p.loadHideTopBar(c)) { p.saveHideTopBar(c, it) }
                switch("hide_bottom", s(R.string.bridge_cp_hide_bottom), p.loadHideBottomBar(c)) { p.saveHideBottomBar(c, it) }
                switch("safe_outside", s(R.string.bridge_cp_safe_outside), p.loadSafeAreaDrawOutside(c)) { p.saveSafeAreaDrawOutside(c, it) }
                switch("gestures", s(R.string.bridge_cp_gestures), p.loadMoreGesturesToSettings(c)) { p.saveMoreGesturesToSettings(c, it) }
            },
            page(c, "sound", c.getString(R.string.bridge_cp_sound), "carplay") {
                slider("buffer", s(R.string.bridge_cp_buffer), p.loadMainMediaAudioBufferDurationMs(c), MainMediaAudioBuffer.MIN_DURATION_MS, MainMediaAudioBuffer.MAX_DURATION_MS, 50, "ms") {
                    p.saveMainMediaAudioBufferDurationMs(c, it)
                }
                slider("mic", s(R.string.bridge_cp_mic), p.loadMicrophoneGainPercent(c), MicrophoneGain.MIN_PERCENT, MicrophoneGain.MAX_PERCENT, MicrophoneGain.STEP_PERCENT, "%") {
                    p.saveMicrophoneGainPercent(c, it)
                }
                switch("channels", s(R.string.bridge_cp_channels), p.loadAdvancedAudioChannelMapping(c)) { p.saveAdvancedAudioChannelMapping(c, it) }
            },
            page(c, "wireless", c.getString(R.string.bridge_cp_wireless), "wifi") {
                switch("wireless", s(R.string.bridge_cp_wireless), p.loadWirelessEnabled(c)) { p.saveWirelessEnabled(c, it) }
                pick("hotspot", s(R.string.bridge_cp_hotspot), p.loadWirelessHotspotMode(c), label = {
                    when (it) {
                        WirelessHotspotMode.WIFI_P2P -> "Wi-Fi Direct"
                        WirelessHotspotMode.LOCAL_ONLY_HOTSPOT -> s(R.string.bridge_cp_hotspot_local)
                        WirelessHotspotMode.MANUAL -> s(R.string.bridge_cp_hotspot_manual)
                    }
                }) { p.saveWirelessHotspotMode(c, it) }
                if (p.loadWirelessHotspotMode(c) == WirelessHotspotMode.MANUAL) {
                    text("ssid", "SSID", p.loadManualHotspotSsid(c), 32) { p.saveManualHotspotSsid(c, it) }
                    pick("band", s(R.string.bridge_cp_band), p.loadManualHotspotBand(c), label = {
                        when (it) {
                            ManualHotspotBand.AUTO -> s(R.string.bridge_cp_auto)
                            ManualHotspotBand.GHZ_2_4 -> "2,4 GHz"
                            ManualHotspotBand.GHZ_5 -> "5 GHz"
                        }
                    }) { p.saveManualHotspotBand(c, it) }
                    slider("channel", s(R.string.bridge_cp_channel), p.loadManualHotspotChannel(c), 0, 165, hint = s(R.string.bridge_cp_channel_hint)) {
                        p.saveManualHotspotChannel(c, it)
                    }
                    pick("security", s(R.string.bridge_cp_security), p.loadManualHotspotSecurity(c), label = {
                        when (it) {
                            ManualHotspotSecurity.OPEN -> s(R.string.bridge_cp_open)
                            ManualHotspotSecurity.WPA2 -> "WPA2"
                            ManualHotspotSecurity.WPA3_TRANSITION -> "WPA2 / WPA3"
                            ManualHotspotSecurity.WPA3 -> "WPA3"
                        }
                    }) { p.saveManualHotspotSecurity(c, it) }
                }
            },
            page(c, "system", c.getString(R.string.bridge_cp_system), "settings") {
                pick("mfi", s(R.string.bridge_cp_mfi), p.loadMfiTarget(c), label = {
                    when (it) {
                        MfiTarget.USB_CH341 -> "USB CH341"
                        MfiTarget.I2C -> "I²C"
                        MfiTarget.REMOTE -> s(R.string.bridge_cp_mfi_remote)
                        MfiTarget.LOCAL_FILES -> s(R.string.bridge_cp_mfi_files)
                    }
                }) { p.saveMfiTarget(c, it) }
                if (p.loadMfiTarget(c) == MfiTarget.REMOTE) text("mfi_server", s(R.string.bridge_cp_mfi_server), p.loadRemoteMfiServer(c), 200) { p.saveRemoteMfiServer(c, it.trim()) }
                section(s(R.string.bridge_cp_identity))
                text("manufacturer", s(R.string.bridge_cp_manufacturer), p.loadManufacturer(c), 40) { p.saveManufacturer(c, it) }
                text("model", s(R.string.bridge_cp_model), p.loadModel(c), 40) { p.saveModel(c, it) }
                text("oem_label", s(R.string.bridge_cp_oem), p.loadOemLabel(c), 40) { p.saveOemLabel(c, it) }
                section(s(R.string.bridge_cp_start))
                switch("boot", s(R.string.bridge_cp_boot), p.loadAutoStartOnBoot(c)) { p.saveAutoStartOnBoot(c, it) }
                switch("location", s(R.string.bridge_cp_location), p.loadLocationReportingEnabled(c)) { p.saveLocationReportingEnabled(c, it) }
                section(s(R.string.bridge_cp_logs))
                switch("debug_logs", s(R.string.bridge_cp_debug_logs), p.loadDebugLogsEnabled(c)) { p.saveDebugLogsEnabled(c, it) }
                switch("frame_logs", s(R.string.bridge_cp_frame_logs), p.loadVideoFrameLogsEnabled(c)) { p.saveVideoFrameLogsEnabled(c, it) }
                switch("metrics", s(R.string.bridge_cp_metrics), p.loadMediaMetricsEnabled(c)) { p.saveMediaMetricsEnabled(c, it) }
                switch("audio_capture", s(R.string.bridge_cp_audio_capture), p.loadAudioPacketCaptureEnabled(c)) { p.saveAudioPacketCaptureEnabled(c, it) }
            },
        )
    }
}
