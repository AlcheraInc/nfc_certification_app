package com.alcherainc.nfc_certification_app.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class FaceProximity(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    var maximum = sensor?.maximumRange ?: "(Not Applicable)"
    var distance = 0f
    private var callback: ((Float) -> Unit)? = null

    init {
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun setStatusChangeListener(callback: (distance: Float) -> Unit) {
        this.callback = callback
    }

    override fun onSensorChanged(event: SensorEvent) {
        distance = event.values[0]
        callback?.let { it(distance) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    companion object {
        private const val TAG = "FaceProximity"
    }
}