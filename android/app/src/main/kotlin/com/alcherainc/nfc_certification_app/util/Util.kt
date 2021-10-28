package com.alcherainc.nfc_certification_app.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.alcherainc.facesdk.type.FeatureExtension.FaceFeature
import com.alcherainc.facesdk.type.FeatureExtension.InputAlignedFaceImage
import com.alcherainc.nfc_certification_app.camera.YuvToRgbConverter
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.*
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.collections.HashMap

object Util {

    private const val TAG = "Util"


    fun decodeBarcode(bitmap: Bitmap): Result? {
        val qrReader = MultiFormatReader()
        val imageWidth = bitmap.width
        val imageHeight = bitmap.height
        val imagePixels = IntArray(imageWidth * imageHeight)
        bitmap.getPixels(imagePixels, 0, imageWidth, 0, 0, imageWidth, imageHeight)

        val source = RGBLuminanceSource(imageWidth, imageHeight, imagePixels)
        try{
            return qrReader.decode(BinaryBitmap(HybridBinarizer(source)))
        } catch(e: java.lang.Exception) {
            Log.w(TAG, "Exception occured while scanning QR code: ${e}")
            return null
        }
    }
}