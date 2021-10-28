package com.alcherainc.nfc_certification_app.util

import android.graphics.*
import android.util.Base64
import com.alcherainc.facesdk.error.Error
import com.alcherainc.facesdk.type.FeatureExtension.FaceFeature
import com.alcherainc.facesdk.type.FeatureExtension.InputAlignedFaceImage
import com.alcherainc.facesdk.type.InputImage
import com.alcherainc.facesdk.type.Point
import com.alcherainc.nfc_certification_app.camera.FaceSdk
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImageUtils {
    // https://stackoverflow.com/questions/18086568/android-convert-argb-8888-bitmap-to-3byte-bgr/18101779#18101779
    fun getImagePixels(image: Bitmap): ByteArray {
        // calculate how many bytes our image consists of
        val bytes = image.byteCount
        val buffer = ByteBuffer.allocate(bytes) // Create a new buffer
        image.copyPixelsToBuffer(buffer) // Move the byte data to the buffer
        val temp = buffer.array() // Get the underlying array containing the data.
        val pixels = ByteArray(temp.size / 4 * 3) // Allocate for 3 byte BGR

        // Copy pixels into place
        for (i in 0 until temp.size / 4) {
            pixels[i * 3] = temp[i * 4 + 2] // B
            pixels[i * 3 + 1] = temp[i * 4 + 1] // G
            pixels[i * 3 + 2] = temp[i * 4 + 0] // R
            // Alpha is discarded
        }
        return pixels
    }

    fun makeJpegBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun Base64ToFloatArray(base64Str: String): FloatArray {
        var byteArray = Base64.decode(base64Str, Base64.DEFAULT)
        var floatBuffer = ByteBuffer.wrap(byteArray).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val floatArray = FloatArray(floatBuffer.capacity())
        floatBuffer.get(floatArray)

        return floatArray
    }

    fun ExtractFeatureFromBase64JpegByteArray(faceBase64: String): FaceFeature {
        val faceFeature: FaceFeature
        Base64.decode(faceBase64, Base64.DEFAULT).also {
            val faceBitmap = BitmapFactory.decodeByteArray(it, 0, it.size)

            val faceInputAlignedFace = InputAlignedFaceImage()
            val faceByteArray = getImagePixels(faceBitmap)

            faceInputAlignedFace.bgr_image_buffer = faceByteArray
            faceFeature = FaceSdk.getFeatureExtension().ExtractFeature(faceInputAlignedFace)
        }

        return faceFeature
    }

    //  Draw virtual mask on face in bitmap image using face landmark
    fun extractVirtualMask(bitmap: Bitmap, landmark: Array<Point>): Bitmap {
        val maskBitmap = bitmap.copy(bitmap.config, true)
        val path = Path()
        val maskPointIndices = arrayOf(45, 81, 30, 29, 28, 27, 26, 25, 24, 23, 22,
            21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 80)
        for(i in 0..maskPointIndices.size - 1) {
            val landmarkIndex = landmark[maskPointIndices[i]]
            if(i == 0) {
                path.moveTo(landmarkIndex.x, landmarkIndex.y)
            } else {
                path.lineTo(landmarkIndex.x, landmarkIndex.y)
            }
        }
        val paint = Paint()
        paint.setColor(Color.WHITE)
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true

        val canvas = Canvas(maskBitmap)
        canvas.drawPath(path, paint)

        return maskBitmap
    }

    //  Draw virtual mask on face in bitmap image
    fun extractVirtualMask(base64JpegImage: String): Bitmap? {
        val faceByteArray = Base64.decode(base64JpegImage, Base64.DEFAULT)
        val faceBitmap = BitmapFactory.decodeByteArray(faceByteArray, 0, faceByteArray.size)

        val inputImage = InputImage()
        inputImage.bgr_image_buffer = getImagePixels(faceBitmap)
        inputImage.width = faceBitmap.width
        inputImage.height = faceBitmap.height

        val detectResult = FaceSdk.getInstance().DetectFaceInContinuousImage(inputImage)
        if(detectResult.last_error == Error.NoError
            && detectResult.faces != null
            && detectResult.faces.size > 0) {
            return extractVirtualMask(faceBitmap, detectResult.faces[0].landmark.points)
        } else {
            return null
        }
    }
}