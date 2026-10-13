package com.shilapi.xcertplay.orchestration

import com.shilapi.xcertplay.mfi.MfiAuthenticationClient
import com.shilapi.xcertplay.mfi.MfiAuthenticator
import com.shilapi.xcertplay.mfi.MfiDeviceScanner
import com.shilapi.xcertplay.transport.I2cTransport
import com.shilapi.xcertplay.transport.I2cTransportException
import com.shilapi.xcertplay.transport.LinuxI2cTransport
import java.io.File
import java.io.Closeable
import java.io.IOException

/** Signals the normal, retryable state where the MFi address probe found no chip. */
internal class MfiCoprocessorNotFoundException(
    message: String = "No MFi authentication coprocessor responded to the device probe",
) : IOException(message)

/** An opened MFi coprocessor client plus the handle that releases its backing transport. */
class MfiSession(
    val client: MfiAuthenticator,
    private val closeable: Closeable?,
) : Closeable {
    override fun close() {
        closeable?.close()
    }
}

/** Runs the documented address probe and wraps the first responding MFi coprocessor. */
object MfiRuntime {
    fun scan(transport: I2cTransport): MfiAuthenticationClient {
        val discovery = MfiDeviceScanner(
            transport = transport,
            probeTimeoutMillis = MFI_STARTUP_PROBE_TIMEOUT_MILLIS,
        ).scan()
        val chip = discovery.chip
            ?: throw MfiCoprocessorNotFoundException(
                "No MFi authentication coprocessor responded: " +
                    discovery.failures.joinToString { "0x${it.address7Bit.toString(16)}=${it.error}" },
            )
        return MfiAuthenticationClient(transport, chip.address7Bit)
    }

    /** Value of the I2C device setting that makes [scanLinuxBuses] look for the bus itself. */
    const val AUTO_I2C_PATH = "auto"

    /** The `/dev/i2c-N` nodes this app can open, in bus order. */
    fun linuxI2cNodes(): List<String> =
        File("/dev")
            .listFiles { f -> I2C_NODE.matches(f.name) }
            .orEmpty()
            .filter { it.canRead() && it.canWrite() }
            .sortedBy { it.name.substringAfter("i2c-").toInt() }
            .map { it.path }

    /**
     * Probes every bus of [linuxI2cNodes] (short probe each) and keeps the first one where the coprocessor answers;
     * its transport stays open for the session.
     */
    fun scanLinuxBuses(): LinuxMfiMatch {
        val nodes = linuxI2cNodes()
        if (nodes.isEmpty()) {
            throw MfiCoprocessorNotFoundException("No /dev/i2c-N node this app can open (it needs read and write access)")
        }
        val failures = ArrayList<String>()
        for (path in nodes) {
            val transport = try {
                LinuxI2cTransport.open(path)
            } catch (error: I2cTransportException) {
                failures += "$path: ${error.message}"
                continue
            }
            val chip = MfiDeviceScanner(transport, probeTimeoutMillis = AUTO_PROBE_TIMEOUT_MILLIS).scan().chip
            if (chip != null) return LinuxMfiMatch(path, MfiAuthenticationClient(transport, chip.address7Bit), transport)
            try {
                transport.close()
            } catch (_: I2cTransportException) {
            }
            failures += "$path: no answer"
        }
        throw MfiCoprocessorNotFoundException("No MFi authentication coprocessor on ${failures.joinToString()}")
    }

    private val I2C_NODE = Regex("i2c-[0-9]+")
    private const val MFI_STARTUP_PROBE_TIMEOUT_MILLIS = 15_000L
    private const val AUTO_PROBE_TIMEOUT_MILLIS = 1_000L
}

/** Bus found by [MfiRuntime.scanLinuxBuses]. */
class LinuxMfiMatch(
    val path: String,
    val client: MfiAuthenticationClient,
    val transport: LinuxI2cTransport,
)
