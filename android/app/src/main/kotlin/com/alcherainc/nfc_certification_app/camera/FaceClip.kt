package com.alcherainc.nfc_certification_app.camera

import android.content.Context
import android.graphics.*
import android.media.Image
import com.alcherainc.facesdk.type.Face


class FaceClip(private val context: Context) {

    private val yuvToRgbConverter = YuvToRgbConverter(context)

    fun getFaceBitmap(image: Image?, face: Face, rotationDegrees: Int, resizeWidth: Int? = null, resizeHeight: Int? = null): Bitmap? {
        if (image != null) {
            val cropBitmap = yuvToRgbConverter.getImageToBitmap(image, rotationDegrees, resizeWidth, resizeHeight)
            var x = face.box.x.toInt()
            if (x < 0) {
                x = 0
            }
            var y = face.box.y.toInt()
            if (y < 0) {
                y = 0
            }
            var w = face.box.width.toInt()
            if (cropBitmap.width < x + w) {
                w = cropBitmap.width - x
            }
            var h = face.box.height.toInt()
            if (cropBitmap.height < y + h) {
                h = cropBitmap.height - y
            }
            return Bitmap.createBitmap(cropBitmap, x, y, w, h)
        }
        return null
    }

    fun flippingBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.setScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
    }

    /**
     * @param bmp input bitmap
     * @param contrast 0..2 1 is default
     * @param brightness -255..255 0 is default
     * @return new Bitmap
     */
    private fun changeBitmapContrastBrightness(bmp: Bitmap, contrast: Float = 1f, brightness: Float = 0f): Bitmap {
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val ret = Bitmap.createBitmap(bmp.width, bmp.height, bmp.config)
        val canvas = Canvas(ret)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        return ret
    }

    companion object {
        fun getFaceBitmap(bitmap: Bitmap, face: Face): Bitmap? {
            var x = face.box.x.toInt()
            if (x < 0) {
                x = 0
            }
            var y = face.box.y.toInt()
            if (y < 0) {
                y = 0
            }
            var w = face.box.width.toInt()
            if (bitmap.width < x + w) {
                w = bitmap.width - x
            }
            var h = face.box.height.toInt()
            if (bitmap.height < y + h) {
                h = bitmap.height - y
            }
            return Bitmap.createBitmap(bitmap, x, y, w, h)
        }
    }
}