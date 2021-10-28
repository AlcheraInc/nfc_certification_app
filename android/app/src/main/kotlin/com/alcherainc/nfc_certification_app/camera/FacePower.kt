package com.alcherainc.nfc_certification_app.camera

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.round

class FacePower(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
    private var thermal = "(Not Applicable)"

    init {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            powerManager?.addThermalStatusListener {
                when (it) {
                    PowerManager.THERMAL_STATUS_NONE -> {
                        thermal = "OK"
                    }
                    PowerManager.THERMAL_STATUS_LIGHT -> {
                        thermal = "LIGHT"
                    }
                    PowerManager.THERMAL_STATUS_MODERATE -> {
                        thermal = "MODERATE"
                    }
                    PowerManager.THERMAL_STATUS_SEVERE -> {
                        thermal = "SEVERE"
                    }
                    PowerManager.THERMAL_STATUS_CRITICAL -> {
                        thermal = "CRITICAL"
                    }
                    PowerManager.THERMAL_STATUS_EMERGENCY -> {
                        thermal = "EMERGENCY"
                    }
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> {
                        thermal = "SHUTDOWN"
                    }
                    else -> {
                        thermal = "UNKNOWN"
                    }
                }
            }
        }
    }

    fun getStatusPower(): Power {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val chargeStatus: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val charging: Boolean = chargeStatus == BatteryManager.BATTERY_STATUS_CHARGING || chargeStatus == BatteryManager.BATTERY_STATUS_FULL
        val usb: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val ac: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
        val battery = batteryStatus?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)
        val percent = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }
        return Power(percent, charging, usb, ac, battery)
    }

    fun getStatusThermal(): DeviceThermal {
        return DeviceThermal(thermal, round(getCpuTemperature() * 10) / 10)
    }

    private fun getCpuTemperature(): Float {
        val process: Process
        return try {
            process = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp")
            process.waitFor()
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            bufferedReader.use {
                val line: String = bufferedReader.readLine()
                line.toFloat() / 1000.0f
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0.0f
        }
    }

    data class Power(
        val percent: Float?,
        val charging: Boolean,
        val usb: Boolean,
        val ac: Boolean,
        val battery: Boolean?,
    )

    data class DeviceThermal(
        val status: String,
        val cpu: Float
    )

    companion object {
        private const val TAG = "FacePower"
    }
}