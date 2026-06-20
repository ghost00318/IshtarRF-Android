package com.ishtarrf.data.serial

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * Owns the USB-OTG serial link to the ESP32. Exposes:
 *  - [state]: a [StateFlow] of the connection lifecycle.
 *  - [lines]: a [SharedFlow] of complete, newline-delimited JSON strings.
 *
 * Framing mirrors the desktop app: bytes are buffered and split on `\n`.
 */
class UsbSerialManager(
    private val appContext: Context,
) : SerialInputOutputManager.Listener {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.ishtarrf.USB_PERMISSION"
        private const val BAUD_RATE = 115_200
        private const val WRITE_TIMEOUT_MS = 2_000
    }

    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager

    private var port: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private val rxBuffer = StringBuilder()

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _lines = MutableSharedFlow<String>(extraBufferCapacity = 512)
    val lines: SharedFlow<String> = _lines.asSharedFlow()

    private var receiverRegistered = false

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            @Suppress("DEPRECATION")
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            if (granted && device != null) {
                openDevice(device)
            } else {
                _state.value = ConnectionState.Error("USB permission denied")
            }
        }
    }

    fun availableDrivers(): List<UsbSerialDriver> =
        UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

    /** True when a supported adapter is currently plugged in. */
    fun hasDevice(): Boolean = availableDrivers().isNotEmpty()

    /** Begins (or requests permission for) a connection to the first adapter found. */
    fun connect() {
        val drivers = availableDrivers()
        if (drivers.isEmpty()) {
            _state.value = ConnectionState.Error("No USB serial device found. Plug the ESP32 in via OTG.")
            return
        }
        registerReceiver()
        val device = drivers.first().device
        if (usbManager.hasPermission(device)) {
            openDevice(device)
        } else {
            _state.value = ConnectionState.Connecting
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val intent = Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName)
            val pi = PendingIntent.getBroadcast(appContext, 0, intent, flags)
            usbManager.requestPermission(device, pi)
        }
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        // Register as NOT_EXPORTED on every API level. The USB-permission result is
        // an internal broadcast we send to ourselves via an explicit, package-scoped
        // PendingIntent, so no other app should be able to deliver a spoofed result.
        ContextCompat.registerReceiver(
            appContext,
            permissionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun openDevice(device: UsbDevice) {
        try {
            val driver = availableDrivers().firstOrNull { it.device.deviceId == device.deviceId }
                ?: UsbSerialProber.getDefaultProber().probeDevice(device)
                ?: run {
                    _state.value = ConnectionState.Error("Unsupported USB device")
                    return
                }
            val connection = usbManager.openDevice(device) ?: run {
                _state.value = ConnectionState.Error("Failed to open device (no permission?)")
                return
            }
            val p = driver.ports[0]
            p.open(connection)
            p.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            // Assert control lines like pyserial does; ignore adapters that don't support them.
            runCatching { p.dtr = true }
            runCatching { p.rts = true }

            port = p
            synchronized(rxBuffer) { rxBuffer.setLength(0) }
            ioManager = SerialInputOutputManager(p, this).apply { start() }

            _state.value = ConnectionState.Connected(
                deviceName = device.productName ?: driver.javaClass.simpleName.removeSuffix("Driver"),
            )
        } catch (e: Exception) {
            _state.value = ConnectionState.Error(e.message ?: "Open error")
            closeQuietly()
        }
    }

    fun disconnect() {
        closeQuietly()
        _state.value = ConnectionState.Disconnected
    }

    private fun closeQuietly() {
        runCatching { ioManager?.stop() }
        ioManager = null
        runCatching { port?.close() }
        port = null
    }

    /** Sends a raw command string (already newline-terminated). */
    fun write(text: String) {
        val p = port ?: return
        try {
            p.write(text.toByteArray(Charsets.UTF_8), WRITE_TIMEOUT_MS)
        } catch (e: IOException) {
            _state.value = ConnectionState.Error("Write error: ${e.message}")
            closeQuietly()
        }
    }

    // ---- SerialInputOutputManager.Listener (called on the IO thread) ----

    override fun onNewData(data: ByteArray) {
        synchronized(rxBuffer) {
            rxBuffer.append(String(data, Charsets.UTF_8))
            var idx = rxBuffer.indexOf("\n")
            while (idx >= 0) {
                val line = rxBuffer.substring(0, idx).trim()
                rxBuffer.delete(0, idx + 1)
                if (line.isNotEmpty()) _lines.tryEmit(line)
                idx = rxBuffer.indexOf("\n")
            }
        }
    }

    override fun onRunError(e: Exception) {
        _state.value = ConnectionState.Error("Serial error: ${e.message}")
        closeQuietly()
    }
}
