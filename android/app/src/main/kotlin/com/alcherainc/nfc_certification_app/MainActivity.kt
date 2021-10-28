package com.alcherainc.nfc_certification_app

import android.os.Bundle
import android.os.PersistableBundle
import io.flutter.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity: FlutterActivity() {
    companion object {
        val TAG = MainActivity::class.java.simpleName
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory("scan_layout", NativeCameraLayoutFactory())
    }
}
