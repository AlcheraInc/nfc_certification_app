package com.alcherainc.nfc_certification_app.camera

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.alcherainc.facesdk.AntispoofingExtension
import com.alcherainc.facesdk.AttributeExtension
import com.alcherainc.facesdk.FaceSDK
import com.alcherainc.facesdk.FeatureExtension
import com.alcherainc.facesdk.type.Bool
import com.alcherainc.facesdk.error.Error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object FaceSdk {
    private const val TAG = "FaceSdk"
    private var SDK_MODEL_PATH = ""
    private val faceSDK = FaceSDK()
    private var antispoofingExtension: AntispoofingExtension? = null
    private var featureExtension: FeatureExtension? = null
    private var attributeExtension: AttributeExtension? = null

    fun getInstance(): FaceSDK {
        return faceSDK
    }

    fun getFeatureExtension(): FeatureExtension {
        if (featureExtension == null) {
            featureExtension = FeatureExtension()
        }
        return featureExtension!!
    }

    fun getAntispoofingExtensionInstance(): AntispoofingExtension {
        if (antispoofingExtension == null) {
            antispoofingExtension = AntispoofingExtension()
        }
        return antispoofingExtension!!
    }

    fun getAttributeExtensionInstance(): AttributeExtension {
        if (attributeExtension == null) {
            attributeExtension = AttributeExtension()
        }
        return attributeExtension!!
    }

    private fun sdkInstall(activity: Activity) {
        SDK_MODEL_PATH = activity.filesDir.absolutePath + File.separator + "model"
        val dest = File(SDK_MODEL_PATH)
        var isInitInstall = false
        if (!dest.exists()) {
            dest.mkdir()
            isInitInstall = true
            Log.d(TAG, "sdkInstall InitInstall")
        }
        val fileNameList = activity.assets.list("model") ?: emptyArray()
        if (fileNameList.isNotEmpty() && fileNameList.size < 2) {
            isInitInstall = true
            Log.d(TAG, "sdkInstall InitInstall model")
        }
//        if (!isInitInstall &&
//            (BuildConfig.VERSION_CODE != PreferenceUtil.getValue(Constants.PREF_VERSION_CODE, 1)
//                    || BuildConfig.VERSION_NAME != PreferenceUtil.getValue(Constants.PREF_VERSION_NAME, "1.0"))) {
//            PreferenceUtil.put(Constants.PREF_VERSION_CODE, BuildConfig.VERSION_CODE)
//            isInitInstall = true
//            Log.d(TAG, "sdkInstall InitInstall ${BuildConfig.VERSION_CODE}")
//        }
        if (isInitInstall) {
            Log.d(TAG, "sdkInstall start")
            for (fileName in fileNameList) {
                val file = File("$SDK_MODEL_PATH${File.separator}$fileName")
                file.createNewFile()
                file.outputStream().use {
                    activity.assets.open("model${File.separator}$fileName").copyTo(it)
                }
            }
            Log.d(TAG, "sdkInstall success")
        } else {
            Log.d(TAG, "sdkInstall pass")
        }
    }

    fun initialize(activity: Activity): Boolean {
        sdkInstall(activity)

        //  ONLY use with FaceSDK with sentinel license feature
//        Log.d(TAG, "Check Sentinel license")
//        val checkLicenseResult = faceSDK.CheckSentinelLicense()
//        if(checkLicenseResult.result == false) {
//            val authorizationCode = faceSDK.GetAuthorizationCode()
//            if(authorizationCode.last_error != Error.NoError) {
//                Log.e("Sentinel", "GetAuthorizationCode: ${authorizationCode.last_error}")
//                false
//            }
//
//            val activateWithServerResult = faceSDK.ActivateWithServer(
//                BuildConfig.SentinelUrl,
//                BuildConfig.SentinelKey,
//                authorizationCode.text)
//            if(activateWithServerResult.last_error != Error.NoError) {
//                Log.e("Sentinel", "ActivateWithServer: ${activateWithServerResult.last_error}")
//                false
//            }
//
//            val updateSentinelResult = faceSDK.UpdateSentinelLicense(activateWithServerResult.text)
//            if(updateSentinelResult.last_error != Error.NoError) {
//                Log.e("Sentinel", "UpdateSentinelLicense: ${updateSentinelResult.last_error}")
//                false
//            }
//        }


//        Log.i("LOG/D", "--------------------[Activate Example]--------------------");
//
//        val activator = Activator();
//        val act_init = activator.Initialize();
//        Log.d("LOG/D", "Activator.Initialize() = last_error: " + act_init.last_error + ", result: " + act_init.result);
//        if (!(act_init.result && act_init.last_error == ActivatorError.LicenseAlreadyActivated)) {
//            val device_code = activator.CreateDeviceCode();
//            Log.d("LOG/D", "Activator.CreateDeviceCode() = last_error: " + device_code.last_error + ", result: " + device_code.result + ", string: " + device_code.string);
//            val license_server = LicenseServer("http://license.alcherainc.com:8081");
//            val activate_code = license_server.getActivateCode("9dbb3fa2-1989-4293-802b-b43d15b7d168", device_code.string);
//            Log.d("LOG/D", "LicenseServer.getActivateCode() = " + activate_code);
//            val act_res = activator.ActivateDevice(activate_code);
//            Log.d("LOG/D", "Activator.ActivateDevice() = last_error: " + act_res.last_error + ", result: " + act_res.result);
//        }
//        val act_deinit = activator.Deinitialize();
//        Log.d("LOG/D", "Activator.Deinitialize() = last_error: " + act_deinit.last_error + ", result: " + act_deinit.result);
//
//        Log.i("LOG/D", "--------------------[Activate Example END]--------------------");

        val result = faceSDK.Initialize(SDK_MODEL_PATH)
        return if (checkError(activity, result, "FaceSDK.Initialize")) {
            success(activity)
        } else {
            false
        }
    }

    private fun success(activity: Activity): Boolean {
        faceSDK.SetMaxDetectableCount(1)
        antispoofingExtension = faceSDK.GetAntispoofingExtension()
        attributeExtension = faceSDK.GetAttributeExtension()
        val resultAntispoofing = checkError(activity, antispoofingExtension!!.Enable(), "AntispoofingExtension.Enable")

//        featureExtension = faceSDK.GetFeatureExtension()
//        val resultFeature =
//            if(BuildConfig.USE_FEATURE_EXTENSION)
//                checkError(activity, featureExtension!!.Enable(), "FeatureExtension.Enable")
//            else
//                true
        val resultFeature = true

        val resultAttribute = checkError(activity, attributeExtension!!.Enable(), "AttributeExtension.Enable")
        return resultAntispoofing && resultFeature && resultAttribute
    }

    fun deinitialize(activity: Activity) {
        checkError(activity, faceSDK.Deinitialize(), "FaceSDK.Deinitialize")
    }

    private fun checkError(activity: Activity, result: Bool, description: String): Boolean {
        if (result.result && result.last_error === Error.NoError) {
            Log.d(TAG, "FaceSDK success $description")
            return true
        } else {
            val message = "FaceSDK error $description ${result.result} ${result.last_error}"
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            }
        }
        return false
    }
}