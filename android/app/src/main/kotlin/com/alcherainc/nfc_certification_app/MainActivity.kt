package com.alcherainc.nfc_certification_app

import io.flutter.Log
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity: FlutterFragmentActivity() {
    companion object {
        val TAG = MainActivity::class.java.simpleName
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        Log.d(TAG, "configureFlutterEngine")
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory("scan_layout", NativeCameraLayoutFactory(this))
    }
}
