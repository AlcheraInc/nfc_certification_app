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
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.alcherainc.facesdk.type.InputImage
import com.alcherainc.nfc_certification_app.util.Device
import com.alcherainc.nfc_certification_app.util.ImageUtils
import com.alcherainc.nfc_certification_app.util.LED
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class FaceCamera2(
    private val activity: ComponentActivity,

    private val isThermalSupport: Boolean,
    private val isFaceMaskSupport: Boolean,
    private val isAntispoofingSupport: Boolean,
    private val faceGraphicView: FaceGraphicView?,
    heatmapView: FrameLayout?,
    faceClipView: ImageView?,
    faceIRClipView: ImageView?
) : FaceCamera() {
    private var RGB_CAMERA_SELECTOR: Int = CameraMetadata.LENS_FACING_FRONT
    private var IR_CAMERA_SELECTOR: Int = CameraMetadata.LENS_FACING_FRONT

    init {
        Log.d(TAG, "Options isThermal $isThermalSupport isFaceMaskSupport $isFaceMaskSupport isAntispoofingSupport $isAntispoofingSupport")
        if (Device.TYPE == Device.DONGA) {
            RGB_CAMERA_SELECTOR = CameraMetadata.LENS_FACING_FRONT
            IR_CAMERA_SELECTOR = CameraMetadata.LENS_FACING_BACK
        } else if (Device.TYPE == Device.HLDS) {
            RGB_CAMERA_SELECTOR = CameraMetadata.LENS_FACING_BACK
            IR_CAMERA_SELECTOR = CameraMetadata.LENS_FACING_FRONT
        } else if (Device.TYPE == Device.PHONE) {
            RGB_CAMERA_SELECTOR = CameraMetadata.LENS_FACING_FRONT
        }
    }

    private val faceProcessor = FaceProcessorCamera2(activity, faceClipView)
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
        faceProcessor.faceGraphicView = faceGraphicView
        faceGraphicView?.setCameraSelector(RGB_CAMERA_SELECTOR)
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

    //  RGB Camera2
    private var rgbCameraId: String = ""
    private var rgbCameraDeviceLevel: Int? = 0
    private var rgbCameraDevice: CameraDevice? = null
    private var rgbImageReader: ImageReader? = null
    private var rgbCameraSession: CameraCaptureSession? = null;
    private var rgbCameraThread: HandlerThread? = null
    private var rgbCameraCaptureThread: HandlerThread? = null
    //  Recorder
    private val VIDEO_OUTPUT_DIR_NAME = "face_record"
    private fun videoOutputName(): String { return "FACE_VID_${SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date())}.mp4" }
    private var currentRecorderOutputFile: File? = null

    private fun startDetectionRequest() {
        //  Face detection
        rgbCameraThread?.quitSafely()
        rgbCameraThread = HandlerThread("RGB ImageReader Camera2")
        rgbCameraThread!!.start()
        val handler = Handler(rgbCameraThread!!.looper)
        rgbImageReader!!.setOnImageAvailableListener(faceProcessor, handler)

        val captureRequest = rgbCameraSession!!.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(rgbImageReader!!.surface)
        }
        rgbCameraSession!!.setRepeatingRequest(
            captureRequest.build(),
            null,
            handler
        )
    }

    //  Video recorder
    private fun startRecorderRequest() {
        val captureRequest = rgbCameraSession!!.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            addTarget(recorderSurface)
        }.build()

        //  Face detection
        rgbCameraThread?.quitSafely()
        rgbCameraThread = HandlerThread("RGB ImageReader Camera2")
        rgbCameraThread!!.start()
        val handler = Handler(rgbCameraThread!!.looper)
        rgbImageReader!!.setOnImageAvailableListener(faceProcessor, handler)
    }

    /**
     * Setup a persistent [Surface] for the recorder so we can use it as an output target for the
     * camera session without preparing the recorder
     */
    private val recorderSurface: Surface by lazy {

        // Get a persistent Surface from MediaCodec, don't forget to release when done
        val surface = MediaCodec.createPersistentInputSurface()

        // Prepare and release a dummy MediaRecorder with our new surface
        // Required to allocate an appropriately sized buffer before passing the Surface as the
        //  output target to the capture session
        createRecorder(this.activity.applicationContext, surface).apply {
            prepare()
            release()
        }
        currentRecorderOutputFile?.delete()

        surface
    }

    /** Saves the video recording */
    private val recorder: MediaRecorder by lazy { createRecorder(this.activity.applicationContext, recorderSurface) }

    /** Creates a [MediaRecorder] instance using the provided [Surface] as input */
    private fun createRecorder(context: Context, surface: Surface) = MediaRecorder().apply {
        val RECORDER_VIDEO_BITRATE: Int = 10_000_000
        val FRAMES_PER_SECOND: Int = 30
        val dirPath = context.filesDir.absolutePath + File.separator + VIDEO_OUTPUT_DIR_NAME
        File(dirPath).mkdirs()
        val outputFile = File(dirPath, videoOutputName())

        //setAudioSource(MediaRecorder.AudioSource.MIC)
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setOutputFile(outputFile.absolutePath)
        setVideoEncodingBitRate(RECORDER_VIDEO_BITRATE)
        setVideoFrameRate(FRAMES_PER_SECOND)
        setVideoSize(RGB_VIDEO_SIZE.width, RGB_VIDEO_SIZE.height)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        //setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setInputSurface(surface)

        currentRecorderOutputFile = outputFile
    }

    //  IR Camera2
    private var irCameraId: String = ""
    private var irCameraSession: CameraCaptureSession? = null;
    private var irCameraThread: HandlerThread? = null
    private var irCameraCaptureThread: HandlerThread? = null
    private var irCameraDeviceLevel: Int? = 0

    init {
        initializeCameraInfo()
    }

    @SuppressLint("RestrictedApi")
    fun checkEmulator() {
        if (manager.cameraIdList.size == 1 && Device.TYPE == Device.PHONE) { // emulator
            RGB_CAMERA_SELECTOR = lensFace
            faceGraphicView?.setCameraSelector(RGB_CAMERA_SELECTOR)
            //setRotation(CameraOrientationUtil.degreesToSurfaceRotation(orientation))
        }
    }

    override fun start(flipRgbCamera: Boolean) {
        LED.init()
        LED.on(LED.TYPE.WHITE)

        try {
            if (flipRgbCamera) {
                RGB_CAMERA_SELECTOR = if (RGB_CAMERA_SELECTOR == CameraMetadata.LENS_FACING_FRONT) {
                    CameraMetadata.LENS_FACING_BACK
                } else {
                    CameraMetadata.LENS_FACING_FRONT
                }
                faceGraphicView?.setCameraSelector(RGB_CAMERA_SELECTOR)
            }

            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            closeRgbCamera()
            initializeCameraInfo()
            openRgbCamera()
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

    override fun stop() {
        LED.dispose()
        closeRgbCamera()
        closeIRCamera()
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        faceThermal?.stop()
    }

    fun startRecord() {
        val degree = when(faceProcessor.orientation % 360) {
            in 45 until 135 -> 90
            in 135 until 225 -> 180
            in 270 until 315 -> 270
            else -> 0
        }
        recorder.apply {
            setOrientationHint(degree)
            prepare()
            start()
        }
    }

    fun stopRecord() {
        recorder.stop()
    }

    //  Set FaceProcessor orientation getting by OrientationEventListener
    override fun setRotation(surfaceRotation: Int) {
        var sensorOrientation = 0
        when(RGB_CAMERA_SELECTOR) {
            CameraCharacteristics.LENS_FACING_FRONT -> {
                sensorOrientation = 270
            }
            CameraCharacteristics.LENS_FACING_BACK -> {
                sensorOrientation = 90
            }
        }

        val degree = when(surfaceRotation) {
            Surface.ROTATION_0 -> { 0 }
            Surface.ROTATION_90 -> { 90 }
            Surface.ROTATION_180 -> { 180 }
            Surface.ROTATION_270 -> { 270 }
            else -> {
                Log.w(TAG, "setRotation warning: input does not Surface.Rotation type")
                0
            }
        }

        faceProcessor.orientation = (degree + sensorOrientation) % 360
    }

    override fun initializeCameraInfo() {
        rgbCameraId = ""
        irCameraId = ""
        /*
         * 아래 characteristics에서 가져온 SCALER_STREAM_CONFIGURATION_MAP에서 size에서 가능한 사이즈를 보고 최적화하여 선택한다.
         */
        for (cameraId in manager.cameraIdList) {
            CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val size =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.getOutputSizes(ImageFormat.YUV_420_888)
            val DEPTH_IS_EXCLUSIVE = characteristics.get(CameraCharacteristics.DEPTH_DEPTH_IS_EXCLUSIVE)
            val ORIENTATION = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
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
            if (Device.TYPE != Device.PHONE && LENS_FACING == IR_CAMERA_SELECTOR) {
                // IR Camera
                irCameraId = cameraId
                faceProcessorIR.orientation = ORIENTATION
                irCameraDeviceLevel = HARDWARE_LEVEL
                cameraName = "IR"
            } else {
                if(LENS_FACING == RGB_CAMERA_SELECTOR) {
                    rgbCameraId = cameraId
                    faceProcessor.portraitOrientation = ORIENTATION
                    faceProcessor.orientation = ORIENTATION
                    rgbCameraDeviceLevel = HARDWARE_LEVEL
                }
                cameraName = "RGB"
            }
            val fpsRange =
                characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            Log.d(TAG, "$cameraName Camera cameraId $cameraId LENS $LENS_FACING")
            Log.d(TAG, "$cameraName Camera available size ${size?.joinToString(",")}")
            Log.d(TAG, "$cameraName Camera available fps ${fpsRange?.joinToString(",")}")
            Log.d(TAG, "$cameraName Camera orientation $ORIENTATION level $HARDWARE_LEVEL depthExclusive $DEPTH_IS_EXCLUSIVE")
            Log.d(TAG, "$cameraName Camera capability ${cameraDeviceCapability.joinToString(",")}")
            Log.d(TAG, "$cameraName Camera distortion $LENS_DISTORTION calibration $INTRINSIC_CALIBRATION radial_distortion $RADIAL_DISTORTION")
        }
    }

    private fun openRgbCamera() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "openRgbCamera")

            closeRgbCamera()

            manager.openCamera(
                rgbCameraId,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        rgbCameraDevice = camera
                        rgbImageReader = ImageReader.newInstance(RGB_DETECTION_SIZE.width, RGB_DETECTION_SIZE.height, ImageFormat.YUV_420_888, 3)
                        camera.createCaptureSession(
                            mutableListOf(rgbImageReader!!.surface, recorderSurface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    try {
                                        rgbCameraSession = session
                                        startDetectionRequest()
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
                        Log.d(TAG, "rgbCamera disconnected")
                        stop()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.d(TAG, "rgbCamera error $error")
                    }
                }, null
            )
        }
    }

    private fun closeRgbCamera() {
        rgbCameraDevice?.close()
        rgbImageReader?.close()
//        rgbCameraCaptureThread?.quitSafely()
//        rgbCameraThread?.quitSafely()
        if (rgbCameraSession != null) {
            rgbCameraSession?.close()
            rgbCameraSession = null
            /*
             * 강제로 하드웨어 타입이 레거시일 경우 camera2 api에서 release가 안되는 문제가 있음.
             * 언제 해결될진 모르나 강제로 카메라를 가져와서 release하는 방어코드를 추가
             */
            if (rgbCameraDeviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                if (rgbCameraId.isNotEmpty()) {
                    try {
                        val rgbCameraLegacy = Camera.open(rgbCameraId.toInt())
                        rgbCameraLegacy?.release()
                    } catch (e: Exception) {
                        Log.d(TAG, "rgbCameraLegacy throw $e")
                    }
                }
            }
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
                        faceThermal?.setFaceInfo(RGB_DETECTION_SIZE.width, RGB_DETECTION_SIZE.height, FaceDetectLogic.rgbFaces!![0])
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

    companion object {
        const val TAG = "FaceCamera2"

        // 최적화하기 위해 조정가능
        val RGB_DETECTION_SIZE = if (Device.TYPE == Device.DONGA) {
            Size(1280, 720)
        } else if(Device.TYPE == Device.HLDS) { // HLDS
            Size(800, 600)
        } else if(Device.TYPE == Device.PHONE)  {
            Size(1080, 1920)
        } else {
            Size(1280, 720)
        }
        val RGB_VIDEO_SIZE = if (Device.TYPE == Device.DONGA) {
            Size(1280, 720)
        } else if(Device.TYPE == Device.HLDS) { // HLDS
            Size(800, 600)
        } else if(Device.TYPE == Device.PHONE)  {
            Size(1280, 720)
        } else {
            Size(1280, 720)
        }

        val IR_CAMERA_SIZE = if (Device.TYPE == Device.DONGA) {
            Size(1024, 768)
        } else { // HLDS
            Size(800, 600)
        }
    }

    class FaceProcessorCamera2(
        private val activity: Activity,
        private val faceClipView: ImageView?,
    ) : ImageReader.OnImageAvailableListener {

        var portraitOrientation: Int = 0
        var orientation: Int = 0
        private val faceClip = FaceClip(activity)
        var faceGraphicView: FaceGraphicView? = null
        private val yuvToRgbConverter = YuvToRgbConverter(activity)

        override fun onImageAvailable(reader: ImageReader?) {
            val image = reader?.acquireLatestImage()

            var bitmap: Bitmap? = null
            if (image != null) {
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

                FaceDetectLogic.rgbInputImage = inputImage
                FaceDetectLogic.fullscreenImageBitmap = bitmap
                FaceDetectLogic.update()

                faceGraphicView?.setRotation(portraitOrientation - orientation)
                faceGraphicView?.setInputImage(inputImage, FaceDetectLogic.rgbFaces)
                if (faceClipView != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        faceClipView.setImageBitmap(bitmap)
                    }
                }
            }
            if (image != null && faceClipView != null && bitmap != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    faceClipView.setImageBitmap(bitmap)
                }
            }
            image?.close()
        }

        companion object {
            private val TAG = FaceProcessorCamera2::class.java.simpleName
        }
    }
}