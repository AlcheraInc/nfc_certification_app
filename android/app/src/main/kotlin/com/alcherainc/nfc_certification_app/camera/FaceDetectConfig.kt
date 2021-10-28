package com.jhoonPark.flutter_playground.camera

class FaceDetectConfig {
    companion object {
        private const val TAG = "FaceDetectConfig"
    }

    enum class CONFIG_TYPE {
        DEFAULT,
        REGISTER,
        FIT_TIGHT,
        WALK_THROUGH
    }
    var currentConfigType: CONFIG_TYPE = CONFIG_TYPE.DEFAULT

    var VALID_PITCH_DEGREE = 20 // up and down
    var VALID_YAW_DEGREE = 20 // left and right
    var VALID_ROLL_DEGREE = 20 // twist
    var VALID_FACE_SIZE_MIN_RATE_ORIENTATION_LANDSCAPE = 0f//6.4f
    var VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT = 0f//8.0f
    var VALID_FACE_SIZE_MAX_RATE_ORIENTATION_LANDSCAPE = 3.0f
    var VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT = 1.7f
    var VALID_FACE_SIZE_MIN_RATE = VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT // must be set zero for remove limit
    var VALID_FACE_SIZE_MAX_RATE = VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT
    var VALID_FACE_POSITION_RATE = 0 // must be set zero for remove limit
    var VALID_FACE_DETECT_CONFIDENCE_VALUE = 0.9
    var VALID_FACE_MASK_USAGE_RATE = 0.5
    var VALID_FACE_LIVENESS_QUEUE_SIZE = 3
    var VALID_FACE_LIVENESS_CONFIDENCE_VALUE = 0.25
    var VALID_FACE_LIVENESS_EXACTLY_CONFIDENCE_VALUE = 0.8

    init {
        setByType(CONFIG_TYPE.DEFAULT)
    }

    fun setByType(configType: CONFIG_TYPE) {
        currentConfigType = configType
        when(configType) {
            CONFIG_TYPE.DEFAULT -> {
                VALID_PITCH_DEGREE = 20 // up and down
                VALID_YAW_DEGREE = 20 // left and right
                VALID_ROLL_DEGREE = 20 // twist
                VALID_FACE_SIZE_MIN_RATE_ORIENTATION_LANDSCAPE = 6.4f
                VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT = 8.0f
                VALID_FACE_SIZE_MAX_RATE_ORIENTATION_LANDSCAPE = 3.0f
                VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT = 1.7f
                VALID_FACE_SIZE_MIN_RATE = VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT // must be set zero for remove limit
                VALID_FACE_SIZE_MAX_RATE = VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT
                VALID_FACE_POSITION_RATE = 5 // must be set zero for remove limit
                VALID_FACE_DETECT_CONFIDENCE_VALUE = 0.9
                VALID_FACE_MASK_USAGE_RATE = 0.5
                VALID_FACE_LIVENESS_QUEUE_SIZE = 3
                VALID_FACE_LIVENESS_CONFIDENCE_VALUE = 0.25
                VALID_FACE_LIVENESS_EXACTLY_CONFIDENCE_VALUE = 0.8
            }
            CONFIG_TYPE.REGISTER -> {
                VALID_PITCH_DEGREE = 20 // up and down
                VALID_YAW_DEGREE = 20 // left and right
                VALID_ROLL_DEGREE = 20 // twist
                VALID_FACE_SIZE_MIN_RATE_ORIENTATION_LANDSCAPE = 6.4f
                VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT = 2.0f
                VALID_FACE_SIZE_MAX_RATE_ORIENTATION_LANDSCAPE = 3.0f
                VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT = 0f
                VALID_FACE_SIZE_MIN_RATE = VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT // must be set zero for remove limit
                VALID_FACE_SIZE_MAX_RATE = VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT
                VALID_FACE_POSITION_RATE = 5 // must be set zero for remove limit
                VALID_FACE_DETECT_CONFIDENCE_VALUE = 0.9
                VALID_FACE_MASK_USAGE_RATE = 0.5
                VALID_FACE_LIVENESS_QUEUE_SIZE = 3
                VALID_FACE_LIVENESS_CONFIDENCE_VALUE = 0.25
                VALID_FACE_LIVENESS_EXACTLY_CONFIDENCE_VALUE = 0.8
            }
            CONFIG_TYPE.FIT_TIGHT -> {
                VALID_PITCH_DEGREE = 20 // up and down
                VALID_YAW_DEGREE = 20 // left and right
                VALID_ROLL_DEGREE = 20 // twist
                VALID_FACE_SIZE_MIN_RATE_ORIENTATION_LANDSCAPE = 6.4f
                VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT = 2.5f
                VALID_FACE_SIZE_MAX_RATE_ORIENTATION_LANDSCAPE = 3.0f
                VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT = 0f
                VALID_FACE_SIZE_MIN_RATE = VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT // must be set zero for remove limit
                VALID_FACE_SIZE_MAX_RATE = VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT
                VALID_FACE_POSITION_RATE = 5 // must be set zero for remove limit
                VALID_FACE_DETECT_CONFIDENCE_VALUE = 0.9
                VALID_FACE_MASK_USAGE_RATE = 0.5
                VALID_FACE_LIVENESS_QUEUE_SIZE = 3
                VALID_FACE_LIVENESS_CONFIDENCE_VALUE = 0.25
                VALID_FACE_LIVENESS_EXACTLY_CONFIDENCE_VALUE = 0.8
            }
            CONFIG_TYPE.WALK_THROUGH -> {
                VALID_PITCH_DEGREE = 20 // up and down
                VALID_YAW_DEGREE = 20 // left and right
                VALID_ROLL_DEGREE = 20 // twist
                VALID_FACE_SIZE_MIN_RATE_ORIENTATION_LANDSCAPE = 0f
                VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT = 0f
                VALID_FACE_SIZE_MAX_RATE_ORIENTATION_LANDSCAPE = 3.0f
                VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT = 1.7f
                VALID_FACE_SIZE_MIN_RATE = VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT // must be set zero for remove limit
                VALID_FACE_SIZE_MAX_RATE = VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT
                VALID_FACE_POSITION_RATE = 0 // must be set zero for remove limit
                VALID_FACE_DETECT_CONFIDENCE_VALUE = 0.9
                VALID_FACE_MASK_USAGE_RATE = 0.5
                VALID_FACE_LIVENESS_QUEUE_SIZE = 3
                VALID_FACE_LIVENESS_CONFIDENCE_VALUE = 0.25
                VALID_FACE_LIVENESS_EXACTLY_CONFIDENCE_VALUE = 0.8
            }
            else ->  {
                VALID_PITCH_DEGREE = 20 // up and down
                VALID_YAW_DEGREE = 20 // left and right
                VALID_ROLL_DEGREE = 20 // twist
                VALID_FACE_SIZE_MIN_RATE_ORIENTATION_LANDSCAPE = 6.4f
                VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT = 8.0f
                VALID_FACE_SIZE_MAX_RATE_ORIENTATION_LANDSCAPE = 3.0f
                VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT = 1.7f
                VALID_FACE_SIZE_MIN_RATE = VALID_FACE_SIZE_MIN_RATE_ORIENTATION_PORTRAIT // must be set zero for remove limit
                VALID_FACE_SIZE_MAX_RATE = VALID_FACE_SIZE_MAX_RATE_ORIENTATION_PORTRAIT
                VALID_FACE_POSITION_RATE = 5 // must be set zero for remove limit
                VALID_FACE_DETECT_CONFIDENCE_VALUE = 0.9
                VALID_FACE_MASK_USAGE_RATE = 0.5
                VALID_FACE_LIVENESS_QUEUE_SIZE = 3
                VALID_FACE_LIVENESS_CONFIDENCE_VALUE = 0.25
                VALID_FACE_LIVENESS_EXACTLY_CONFIDENCE_VALUE = 0.8
            }
        }
    }
}