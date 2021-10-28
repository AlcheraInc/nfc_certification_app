package com.alcherainc.nfc_certification_app

import android.content.Context
import androidx.activity.ComponentActivity
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class NativeCameraLayoutFactory(val activity: ComponentActivity): PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val creationParams = args as Map<String?, Any?>?
        return NativeCameraLayout(activity, context, viewId, creationParams)
    }
}