package com.alcherainc.nfc_certification_app.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraMetadata
import android.os.HandlerThread
import android.util.Size
import android.widget.FrameLayout
import android.widget.ImageView

abstract class FaceCamera {
    abstract fun start(flipRgbCamera: Boolean = false)
    abstract fun stop()

    //  Set FaceProcessor orientation getting by OrientationEventListener
    abstract fun setRotation(surfaceRotation: Int)

    abstract protected fun initializeCameraInfo()
    abstract protected fun openIRCamera()
    abstract protected fun closeIRCamera()
    abstract fun getTemperature(callback: (Float) -> Unit)
}