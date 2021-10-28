package com.alcherainc.nfc_certification_app.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.util.Log
import com.alcherainc.facesdk.FaceSDK
import com.alcherainc.facesdk.error.Error
import com.alcherainc.facesdk.type.*
import com.alcherainc.facesdk.type.AntispoofingExtension.InputIRImage
import com.alcherainc.facesdk.type.AttributeExtension.AlignedFaceImage
import com.alcherainc.facesdk.type.FeatureExtension.FaceFeature
import com.alcherainc.facesdk.type.FeatureExtension.InputAlignedFaceImage
import com.jhoonPark.flutter_playground.camera.FaceDetectConfig
import com.alcherainc.nfc_certification_app.camera.FaceSdk
import com.alcherainc.nfc_certification_app.util.ImageUtils
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.*
import kotlin.math.abs


object FaceDetectLogic {
    private const val TAG = "FaceDetectLogic"

    val faceDetectConfig: FaceDetectConfig = FaceDetectConfig()

    @get:Synchronized
    @set:Synchronized
    var skipFaceProcessor = false

    @get:Synchronized
    @set:Synchronized
    var rgbFaces: Array<Face>? = null

    //  cached in successProcess, to use in on-device face recognition
    @get:Synchronized
    @set:Synchronized
    var cachedRgbFaces: Array<Face>? = null

    @get:Synchronized
    @set:Synchronized
    var cachedRgbInputImage: InputImage? = null

    @get:Synchronized
    @set:Synchronized
    var rgbInputImage: InputImage? = null

    @get:Synchronized
    @set:Synchronized
    var irFaces: Array<Face>? = null

    @get:Synchronized
    @set:Synchronized
    var irInputImage: InputImage? = null

    @get:Synchronized
    @set:Synchronized
    var irInputDepthImage: InputIRImage? = null

    //  Aligned face image for detection
    @get:Synchronized
    @set:Synchronized
    var alignedFaceBitmapImage: Bitmap? = null

    @get:Synchronized
    @set:Synchronized
    var alignedFaceJpegImageBase64: String? = null

    //  For SDK research
    @get:Synchronized
    @set:Synchronized
    var fullscreenImageBitmap: Bitmap? = null
    var fullscreenImageBase64: String? = null

    //  3:4 Cropped image for display
    @get:Synchronized
    @set:Synchronized
    var croppedFaceImageBitmap: Bitmap? = null
    var croppedFaceImageBase64: String? = null

    var isFaceMaskSupport = true
    var isAntispoofingSupport = true

    var isFaceDeteced = false
    var isValidPosition = false
    var isValidSizeMin = false
    var isValidSizeMax = false
    var isValidPose = false
    var isLiveness = false
    var isFaceMaskUsage = false
    var valid = false
    var isLandmarkValid = false

    private val livenessQueue = LinkedList<Float>()

    data class Result(
        val isFaceDeteced: Boolean,
        val isValidPosition: Boolean,
        val isValidSizeMin: Boolean,
        val isValidSizeMax: Boolean,
        val isValidPose: Boolean,
        val isFaceMaskUsage: Boolean,
        val isLiveness: Boolean,
        val faceBitmapImage: Bitmap?,
    )

    data class MotionInteractionResult(
        val isFaceDeteced: Boolean,
        val isValidPosition: Boolean,
        val isValidSizeMin: Boolean,
        val isValidSizeMax: Boolean,
        val isValidPose: Boolean,
        val isFaceMaskUsage: Boolean,
        val isLiveness: Boolean,
        val faceBitmapImage: Bitmap?,
        val isLandmarkValid: Boolean,
        val faceDegree: Float,
        val targetDegree: Float,
        val isSuccess: Boolean
    )

    private val VALID_FACE_DETECT_CONFIDENCE_VALUE = 0.8f
    //  Previously selected motion interaction target degree
    private var prevTargetDegree: Float = 0f
    private var targetDegree: Float = 0f
    val DEGREE_TOLERANCE = 30f
    private var prevFaceDegree: Float = 0f

    @Synchronized
    fun init() {
        rgbFaces = null
        rgbInputImage = null
        irFaces = null
        irInputImage = null
        irInputDepthImage = null
        alignedFaceBitmapImage = null
        alignedFaceJpegImageBase64 = null
        isFaceDeteced = false
        isValidPosition = false
        isValidSizeMin = false
        isValidSizeMax = false
        isValidPose = false
        isLiveness = false
        isFaceMaskUsage = false
        valid = false
        livenessQueue.clear()
    }

    @get:Synchronized
    @set:Synchronized
    private var detectCallback: ((result: Result) -> Unit)? = null

    @Synchronized
    fun setDetectCallback(callback: (result: Result) -> Unit) {
        detectCallback = callback
    }

    @get:Synchronized
    @set:Synchronized
    private var motionAntispoofingCallback: ((result: MotionInteractionResult) -> Unit)? = null

    @Synchronized
    fun setMotionAntispoofingCallback(callback: (result: MotionInteractionResult) -> Unit) {
        motionAntispoofingCallback = callback
    }

    @get:Synchronized
    @set:Synchronized
    private var doesMotionInteractionProcess = false
    private var isFirstMotionInteractionState = true

    fun startMotionInteraction(): Boolean {
        if(isFirstMotionInteractionState) {
            isFirstMotionInteractionState = false
            prevTargetDegree = 90f
        }
        //  0~359
//        targetDegree = prevTargetDegree
//        while(Math.abs(targetDegree - prevTargetDegree) < 90f) {
//            targetDegree = Math.random().toFloat() * 360f
//        }
        //  Up-Down-Right-Left
        targetDegree = prevTargetDegree
        //  Do not use same value that used right before
        while(targetDegree == prevTargetDegree) {
            val random = SecureRandom()
            targetDegree = when(random.nextInt(360)) {
                in 45 until 135 -> 90f
                in 135 until 225 -> 180f
                in 225 until 315 -> 270f
                else -> 0f
            }
        }
        prevTargetDegree = targetDegree
        doesMotionInteractionProcess = true
        return doesMotionInteractionProcess
    }
    fun stopMotionInteraction() {
        isFirstMotionInteractionState = true
        doesMotionInteractionProcess = false
    }

    private fun getMotionDegreeUsingPose(yaw: Float, pitch: Float): Float {
        val yawRadian = Math.toRadians(yaw.toDouble())
        val pitchRadian = Math.toRadians(pitch.toDouble())
        Log.d(TAG, "Math.sin(pitchRadian): ${Math.sin(pitchRadian)}, Math.sin(yawRadian): ${Math.sin(yawRadian)}")
        val radian = Math.atan(Math.sin(pitchRadian) / Math.sin(yawRadian))
        var degree = Math.abs(Math.toDegrees(radian)).toInt()
        /*
         * -yaw +pitch : left bottom : -degree
         * +yaw +pitch : right bottom : +degree
         * +yaw -pitch : right top : -degree
         * -yaw -pitch : left top : +degree
         */
        if (yaw < 0 && pitch > 0) {
            // nothig
        } else if (yaw > 0 && pitch > 0) {
            degree = 180 - degree
        } else if (yaw > 0 && pitch < 0) {
            degree = 180 + degree
        } else if (yaw < 0 && pitch <= 0) {
            degree = 360 - degree
        } else if (yaw < 0 && pitch.toInt() == 0) {
            degree = 0
        } else if (yaw.toInt() == 0 && pitch > 0) {
            degree = 90
        } else if (yaw > 0 && pitch.toInt() == 0) {
            degree = 180
        } else if (yaw.toInt() == 0 && pitch < 0) {
            degree = 270
        } else {
            degree = 0
        }
        var lerpedDegree = prevFaceDegree * 0.5f + degree * 0.5f
        if(degree <= 0f)
            lerpedDegree = prevFaceDegree
        else if(Math.abs(prevFaceDegree - degree) > 270) {
            lerpedDegree = degree.toFloat()
        }
        prevFaceDegree = lerpedDegree
        Log.d(TAG, "raw degree: ${degree}, lerped degree: ${lerpedDegree}")
        return lerpedDegree
    }
    private fun getDegreeDifference(left: Float, right: Float): Float {
        val prevX = Math.cos(Math.toRadians(left.toDouble()))
        val prevY = Math.sin(Math.toRadians(left.toDouble()))
        val currX = Math.cos(Math.toRadians(right.toDouble()))
        val currY = Math.sin(Math.toRadians(right.toDouble()))
        return Math.toDegrees(Math.acos((prevX * currX) + (prevY * currY))).toFloat()
    }

    @Synchronized
    fun update() {
        if(skipFaceProcessor)
            return

        // try face detect
        if (rgbInputImage != null) {
            val faces = FaceSdk.getInstance().DetectFaceInSingleImage(rgbInputImage)
            rgbFaces = if (faces != null && faces.faces.isNotEmpty()) {
                isFaceDeteced = true
                faces.faces
            } else {
                isFaceDeteced = false
                null
            }
        } else {
            rgbFaces = null
        }
        // check valid face
        if (rgbInputImage != null && rgbFaces != null) {
            val pose = rgbFaces!![0].pose
            val pitch = pose.pitch_degree
            val yaw = pose.yaw_degree
            val roll = pose.roll_degree
            isValidPose = abs(pitch) < faceDetectConfig.VALID_PITCH_DEGREE &&
                    abs(yaw) < faceDetectConfig.VALID_YAW_DEGREE &&
                    abs(roll) < faceDetectConfig.VALID_ROLL_DEGREE

            val face = rgbFaces!![0].box
            val imageW = rgbInputImage!!.width
            val imageH = rgbInputImage!!.height
            val imageCenterX = imageW / 2
            val imageCenterY = imageH / 2
            val faceCenterX = face.x + (face.width / 2)
            val faceCenterY = face.y + (face.height / 2)
            val centerDiffX = abs(imageCenterX - faceCenterX)
            val centerDiffY = abs(imageCenterY - faceCenterY)
            isValidPosition = if (faceDetectConfig.VALID_FACE_POSITION_RATE != 0) {
                val widthRatio = imageW / faceDetectConfig.VALID_FACE_POSITION_RATE
                val heightRatio = imageH / faceDetectConfig.VALID_FACE_POSITION_RATE
                centerDiffX < widthRatio && centerDiffY < heightRatio
            } else {
                true
            }
            if (imageW > imageH) {
                faceDetectConfig.VALID_FACE_SIZE_MIN_RATE = faceDetectConfig.VALID_FACE_SIZE_MIN_RATE_ORIENTATION_LANDSCAPE
                faceDetectConfig.VALID_FACE_SIZE_MAX_RATE = faceDetectConfig.VALID_FACE_SIZE_MAX_RATE_ORIENTATION_LANDSCAPE
            } else {
                faceDetectConfig.VALID_FACE_SIZE_MIN_RATE = faceDetectConfig.VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT
                faceDetectConfig.VALID_FACE_SIZE_MAX_RATE = faceDetectConfig.VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT

            }
            isValidSizeMin = if (faceDetectConfig.VALID_FACE_SIZE_MIN_RATE != 0.0f) {
                imageW / faceDetectConfig.VALID_FACE_SIZE_MIN_RATE < face.width
            } else {
                true
            }
            isValidSizeMax = face.width < imageW / faceDetectConfig.VALID_FACE_SIZE_MAX_RATE

            valid = isFaceDeteced && isValidPosition && isValidSizeMin && isValidSizeMax && isValidPose

//            // check face mouth occlusion
//            if (valid && isFaceMaskSupport) {
//                val faceOcclusion = FaceSdk.getAttributeExtensionInstance()
//                    .DetectOcclusion(rgbInputImage, rgbFaces!![0])
//                if (faceOcclusion.last_error == Error.NoError) {
//                    val occlusion = faceOcclusion.occlusion
////                    val leftEye = ((occlusion.left_eye_occlusion_confidence * 1000).toInt()) / 1000f
////                    val rightEye = ((occlusion.right_eye_occlusion_confidence * 1000).toInt()) / 1000f
//                    val mouth = ((occlusion.mouth_occlusion_confidence * 1000).toInt()) / 1000f
//                    isFaceMaskUsage = mouth > VALID_FACE_MASK_USAGE_RATE
//                    Log.d(TAG, "AttributeExtensionInstance.DetectOcclusion mouth $mouth")
//                } else {
//                    Log.d(TAG, "AttributeExtension.DetectOcclusion error ${faceOcclusion.last_error}")
//                    isFaceMaskUsage = false
//                }
//            } else {
//                isFaceMaskUsage = false
//            }

            // check face mask usage
            if (valid && isFaceMaskSupport) {
                val faceMask = FaceSdk.getAttributeExtensionInstance()
                    .CheckMask(rgbInputImage, rgbFaces!![0])
                if (faceMask.last_error == Error.NoError) {
                    val mask = faceMask.mask
//                    val leftEye = ((occlusion.left_eye_occlusion_confidence * 1000).toInt()) / 1000f
//                    val rightEye = ((occlusion.right_eye_occlusion_confidence * 1000).toInt()) / 1000f
                    //  (0f~1f) 0: Use mask, 1: No mask
                    val maskConfidence = ((mask.confidence * 1000).toInt()) / 1000f
                    isFaceMaskUsage = maskConfidence < faceDetectConfig.VALID_FACE_MASK_USAGE_RATE
                    Log.d(TAG, "AttributeExtensionInstance.CheckMask maskConfidence $maskConfidence")
                } else {
                    Log.d(TAG, "AttributeExtension.CheckMask error ${faceMask.last_error}")
                    isFaceMaskUsage = false
                }
            } else {
                isFaceMaskUsage = false
            }

//            // check face mouth occlusion & mask usage
//            if (valid && isFaceMaskSupport) {
//                var isFaceOccluded = false
//                val faceOcclusion = FaceSdk.getAttributeExtensionInstance()
//                    .DetectOcclusion(rgbInputImage, rgbFaces!![0])
//                if (faceOcclusion.last_error == Error.NoError) {
//                    val occlusion = faceOcclusion.occlusion
////                    val leftEye = ((occlusion.left_eye_occlusion_confidence * 1000).toInt()) / 1000f
////                    val rightEye = ((occlusion.right_eye_occlusion_confidence * 1000).toInt()) / 1000f
//                    val mouth = ((occlusion.mouth_occlusion_confidence * 1000).toInt()) / 1000f
//                    isFaceOccluded = mouth > VALID_FACE_MASK_USAGE_RATE
//                    Log.d(TAG, "AttributeExtensionInstance.DetectOcclusion mouth $mouth")
//                } else {
//                    Log.d(TAG, "AttributeExtension.DetectOcclusion error ${faceOcclusion.last_error}")
//                    isFaceOccluded = false
//                }
//
//                var isFaceUseMask = false
//                val faceMask = FaceSdk.getAttributeExtensionInstance()
//                    .CheckMask(rgbInputImage, rgbFaces!![0])
//                if (faceMask.last_error == Error.NoError) {
//                    val mask = faceMask.mask
////                    val leftEye = ((occlusion.left_eye_occlusion_confidence * 1000).toInt()) / 1000f
////                    val rightEye = ((occlusion.right_eye_occlusion_confidence * 1000).toInt()) / 1000f
//                    //  (0f~1f) 0: Use mask, 1: No mask
//                    val maskConfidence = ((mask.confidence * 1000).toInt()) / 1000f
//                    isFaceUseMask = maskConfidence < VALID_FACE_MASK_USAGE_RATE
//                    Log.d(TAG, "AttributeExtensionInstance.CheckMask maskConfidence $maskConfidence")
//                } else {
//                    Log.d(TAG, "AttributeExtension.CheckMask error ${faceMask.last_error}")
//                    isFaceUseMask = false
//                }
//
//                isFaceMaskUsage = (isFaceOccluded || isFaceUseMask)
//            } else {
//                isFaceMaskUsage = false
//            }

            // check liveness
            if (irInputImage != null && valid) {
                val faces = FaceSdk.getInstance().DetectFaceInSingleImage(irInputImage)
                irFaces = if (faces != null && faces.faces.isNotEmpty()) {
                    faces.faces
                } else {
                    null
                }
                if (irFaces != null && irFaces!!.isNotEmpty()) {
                    val face = irFaces!![0]
                    if (irInputDepthImage != null) {
                        val depthImageLiveness = FaceSdk.getAntispoofingExtensionInstance()
                            .CheckLivenessFromIRImage(irInputDepthImage, face)
                        if (depthImageLiveness.last_error == Error.NoError) {
                            Log.d(TAG, "AntispoofingExtension.CheckLivenessFromDepthImage confidence ${depthImageLiveness.confidence}")
                            // check valid confidence logic for liveness true.
                            // because There are times when there is a is_live true for a fake face.
                            if (livenessQueue.size == faceDetectConfig.VALID_FACE_LIVENESS_QUEUE_SIZE) {
                                livenessQueue.removeFirst()
                            }
                            livenessQueue.add(depthImageLiveness.confidence)
                            if (livenessQueue.size == faceDetectConfig.VALID_FACE_LIVENESS_QUEUE_SIZE) {
                                val sum = livenessQueue.reduce { acc, value -> acc + value }
                                val avg = sum / faceDetectConfig.VALID_FACE_LIVENESS_QUEUE_SIZE
                                isLiveness = avg > faceDetectConfig.VALID_FACE_LIVENESS_CONFIDENCE_VALUE
                                Log.d(TAG, "AntispoofingExtension.CheckLivenessFromDepthImage confidence ${avg} avg isLiveness $isLiveness")
                            }
                            if (!isLiveness) {
                                isLiveness = depthImageLiveness.confidence > faceDetectConfig.VALID_FACE_LIVENESS_EXACTLY_CONFIDENCE_VALUE
                            }
                            if (isLiveness) {
                                successProcess()
                            }
                        } else {
                            isLiveness = false
                            livenessQueue.clear()
                            Log.d(TAG, "AntispoofingExtension.CheckLivenessFromDepthImage error ${depthImageLiveness.last_error}")
                        }
                    }
                } else {
                    livenessQueue.clear()
                    isLiveness = false
                }
            } else if (!isAntispoofingSupport && valid) {
                successProcess()
            }
        } else {
            livenessQueue.clear()
            valid = false
        }

        if(doesMotionInteractionProcess) {
            var faceDegree = -1f
            if(rgbFaces != null && rgbFaces != null) {
                //  Pose validation
                isValidPose = Math.abs(rgbFaces!![0].pose.roll_degree) < 15f
                faceDegree = getMotionDegreeUsingPose(rgbFaces!![0].pose.yaw_degree, rgbFaces!![0].pose.pitch_degree)
                val landmarkConfidence = FaceSdk.getInstance().ComputeLandmarkConfidence(rgbInputImage, rgbFaces!![0])
                //Log.d(TAG, "landmark confidence ${landmarkConfidence.confidence}")
                //  CONFIDENCE VALUE IS UNSTABLE IN MASK FACE SO DO NOT USE IN MASK FACE
                isLandmarkValid = !isFaceMaskUsage && landmarkConfidence.confidence >= VALID_FACE_DETECT_CONFIDENCE_VALUE
            } else {
                isLandmarkValid = false
                isValidPose = false
            }
            motionAntispoofingCallback?.let {
                it(MotionInteractionResult(
                    isFaceDeteced,
                    isValidPosition,
                    isValidSizeMin,
                    isValidSizeMax,
                    isValidPose,
                    isFaceMaskUsage,
                    isLiveness,
                    alignedFaceBitmapImage,
                    isLandmarkValid,
                    faceDegree,
                    targetDegree,
                    getDegreeDifference(faceDegree, targetDegree) < DEGREE_TOLERANCE
                )
                )
            }
        } else {
            detectCallback?.let {
                it(
                    Result(
                        isFaceDeteced,
                        isValidPosition,
                        isValidSizeMin,
                        isValidSizeMax,
                        isValidPose,
                        isFaceMaskUsage,
                        isLiveness,
                        alignedFaceBitmapImage,
                    )
                )
            }
        }
    }

    private fun successProcess() {
//        val landmarkConfidence = FaceSdk.getInstance().ComputeLandmarkConfidence(this.rgbInputImage, this.rgbFaces!![0])
//        Log.d(TAG, "successProcess confidence ${landmarkConfidence.confidence}")
    //        if (this.isFaceMaskSupport || landmarkConfidence.confidence >= VALID_FACE_DETECT_CONFIDENCE_VALUE) {
        cachedRgbFaces = rgbFaces!!.clone()
        cachedRgbInputImage = InputImage().apply {
            bgr_image_buffer = rgbInputImage!!.bgr_image_buffer
            width = rgbInputImage!!.width
            height = rgbInputImage!!.height
        }

        isFaceDeteced = true

        //  Aligned face image for detection
        alignedFaceBitmapImage = makeAlignFaceBitmap()
        alignedFaceJpegImageBase64 = ImageUtils.makeJpegBase64(alignedFaceBitmapImage!!)
        Log.d(TAG, "Image ${rgbInputImage!!.width}x${rgbInputImage!!.height} Face ${rgbFaces!![0].box.width}x${rgbFaces!![0].box.height}")

        if(faceDetectConfig.currentConfigType == FaceDetectConfig.CONFIG_TYPE.REGISTER) {
            fullscreenImageBase64 = ImageUtils.makeJpegBase64(fullscreenImageBitmap!!)
            rgbFaces?.let {
                //  WARNING: rgbFaces will be change if do shallow copy & change copied instance's member values
                val rgbFace = Face()
                rgbFace.box = Box()
                rgbFace.box.x = it[0].box.x
                rgbFace.box.y = it[0].box.y
                rgbFace.box.width = it[0].box.width
                rgbFace.box.height = it[0].box.height

                rgbFace.id = it[0].id
                rgbFace.landmark = Landmark()
                rgbFace.pose = Pose()

                val ratio = 3f/4f   //  width/height

                val originWidth = rgbFace.box.width
                val originHeight = rgbFace.box.height
                if(rgbFace.box.width > rgbFace.box.height) {
                    rgbFace.box.height = rgbFace.box.width / ratio
                } else {
                    rgbFace.box.width = rgbFace.box.height * ratio
                }

                val SCALE = 1.8f
                rgbFace.box.width *= SCALE
                rgbFace.box.height *= SCALE

                if(rgbFace.box.width < 112 || rgbFace.box.height < 112) {
                    rgbFace.box.width = 112f
                    rgbFace.box.height = 112f / ratio
                }

                val translation_x = (rgbFace.box.width - originWidth) * 0.5f
                val translation_y = (rgbFace.box.height - originHeight) * 0.5f
                rgbFace.box.x = Math.max(rgbFace.box.x - translation_x, 0f)
                rgbFace.box.y = Math.max(rgbFace.box.y - translation_y, 0f)

                val faceBitmap = FaceClip.getFaceBitmap(fullscreenImageBitmap!!, rgbFace)
                croppedFaceImageBitmap = faceBitmap!!
                croppedFaceImageBase64 = ImageUtils.makeJpegBase64(faceBitmap!!)
            }
        }

//        } else {
//            this.isFaceDeteced = false
//        }
    }

    fun getFaceBitmap(bitmap: Bitmap, face: Face): Bitmap {
        val cropBitmap = bitmap
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

    private fun makeAlignFaceBitmap(): Bitmap {
        val alignedFaceImage = FaceSdk.getAttributeExtensionInstance()
            .AlignFaceImage(rgbInputImage, rgbFaces!![0])
        val bgrBuffer = alignedFaceImage.bgr_image_buffer!!
        return makeBitmapImage(bgrBuffer, AlignedFaceImage.kWidth, AlignedFaceImage.kHeight)
    }

    private fun makeBitmapImage(rgb888: ByteArray, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val colors = IntArray(width * height)
        var r: Int
        var g: Int
        var b: Int
        for (ci in colors.indices) {
            r = (0xFF and rgb888[3 * ci + 2].toInt())
            g = (0xFF and rgb888[3 * ci + 1].toInt())
            b = (0xFF and rgb888[3 * ci].toInt())
            colors[ci] = Color.rgb(r, g, b)
        }
        bitmap.setPixels(colors, 0, width, 0, 0, width, height)
        return bitmap
    }
}