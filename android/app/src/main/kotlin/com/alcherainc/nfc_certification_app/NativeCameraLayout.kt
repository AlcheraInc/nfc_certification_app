package com.alcherainc.nfc_certification_app

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import io.flutter.plugin.platform.PlatformView

class NativeCameraLayout(context: Context, id: Int, creationParams: Map<String?, Any?>?): PlatformView {
    private val textView: TextView = TextView(context)

    override fun getView(): View {
        return textView
    }

    override fun dispose() {

    }

    init {
        textView.textSize = 72f
        textView.setBackgroundColor(Color.rgb(255, 255, 255))
        textView.text = "rendered on a native Android view (id: $id)"
    }
}