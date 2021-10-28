package com.alcherainc.nfc_certification_app

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.camera.view.PreviewView
import com.alcherainc.nfc_certification_app.camera.FaceCamera
import com.alcherainc.nfc_certification_app.camera.FaceCameraX
import com.alcherainc.nfc_certification_app.camera.FaceSdk
import io.flutter.Log
import io.flutter.plugin.platform.PlatformView

class NativeCameraLayout(activity: ComponentActivity, context: Context, id: Int, creationParams: Map<String?, Any?>?): PlatformView {
    companion object {
        val TAG = NativeCameraLayout::class.java.simpleName
    }

    private val frameLayout: FrameLayout = FrameLayout(context)
    private val previewView: PreviewView = PreviewView(context)

    private lateinit var camera: FaceCamera

    override fun getView(): View {
        return frameLayout
    }

    init {
        frameLayout.addView(previewView)

        Log.d(TAG, "FaceSdk.initialize: ${FaceSdk.initialize(activity)}")
        val camera = FaceCameraX(activity, frameLayout, previewView,
            false, false, false,
        null, null, null)
        camera.start()
    }

    override fun dispose() {
        camera.stop()
    }
}