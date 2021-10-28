package com.alcherainc.nfc_certification_app.util

import android.os.Build
import android.util.Log
import java.io.File
import java.util.*

object Device {

    const val TAG = "Device"

    const val DONGA = 0
    const val HLDS = 1
    const val PHONE = 2

    val TYPE: Int

    init {
        // check device models
        TYPE = if (File("/sys/class/backlight/led-brightness/brightness").exists()) {
            DONGA
//            RGB Camera Front available size 1920x1080,1280x720,1024x768,800x600,640x480,320x240
//            IR Camera Back available size 1280x960,1280x720,1024x768,800x600,848x480,640x480
        } else if (File("/sys/devices/platform/misc_power_en/w_led").exists()) {
            HLDS
//            RGB Camera Back available size 1920x1080,1280x960,1280x720,800x600,640x480,320x240,160x120
//            IR Camera Front available size 1920x1080,1280x1024,1280x720,800x600,640x480,352x288,320x240,176x144,160x120
        } else {
            PHONE
        }
        Log.i(TAG, "DEVICE_TYPE : $TYPE");
        Log.i(TAG, "CPU_ABI : ${Build.CPU_ABI}");
        Log.i(TAG, "CPU_ABI2 : ${Build.CPU_ABI2}");
        Log.i(TAG, "OS.ARCH : ${System.getProperty("os.arch")}");
        Log.i(TAG, "SUPPORTED_ABIS : ${Arrays.toString(Build.SUPPORTED_ABIS)}");
        Log.i(TAG, "SUPPORTED_32_BIT_ABIS : ${Arrays.toString(Build.SUPPORTED_32_BIT_ABIS)}");
        Log.i(TAG, "SUPPORTED_64_BIT_ABIS : ${Arrays.toString(Build.SUPPORTED_64_BIT_ABIS)}");
    }
}