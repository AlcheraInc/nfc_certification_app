package com.alcherainc.nfc_certification_app.camera

import android.app.Activity
import android.graphics.Bitmap
import android.media.Image
import android.media.ImageReader
import android.widget.ImageView
import com.alcherainc.facesdk.type.AntispoofingExtension.InputIRImage
import com.alcherainc.facesdk.type.InputImage
import com.alcherainc.nfc_certification_app.util.Device
import com.alcherainc.nfc_certification_app.util.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FaceProcessorIR(
    private val activity: Activity,
    private val faceClipView: ImageView?,
) : ImageReader.OnImageAvailableListener {

    var orientation: Int = 0
    private val faceClip = FaceClip(activity)
    private val yuvToRgbConverter = YuvToRgbConverter(activity)

    override fun onImageAvailable(reader: ImageReader?) {
        val image = reader?.acquireLatestImage()

        var bitmap: Bitmap? = null
        if (image != null && FaceDetectLogic.valid) {
            val byteArray = getByteArray(image.planes)
            FaceDetectLogic.irInputDepthImage = getInputDepthImage(byteArray, image.width, image.height, orientation)

            bitmap = if (Device.TYPE == Device.DONGA) {
                // DONA전기의 카메라의 경우 device에서 resize가 안된 상태로 들어옴. 때문에 강제로 사이즈 조정해야 함.
                yuvToRgbConverter.getImageToBitmap(image, orientation, image.height, image.width)
            } else { // HLDS
                yuvToRgbConverter.getImageToBitmap(image, orientation, null, null)
            }
            val inputImage = InputImage()
            inputImage.width = bitmap.width
            inputImage.height = bitmap.height
            inputImage.bgr_image_buffer = ImageUtils.getImagePixels(bitmap)
            FaceDetectLogic.irInputImage = inputImage
            FaceDetectLogic.update()
        }
        if (image != null && faceClipView != null && bitmap != null) {
            CoroutineScope(Dispatchers.Main).launch {
                faceClipView.setImageBitmap(bitmap)
            }
        }
        image?.close()
    }

    private fun getByteArray(planes: Array<Image.Plane>): ByteArray {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize: Int = yBuffer.remaining()
        val uSize: Int = uBuffer.remaining()
        val vSize: Int = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        return nv21
    }

    private fun getInputDepthImage(
        byteArray: ByteArray,
        width: Int,
        height: Int,
        orientation: Int,
    ): InputIRImage {
        val yByteArray = byteArray.slice(IntRange(0, (width * height) - 1)).toByteArray()
        var mWidth = width
        var mHeight = height
        val newByteArray = when (orientation) {
            270 -> {
                mWidth = height
                mHeight = width
                yuvToRgbConverter.rotateYUV420Degree270(yByteArray, width, height)
            }
            180 -> yuvToRgbConverter.rotateYUV420Degree180(yByteArray, width, height)
            90 -> {
                mWidth = height
                mHeight = width
                yuvToRgbConverter.rotateYUV420Degree90(yByteArray, width, height)
            }
            else -> yByteArray
        }
        val inputIRImage = InputIRImage()
        inputIRImage.ir_image_buffer = newByteArray
        inputIRImage.width = mWidth
        inputIRImage.height = mHeight
        return inputIRImage
    }

    private fun onDrawFace(inputImage: InputImage?, image: Image?) {
        if (faceClipView != null && inputImage != null && FaceDetectLogic.irFaces != null && FaceDetectLogic.irFaces!!.isNotEmpty()) {
            val face = FaceDetectLogic.irFaces!![0]
            if (face != null) {
                val bitmap = faceClip.getFaceBitmap(image, face, orientation)
                CoroutineScope(Dispatchers.Main).launch {
                    faceClipView.setImageBitmap(bitmap)
                }
            }
        }
    }

    companion object {
        private const val TAG = "FaceProcessorIR"
    }

}