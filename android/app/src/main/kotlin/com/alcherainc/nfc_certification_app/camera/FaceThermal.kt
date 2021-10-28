package com.alcherainc.nfc_certification_app.camera

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.FrameLayout
import androidx.core.graphics.toRect
import com.alcherainc.facesdk.type.Face
import com.alcherainc.heimann.DetectedArea
import com.alcherainc.heimann.HeatMap
import com.alcherainc.heimann.HeimannDevice
import com.alcherainc.heimann.PreviewSize
import com.common.thermalimage.HotImageCallback
import com.common.thermalimage.TemperatureBitmapData
import com.common.thermalimage.ThermalImageUtil
import com.alcherainc.nfc_certification_app.util.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class FaceThermal(private val context: Context, private val heatmapView: FrameLayout?) {
    private val heimannDevice = HeimannDevice()
    private var dongaDevice: ThermalImageUtil? = null
    private var dongaModel: IntArray? = null
    private val dongaModuleLoadTimer = Timer()
    private var heatMap: HeatMap? = null
    var isStart = false
    var maxTemperature: Float = 0f
    var skinTemperature: Float = 0f

    init {
        start()
    }

    fun start() {
        isStart = if (Device.TYPE == Device.DONGA) {
            dongaDevice = ThermalImageUtil(context)
            dongaModuleLoadTimer.schedule(object : TimerTask() {
                override fun run() {
                    if (dongaDevice?.usingModule != null) {
                        dongaModel = dongaDevice?.usingModule
                        isStart = true
                        Log.d(TAG, "isStart $isStart DONGA ${dongaModel!![0]}")
                        dongaModuleLoadTimer.cancel()
                    }
                }
            }, 0, 500)
            false
        } else if (Device.TYPE == Device.HLDS) {
            val start = heimannDevice.initDevice(context, HeimannDevice.TYPE_SENSOR_SENSOLUTION)
            Log.d(TAG, "isStart HLDS $start")
            start
        } else {
            Log.d(TAG, "isStart PHONE false")
            false
        }
    }

    fun stop() {
        Log.d(TAG, "stop isStart $isStart")
        if (isStart) {
            isStart = false
            if (Device.TYPE == Device.DONGA) {
                dongaModuleLoadTimer.cancel()
                dongaDevice?.release()
            } else if (Device.TYPE == Device.HLDS) {
                heimannDevice.closeDevice()
            }
        }
    }

    fun setFaceInfo(imageWidth: Int?, imageHeight: Int?, face: Face) {
        if (isStart && imageWidth != null && imageHeight != null) {
            if (Device.TYPE == Device.DONGA && dongaModel != null) {
                val modelType = dongaModel!![0]
                var frame = 3
                if (modelType == 4 || modelType == 8 || modelType == 9 || modelType == 10 || modelType == 17) {
                    frame = 1
                } else if (modelType in 19..24 || modelType == 26) {
                    frame = 5
                }
                // make face rect. thermal Rect 32x32 rotation degree 270
                // 카메라 회전값과 온도 센서 회전값이 다르기 때문에 맞춰줘야 함
                val ratioH = 32.0f / imageHeight.toFloat()
                val ratioV = 32.0f / imageWidth.toFloat()
                var left = face.box.x * ratioH
                if (left < 0) {
                    left = 0.0f
                }
                var top = face.box.y * ratioV
                if (top < 0) {
                    top = 0.0f
                }
                var right = left + (face.box.width * ratioH)
                if (right > 32) {
                    right = 32.0f
                }
                var bottom = top + (face.box.height * ratioV)
                if (bottom > 32) {
                    bottom = 32.0f
                }
                val faceRect = RectF(left, top, right, bottom)
                val matrix = Matrix()
                matrix.setRotate(270.0f, 16.0f, 16.0f)
                matrix.mapRect(faceRect)
                val temperature = dongaDevice!!.getDataAndBitmap(faceRect.toRect(), 0f, frame, object : HotImageCallback.Stub() {

                    override fun getTemperatureBimapData(temperatureBitmapData: TemperatureBitmapData?) {
                        if (temperatureBitmapData != null) {
                            val bitmap = temperatureBitmapData.bitmap
                            // bitmap 320x320
                            if (heatmapView != null) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    heatmapView.background = BitmapDrawable(context.resources, bitmap)
                                }
                            }
                        }
                    }

                    override fun onTemperatureFail(reason: String?) {
                        Log.d(TAG, "onTemperatureFail $reason")
                    }

                })
                maxTemperature = 0.0f
                skinTemperature = 0.0f
                if (temperature != null) {
                    maxTemperature = temperature.temperatureNoCorrect
                    skinTemperature = temperature.temperature
                }
            } else if (Device.TYPE == Device.HLDS) {
                val previewSize = PreviewSize()
                previewSize.width = imageWidth
                previewSize.height = imageHeight

                /*
                 * Thermal 영역에 대해 보정이 필요하다.
                 * (Thermal Lib에서 예전의 Face SDK에서 얼굴이 detect되는 위치와 현재 SDK에서 뽑히는 Face 영역과 다를 수 있음)
                 */
                val detectedArea = DetectedArea()
                detectedArea.left = face.box.x - (face.box.width / 3)
                if (detectedArea.left < 0) {
                    detectedArea.left = 0.0f
                }
                detectedArea.top = face.box.y - (face.box.height * 2 / 3)
                if (detectedArea.top < 0) {
                    detectedArea.top = 0.0f
                }
                detectedArea.right = face.box.x + face.box.width + (face.box.width / 3)
                if (imageWidth < detectedArea.right) {
                    detectedArea.right = imageWidth.toFloat()
                }
                detectedArea.bottom = face.box.y + (face.box.height / 2)
                if (imageHeight < detectedArea.bottom) {
                    detectedArea.bottom = imageHeight.toFloat()
                }

                try {
                    maxTemperature = 0.0f
                    skinTemperature = 0.0f
                    val temperature = heimannDevice.getMaxTemperature(previewSize, detectedArea)
                    maxTemperature = temperature.maxTemperature
                    skinTemperature = temperature.maxSkinTemperature
                    heatMap = heimannDevice.getHeatMapWithInputData(temperature)
                    val bitmap = heimannDevice.getBitmapFromHeatMap(heatMap, 160, 160)
                    if (heatmapView != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            heatmapView.background = BitmapDrawable(context.resources, bitmap)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Log.d(TAG, "maxTemperature $maxTemperature skinTemperature $skinTemperature")
        }
    }

    companion object {
        private const val TAG = "FaceThermal"
    }
}