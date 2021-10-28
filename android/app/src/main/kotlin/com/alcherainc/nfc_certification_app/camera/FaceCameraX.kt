package com.alcherainc.nfc_certification_app.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alcherainc.facesdk.type.InputImage
import com.alcherainc.nfc_certification_app.util.Device
import com.alcherainc.nfc_certification_app.util.ImageUtils
import com.alcherainc.nfc_certification_app.util.LED
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors


class FaceCameraX(
    private val activity: ComponentActivity,
//    private val previewViewWrapperView: FrameLayout,
//    private val previewView: PreviewView,
    private val isThermalSupport: Boolean,
    private val isFaceMaskSupport: Boolean,
    private val isAntispoofingSupport: Boolean,
//    private val faceGraphicView: FaceGraphicView?,
    heatmapView: FrameLayout?,
    faceClipView: ImageView?,
    faceIRClipView: ImageView?,
) : FaceCamera() {
    private var RGB_CAMERA_SELECTOR: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var IR_CAMERA_SELECTOR: Int = CameraMetadata.LENS_FACING_FRONT

    init {
        Log.d(TAG, "Options isThermal $isThermalSupport isFaceMaskSupport $isFaceMaskSupport isAntispoofingSupport $isAntispoofingSupport")
        if (Device.TYPE == Device.DONGA) {
            RGB_CAMERA_SELECTOR = CameraSelector.DEFAULT_FRONT_CAMERA
            IR_CAMERA_SELECTOR = CameraMetadata.LENS_FACING_BACK
        } else if (Device.TYPE == Device.HLDS) {
            RGB_CAMERA_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA
            IR_CAMERA_SELECTOR = CameraMetadata.LENS_FACING_FRONT
        } else if (Device.TYPE == Device.PHONE) {
            RGB_CAMERA_SELECTOR = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    private val faceProcessor = FaceProcessorCameraX(activity, faceClipView)
    private val faceProcessorIR = FaceProcessorIR(activity, faceIRClipView)

//    private val faceProximity = FaceProximity(activity)
//    private val facePower = FacePower(activity)
    var faceThermal: FaceThermal? = null

    init {
        if (isThermalSupport) {
            faceThermal = FaceThermal(activity, heatmapView)
        }
        LED.init()
        LED.on(LED.TYPE.WHITE)
        FaceDetectLogic.init()
        FaceDetectLogic.isFaceMaskSupport = isFaceMaskSupport
        if (Device.TYPE == Device.PHONE) {
            FaceDetectLogic.isAntispoofingSupport = false
        } else {
            FaceDetectLogic.isFaceMaskSupport = isFaceMaskSupport
            FaceDetectLogic.isAntispoofingSupport = isAntispoofingSupport
        }
    }

    init {
        //faceProcessor.faceGraphicView = faceGraphicView
        faceProcessor.faceThermal = faceThermal
        //faceGraphicView?.setCameraSelector(RGB_CAMERA_SELECTOR)
//        faceGraphicView?.faceProximity = faceProximity
//        faceGraphicView?.facePower = facePower
//        faceGraphicView?.faceThermal = faceThermal

//        faceProximity.setStatusChangeListener {
//            Log.d(TAG, "faceProximity.setStatusChangeListener ${it.toString()}")
//            if (it <= 0f) {
//                stop()
//            } else {
//                start()
//            }
//        }
    }


    private val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var lensFace = 0
    private var orientation = 0
    private var cameraProvider: ProcessCameraProvider? = null
//    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var irCameraId: String = ""
    private var irCameraSession: CameraCaptureSession? = null;
    private var irCameraThread: HandlerThread? = null
    private var irCameraCaptureThread: HandlerThread? = null
    private var irCameraDeviceLevel: Int? = 0

    init {
        initializeCameraInfo()
        if (isAntispoofingSupport && Device.TYPE != Device.PHONE) {
            openIRCamera()
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val cameraExecutor = Executors.newSingleThreadExecutor()

            if (Device.TYPE == Device.DONGA) {
                // DONA전기의 카메라의 경우 device에서 resize가 안된 상태로 들어옴. 때문에 강제로 사이즈 조정해야 함.
//                previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
//                previewView.scaleX = RGB_CAMERA_SIZE.height.toFloat() / RGB_CAMERA_SIZE.width.toFloat()
//                previewView.scaleY = RGB_CAMERA_SIZE.width.toFloat() / RGB_CAMERA_SIZE.height.toFloat()
//
//                val metrics = DisplayMetrics().also {
//                    previewView.display.getRealMetrics(it)
//                }
//                val scaleX = previewView.scaleY
//                previewViewWrapperView.scaleX = scaleX
//                previewViewWrapperView.scaleY = scaleX
//                val graphicScaleX = metrics.widthPixels.toFloat() / RGB_CAMERA_SIZE.height.toFloat()
//                Log.d(TAG, "display.getRealMetrics $metrics")
//                faceGraphicView?.scaleX = graphicScaleX
//                faceGraphicView?.scaleY = graphicScaleX
            }
            val previewBuilder = Preview.Builder().also {
                it.setTargetAspectRatio(AspectRatio.RATIO_16_9)
            }
//            preview = previewBuilder.build().also {
//                it.setSurfaceProvider(previewView.surfaceProvider)
//            }
            imageAnalyzer = ImageAnalysis.Builder().also {
                it.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                it.setTargetResolution(RGB_CAMERA_SIZE)
            }.build().also {
                it.setAnalyzer(cameraExecutor!!, faceProcessor)
                it.targetRotation = Surface.ROTATION_0
            }
            try {
                checkEmulator()
                // Unbind use cases before rebinding
                cameraProvider!!.unbindAll()
                // Bind use cases to camera
                cameraProvider!!.bindToLifecycle(
                    activity,
                    RGB_CAMERA_SELECTOR,
//                    preview,
                    imageAnalyzer
                )
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } catch (e: Exception) {
                Log.e(TAG, "camera Use case binding failed $e")
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    @SuppressLint("RestrictedApi")
    private fun checkEmulator() {
        if (manager.cameraIdList.size == 1 && Device.TYPE == Device.PHONE) { // emulator
            RGB_CAMERA_SELECTOR = if (lensFace == CameraMetadata.LENS_FACING_FRONT) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
//            faceGraphicView?.setCameraSelector(RGB_CAMERA_SELECTOR)
            setRotation(CameraOrientationUtil.degreesToSurfaceRotation(orientation))
        }
    }

    override fun start(flipRgbCamera: Boolean) {
        if (cameraProvider != null && imageAnalyzer != null) {
            LED.init()
            LED.on(LED.TYPE.WHITE)
            // Unbind use cases before rebinding
            cameraProvider!!.unbindAll()
            if (!cameraProvider!!.isBound(imageAnalyzer!!)) {
                try {
                    if (flipRgbCamera) {
                        RGB_CAMERA_SELECTOR = if (RGB_CAMERA_SELECTOR == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    }
                    // Bind use cases to camera
                    cameraProvider!!.bindToLifecycle(
                        activity,
                        RGB_CAMERA_SELECTOR,
                        imageAnalyzer
                    )
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    if (isAntispoofingSupport && Device.TYPE != Device.PHONE) {
                        closeIRCamera()
                        openIRCamera()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open camera: $e")
                    if (isAntispoofingSupport && Device.TYPE != Device.PHONE) {
                        closeIRCamera()
                    }
                }
            }
        }
    }

    override fun stop() {
        LED.dispose()
        faceProcessor.stop()
        if (cameraProvider != null) {
            cameraProvider!!.unbindAll()
        }

        closeIRCamera()
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        faceThermal?.stop()
    }

    override fun setRotation(rotation: Int) {
        imageAnalyzer?.targetRotation = rotation
    }

    override fun initializeCameraInfo() {
        irCameraId = ""
        /*
         * 아래 characteristics에서 가져온 SCALER_STREAM_CONFIGURATION_MAP에서 size에서 가능한 사이즈를 보고 최적화하여 선택한다.
         */
        for (cameraId in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val size =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.getOutputSizes(ImageFormat.YUV_420_888)
            val DEPTH_IS_EXCLUSIVE = characteristics.get(CameraCharacteristics.DEPTH_DEPTH_IS_EXCLUSIVE)
            val ORIENTATION = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            orientation = ORIENTATION
            val HARDWARE_LEVEL =
                characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) // 0 - limited, 1 - full, 2 - legacy, 3 - uber full
            val cameraDeviceCapability = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: IntArray(0)
            val LENS_DISTORTION = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                characteristics.get(CameraCharacteristics.LENS_DISTORTION)
            } else {
                null
            }
            val INTRINSIC_CALIBRATION = characteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
            val RADIAL_DISTORTION = characteristics.get(CameraCharacteristics.LENS_RADIAL_DISTORTION)
            val LENS_FACING = characteristics.get(CameraCharacteristics.LENS_FACING)
            lensFace = LENS_FACING!!
            var cameraName: String
            if (LENS_FACING == IR_CAMERA_SELECTOR) {
                // IR Camera
                irCameraId = cameraId
                faceProcessorIR.orientation = ORIENTATION
                irCameraDeviceLevel = HARDWARE_LEVEL
                cameraName = "IR"
            } else {
                cameraName = "RGB"
            }
            Log.d(TAG, "$cameraName Camera cameraId $cameraId LENS $LENS_FACING")
            Log.d(TAG, "$cameraName Camera available size ${size?.joinToString(",")}")
            Log.d(TAG, "$cameraName Camera orientation $ORIENTATION level $HARDWARE_LEVEL depthExclusive $DEPTH_IS_EXCLUSIVE")
            Log.d(TAG, "$cameraName Camera capability ${cameraDeviceCapability.joinToString(",")}")
            Log.d(TAG, "$cameraName Camera distortion $LENS_DISTORTION calibration $INTRINSIC_CALIBRATION radial_distortion $RADIAL_DISTORTION")
        }
    }

    override fun openIRCamera() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED && irCameraId.isNotEmpty()
        ) {
            Log.d(TAG, "irCamera openCamera")
            manager.openCamera(
                irCameraId,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        Log.d(TAG, "irCamera opened")
                        val imageReader = ImageReader.newInstance(IR_CAMERA_SIZE.width, IR_CAMERA_SIZE.height, ImageFormat.YUV_420_888, 3)
                        irCameraThread?.quitSafely()
                        irCameraThread = HandlerThread("IR Camera")
                        irCameraThread!!.start()
                        val handler = Handler(irCameraThread!!.looper)
                        imageReader.setOnImageAvailableListener(faceProcessorIR, handler)
                        camera.createCaptureSession(
                            mutableListOf(imageReader.surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    try {
                                        irCameraSession = session
                                        val requestBuilder =
                                            session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                        requestBuilder.addTarget(imageReader.surface)

                                        irCameraCaptureThread?.quitSafely()
                                        irCameraCaptureThread = HandlerThread("IR Camera")
                                        irCameraCaptureThread!!.start()
                                        session.setRepeatingRequest(
                                            requestBuilder.build(),
                                            null,
                                            Handler(irCameraCaptureThread!!.looper)
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "CameraCaptureSession onConfigured threw", e)
                                    }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    Log.d(TAG, "CameraCaptureSession onConfigureFailed")
                                    stop()
                                }

                            },
                            null
                        )
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        Log.d(TAG, "irCamera disconnected")
                        stop()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.d(TAG, "irCamera error $error")
                    }
                }, null
            )
        }
    }

    override fun closeIRCamera() {
        if (irCameraSession != null) {
            irCameraSession?.close()
            irCameraSession = null
            /*
             * 강제로 하드웨어 타입이 레거시일 경우 camera2 api에서 release가 안되는 문제가 있음.
             * 언제 해결될진 모르나 강제로 카메라를 가져와서 release하는 방어코드를 추가
             */
            if (irCameraDeviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                if (irCameraId.isNotEmpty()) {
                    try {
                        val irCameraLegacy = Camera.open(irCameraId.toInt())
                        irCameraLegacy?.release()
                    } catch (e: Exception) {
                        Log.d(TAG, "irCameraLegacy throw $e")
                    }
                }
            }
        }
    }

    override fun getTemperature(callback: (Float) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                var temperature = 0.0f
                if (faceThermal?.isStart!!) {
                    while (true) { // temperature 못가져오는 경우 있음 갸져올 때까지 일단 계속 요청
                        faceThermal?.setFaceInfo(RGB_CAMERA_SIZE.width, RGB_CAMERA_SIZE.height, FaceDetectLogic.rgbFaces!![0])
                        temperature = faceThermal?.skinTemperature!!
                        if (temperature > 0) {
                            callback(temperature)
                            break
                        }
                    }
                } else {
                    callback(temperature)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class FaceProcessorCameraX(
        private val activity: Activity,
        private val faceClipView: ImageView?
    ) : ImageAnalysis.Analyzer {

        var faceGraphicView: FaceGraphicView? = null
        var faceThermal: FaceThermal? = null
        var direction = 0

        private val yuvToRgbConverter = YuvToRgbConverter(activity)

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {
//        setStatisticStart()
            if (imageProxy.image != null) {
                val bitmap = if (Device.TYPE == Device.DONGA) {
                    // DONA전기의 카메라의 경우 device에서 resize가 안된 상태로 들어옴. 때문에 강제로 사이즈 조정해야 함.
                    yuvToRgbConverter.getImageToBitmap(
                        imageProxy.image!!,
                        imageProxy.imageInfo.rotationDegrees,
                        imageProxy.image!!.height,
                        imageProxy.image!!.width
                    )
                } else { // HLDS, PHONE
                    yuvToRgbConverter.getImageToBitmap(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees, null, null)
                }
                val inputImage = InputImage()
                inputImage.width = bitmap.width
                inputImage.height = bitmap.height
                inputImage.bgr_image_buffer = ImageUtils.getImagePixels(bitmap)
                FaceDetectLogic.rgbInputImage = inputImage
                FaceDetectLogic.fullscreenImageBitmap = bitmap
                FaceDetectLogic.update()

                faceGraphicView?.setInputImage(inputImage, FaceDetectLogic.rgbFaces)
                if (faceClipView != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        faceClipView.setImageBitmap(bitmap)
                    }
                }
//        onDrawFaceInfo(FaceDetectLogic.rgbFaces)
            }
            imageProxy.close()
        }

        // Frame count that have been processed so far in an one second interval to calculate FPS.
//    private val fpsTimer = timer("FPS Timer", false, 0, 1000) {
//        framesPerSecond = frameProcessedInOneSecondInterval
//        frameProcessedInOneSecondInterval = 0
//
//        // thermal
//        if (faceThermal != null && faceGraphicView != null && faceGraphicView?.faces != null) {
//            val face = faceGraphicView!!.faces!![0]
//            faceThermal?.setFaceInfo(
//                FaceCamera.RGB_CAMERA_SIZE.width,
//                FaceCamera.RGB_CAMERA_SIZE.height,
//                face
//            )
//        }
//    }
//
//    private var frameProcessedInOneSecondInterval = 0
//    private var framesPerSecond = 0
//    private var numRuns = 0
//    private var totalRunMs: Long = 0
//    private var maxRunMs: Long = 0
//    private var minRunMs = Long.MAX_VALUE
//    private var startMs = SystemClock.elapsedRealtime()
//
        fun stop() {
//        fpsTimer.cancel()
        }
//
//    fun onDrawFaceInfo(rgbFaces: Array<Face>?) {
//        if (rgbFaces != null && rgbFaces!!.size > 0) {
//            setStatisticEnd(rgbFaces)
//        }
//    }
//
//    private fun setStatisticStart() {
//        if (faceGraphicView != null) {
//            startMs = SystemClock.elapsedRealtime()
//            frameProcessedInOneSecondInterval++
//        } else {
//            stop()
//        }
//    }
//
//    private fun setStatisticEnd(faces: Array<Face>?) {
//        val currentMs = SystemClock.elapsedRealtime() - startMs
//        val avgLatency = if (numRuns == 0) 0 else totalRunMs / numRuns
//        val faceCount = faces!!.size
//        if (faceCount > 0) {
//            if (faceGraphicView != null) {
//                faceGraphicView!!.faces = faces
//                numRuns++
//                totalRunMs += currentMs
//                maxRunMs = max(currentMs, maxRunMs)
//                minRunMs = min(currentMs, minRunMs)
//            }
//        } else {
//            faceGraphicView?.faces = null
//        }
//        faceGraphicView?.setDetectInfo(
//            framesPerSecond,
//            avgLatency,
//            maxRunMs,
//            minRunMs,
//            currentMs
//        )
//    }

        private fun makeJpegBase64(bitmap: Bitmap): String {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }

        companion object {
            private val TAG = FaceProcessorCameraX::class.java.simpleName
        }
    }

    companion object {
        val TAG = FaceCameraX::class.java.simpleName

        // 최적화하기 위해 조정가능
        val RGB_CAMERA_SIZE = if (Device.TYPE == Device.DONGA) {
            Size(1280, 720)
        } else if(Device.TYPE == Device.HLDS) { // HLDS
            //Size(800, 600)
            Size(720, 1280)
        } else if(Device.TYPE == Device.PHONE)  {
            Size(1080, 1920)
//            val faceDetectConfig = BuildConfig.FaceDetectConfig
//            if(faceDetectConfig.contains("walkthrough")) {
//                Size(1080, 1920)
//            }
//            else if(faceDetectConfig.contains("fittight")) {
//                Size(720, 1280)
//            }
//            else {
//                Size(1080, 1920)
//            }
        } else {
            Size(1280, 720)
        }

        val IR_CAMERA_SIZE = if (Device.TYPE == Device.DONGA) {
            Size(1024, 768)
        } else { // HLDS
            Size(800, 600)
        }
    }
}