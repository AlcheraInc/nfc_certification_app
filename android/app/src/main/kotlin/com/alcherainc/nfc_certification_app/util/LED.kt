package com.alcherainc.nfc_certification_app.util

import java.io.File
import java.io.FileOutputStream

object LED {

    private const val TAG = "LED"

    enum class TYPE {
        WHITE,
        RED,
        GREEN
    }

    private val FILE_PATH: List<String> = if (Device.TYPE == Device.DONGA) {
        listOf(
            "/sys/class/backlight/led-brightness/brightness"
        )
    } else if (Device.TYPE == Device.HLDS) { // HLDS
        listOf(
            "/sys/devices/platform/misc_power_en/w_led",
            "/sys/devices/platform/misc_power_en/r_led",
            "/sys/devices/platform/misc_power_en/g_led"
        )
    } else {
        emptyList()
    }

    private var devicesFiles: List<File> = FILE_PATH.map { File(it) }
    private var devices: List<FileOutputStream>? = null

    private val ON = "1".toByteArray()
    private val OFF = "0".toByteArray()

    private val BRIGHTNESS_0 = "0".toByteArray()
    private val BRIGHTNESS_255 = "100".toByteArray()

    fun init() {
        dispose()
        devices = devicesFiles.map { it.outputStream() }
    }

    fun dispose() {
        if (devices != null) {
            TYPE.values().forEach { off(it) }
            devices?.forEach {
                try {
                    it.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            devices = null
        }
    }

    fun on(type: TYPE) {
        TYPE.values().forEach { off(it) }
        if (Device.TYPE == Device.DONGA) {
            sendMessage(TYPE.WHITE, BRIGHTNESS_255)
        } else if (Device.TYPE == Device.HLDS) {
            sendMessage(type, ON)
        }
    }

    fun off(type: TYPE) {
        if (Device.TYPE == Device.DONGA) {
            sendMessage(TYPE.WHITE, BRIGHTNESS_0)
        } else if (Device.TYPE == Device.HLDS) {
            sendMessage(type, OFF)
        }
    }

    private fun sendMessage(type: TYPE, message: ByteArray) {
        if (devices != null) {
            try {
                devices!![type.ordinal].write(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}