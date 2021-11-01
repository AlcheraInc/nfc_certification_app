package com.alcherainc.nfc_certification_app.camera

import android.content.Context
import android.graphics.*
import android.hardware.camera2.CameraMetadata
import android.renderscript.Float4
import android.renderscript.Matrix4f
import android.util.Log
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.core.graphics.withMatrix
import com.alcherainc.facesdk.type.Face
import com.alcherainc.facesdk.type.InputImage


class FaceGraphicView(context: Context) : View(context) {

    var faces: Array<Face>? = null
    val transformationMatrix = Matrix()
    var imageWidth = 0
    var imageHeight = 0
    var imageRotation = 0
    private var scaleFactor = 1.0f
    private var postScaleWidthOffset = 0f
    private var postScaleHeightOffset = 0f
    private var isImageFlipped = false
    private var needUpdateTransformation = false
//    private var textDrawLinePosition = 50f
//    private val detectInfo = object {
//        var fps = 0
//        var avgMs = 0L
//        var maxMs = 0L
//        var minMs = 0L
//        var currentMs = 0L
//        var freeSize = 0L
//        var totalSize = 0L
//        var usedSize = 0L
//        var usedSizePercent = 0L
//    }
//    var faceProximity: FaceProximity? = null
//    var facePower: FacePower? = null
//    var faceThermal: FaceThermal? = null

    fun setCameraSelector(cameraMetadata: Int) {
        val isImageFlipped = cameraMetadata == CameraMetadata.LENS_FACING_FRONT
        if (this.isImageFlipped != isImageFlipped) {
            needUpdateTransformation = true
        }
        this.isImageFlipped = isImageFlipped
    }

    fun setCameraSelector(cameraSelector: CameraSelector) {
        val isImageFlipped = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
        if (this.isImageFlipped != isImageFlipped) {
            needUpdateTransformation = true
        }
        this.isImageFlipped = isImageFlipped
    }

    fun setInputImage(inputImage: InputImage, rgbFaces: Array<Face>?) {
        val imageWidth = inputImage.width
        val imageHeight = inputImage.height
        if (this.imageWidth != imageWidth || this.imageHeight != imageHeight) {
            needUpdateTransformation = true
        }
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.faces = rgbFaces

        val imageAspectRatio = imageWidth.toFloat() / imageHeight
        if(imageAspectRatio > 1f) {
            faceBoxPaint.strokeWidth = 4.0f * (imageWidth.toFloat() / 640f)    //  default: (640x480, 4f)
            faceBoxLineLength = 20f * (imageWidth.toFloat() / 640f)
        } else {
            faceBoxPaint.strokeWidth = 4.0f * (imageHeight.toFloat() / 640f)
            faceBoxLineLength = 20f * (imageHeight.toFloat() / 640f)
        }
    }

    fun setRotation(rotation: Int) {
        if(this.imageRotation != rotation) {
            needUpdateTransformation = true
            this.imageRotation = rotation
        }
    }

//
//    private val textPaint = Paint().apply {
//        color = Color.BLUE
//        textSize = 20f
//        strokeWidth = 5.0f
//    }

    private val faceBoxPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND

        strokeWidth = 4.0f
    }
    private var faceBoxLineLength = 20f

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT)
            updateTransformationIfNeeded()
            drawFaces(canvas)
//            textDrawLinePosition = 50f
//            drawPower(canvas)
//            drawProximity(canvas)
//            drawDetectInfo(canvas)
//            drawThermal(canvas)
//            drawFaceInfo(canvas)
            postInvalidate()
        }
    }

    private fun updateTransformationIfNeeded() {
        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) {
            return
        }
        val viewAspectRatio = width.toFloat() / height
        val imageAspectRatio = imageWidth.toFloat() / imageHeight
        postScaleWidthOffset = 0f
        postScaleHeightOffset = 0f
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = width.toFloat() / imageWidth
            postScaleHeightOffset = (width.toFloat() / imageAspectRatio - height) / 2
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = height.toFloat() / imageHeight
            postScaleWidthOffset = (height.toFloat() * imageAspectRatio - width) / 2
        }

        transformationMatrix.reset()
        transformationMatrix.postRotate(imageRotation.toFloat(), imageWidth / 2f, imageHeight / 2f)
        transformationMatrix.postScale(scaleFactor, scaleFactor)
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset)
        if (isImageFlipped) {
            transformationMatrix.postScale(-1f, 1f, width / 2f, height / 2f)
        }
        needUpdateTransformation = false
    }

    private fun drawFaces(canvas: Canvas) {
        if (faces != null) {
            for (face in faces!!) {
                val box = face.box
                canvas.withMatrix(transformationMatrix) {
                    // left up corner
                    drawLine(box.x, box.y, box.x, box.y + faceBoxLineLength, faceBoxPaint)
                    drawLine(box.x, box.y, box.x + faceBoxLineLength, box.y, faceBoxPaint)
                    // right up corner
                    drawLine(box.x + box.width, box.y, box.x + box.width, box.y + faceBoxLineLength, faceBoxPaint)
                    drawLine(box.x + box.width, box.y, box.x + box.width - faceBoxLineLength, box.y, faceBoxPaint)
                    // left down corner
                    drawLine(box.x, box.y + box.height, box.x, box.y + box.height - faceBoxLineLength, faceBoxPaint)
                    drawLine(box.x, box.y + box.height, box.x + faceBoxLineLength, box.y + box.height, faceBoxPaint)
                    // right down corner
                    drawLine(box.x + box.width, box.y + box.height, box.x + box.width, box.y + box.height - faceBoxLineLength, faceBoxPaint)
                    drawLine(box.x + box.width, box.y + box.height, box.x + box.width - faceBoxLineLength, box.y + box.height, faceBoxPaint)

//                    drawRect(
//                        box.x,
//                        box.y,
//                        box.x + box.width,
//                        box.y + box.height,
//                        faceBoxPaint
//                    )
                }

            }
        }
    }

//    fun setDetectInfo(
//        framesPerSecond: Int,
//        avgLatency: Long,
//        maxRunMs: Long,
//        minRunMs: Long,
//        currentMs: Long,
//    ) {
//        detectInfo.fps = framesPerSecond
//        detectInfo.avgMs = avgLatency
//        detectInfo.maxMs = maxRunMs
//        detectInfo.minMs = minRunMs
//        detectInfo.currentMs = currentMs
//
//        // memory usage
//        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
//        val memoryInfo = ActivityManager.MemoryInfo()
//        activityManager?.getMemoryInfo(memoryInfo)
//        val mega = 1_048_576 // 1024 * 1024
//        val totalMem = memoryInfo.totalMem / mega
//        val availMem = memoryInfo.availMem / mega
//        val usedSize = totalMem - availMem
//        val usedSizePercent = 100 * usedSize / totalMem
//        detectInfo.freeSize = availMem
//        detectInfo.totalSize = totalMem
//        detectInfo.usedSize = usedSize
//        detectInfo.usedSizePercent = usedSizePercent
//    }

//    private fun changeTemperature(temperature: Float): Float {
//        return (temperature - 32) * 5 / 9
//    }

//    private fun drawTextLine(canvas: Canvas, text: String) {
//        canvas.drawText(text, 0f, textDrawLinePosition, textPaint)
//        textDrawLinePosition += 50
//    }
//
//    private fun drawDetectInfo(canvas: Canvas) {
//        if (detectInfo.totalSize > 0) {
//            drawTextLine(canvas,
//                "Memory: total ${detectInfo.totalSize}MB / free ${detectInfo.freeSize}MB / used ${detectInfo.usedSize}MB")
//            drawTextLine(canvas,
//                "Latency: Avg ${detectInfo.avgMs}ms / Max ${detectInfo.maxMs}ms / Min ${detectInfo.minMs}ms")
//            drawTextLine(canvas,
//                "FPS ${detectInfo.fps} / CurrentLatency: ${detectInfo.currentMs}ms")
//        }
//    }
//
//    private fun drawProximity(canvas: Canvas) {
//        if (faceProximity != null) {
//            drawTextLine(canvas,
//                "Proximity: maximum: ${faceProximity!!.maximum} / distance: ${faceProximity!!.distance}cm")
//        }
//    }
//
//    private fun drawPower(canvas: Canvas) {
//        if (facePower != null) {
//            drawTextLine(canvas, facePower!!.getStatusPower().toString())
//            drawTextLine(canvas, facePower!!.getStatusThermal().toString())
//        }
//    }
//
//    private fun drawThermal(canvas: Canvas) {
//        if (faceThermal != null) {
//            drawTextLine(canvas,
//                "Thermal: max ${faceThermal?.maxTemperature} / skin ${faceThermal?.skinTemperature}")
//        }
//    }
//
//    private fun drawFaceInfo(canvas: Canvas) {
//        if (faces != null) {
//            for (face in faces!!) {
//                val box = face.box
//                val pose = face.pose
//                drawTextLine(canvas,
//                    "Face: x ${box.x} y ${box.y} w ${box.width} h ${box.height} yaw ${pose.yaw_degree.toInt()} roll ${pose.roll_degree.toInt()} pitch ${pose.pitch_degree.toInt()}")
//                drawTextLine(canvas,"valid pose \t${FaceDetectLogic.isValidPose}")
//                drawTextLine(canvas,"valid position \t${FaceDetectLogic.isValidPosition}")
//                drawTextLine(canvas,"valid size min \t${FaceDetectLogic.isValidSizeMin}")
//                drawTextLine(canvas,"valid size max \t${FaceDetectLogic.isValidSizeMax}")
//                drawTextLine(canvas,"valid liveness \t${FaceDetectLogic.isLiveness}")
//                drawTextLine(canvas,"valid all pass \t${FaceDetectLogic.isFaceDeteced && FaceDetectLogic.isLiveness}")
//            }
//        }
//    }

    companion object {
        private const val TAG = "FaceGraphic"
    }

}