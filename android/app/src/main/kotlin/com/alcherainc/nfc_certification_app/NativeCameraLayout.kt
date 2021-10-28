package com.alcherainc.nfc_certification_app

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.camera.view.PreviewView
import io.flutter.plugin.platform.PlatformView

class NativeCameraLayout(context: Context, id: Int, creationParams: Map<String?, Any?>?): PlatformView {
    private val frameLayout: FrameLayout = FrameLayout(context)
    val previewView: PreviewView = PreviewView(context)

    override fun getView(): View {
        return frameLayout
    }

    override fun dispose() {

    }

    init {
        frameLayout.setBackgroundColor(Color.argb(100, 255, 0, 255))
        frameLayout.addView(previewView)
    }
}