package com.alcherainc.nfc_certification_app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.media.Image
import android.renderscript.*
import android.util.Log
import java.nio.ByteBuffer

class YuvToRgbConverter(context: Context) {
    private val rs = RenderScript.create(context)
    private val scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    private var pixelCount: Int = -1
    private var imageBytesSize: Int = -1
    private lateinit var yuvBuffer: ByteArray
    private lateinit var inputAllocation: Allocation
    private lateinit var outputAllocation: Allocation

    fun getImageToBitmap(image: Image, rotationDegrees: Int, resizeWidth: Int?, resizeHeight: Int?, isFlip: Boolean = false): Bitmap {
        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        yuvToRgb(image, bitmap)
        var rotationBitmap: Bitmap? = null
        val matrix = Matrix()
        if (rotationDegrees != 0) {
            rotationBitmap = if (resizeWidth != null && resizeHeight != null) {
                matrix.setScale(resizeWidth.toFloat() / image.width.toFloat(), resizeHeight.toFloat() / image.height.toFloat())
                matrix.preRotate(rotationDegrees.toFloat())
                if (isFlip) {
                    matrix.setScale(-1f, 1f, resizeWidth / 2f, resizeHeight / 2f)
                }
                Bitmap.createBitmap(bitmap, 0, 0, resizeWidth, resizeHeight, matrix, false)
            } else {
                matrix.preRotate(rotationDegrees.toFloat())
                if (isFlip) {
                    matrix.setScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
            }
        } else {
            rotationBitmap = if (resizeWidth != null && resizeHeight != null) {
                val tmpBitmap = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, false)
                if (isFlip) {
                    matrix.setScale(-1f, 1f, tmpBitmap.width / 2f, tmpBitmap.height / 2f)
                    Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.width, tmpBitmap.height, matrix, false)
                } else {
                    tmpBitmap
                }
            } else {
                if (isFlip) {
                    matrix.setScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
            }
        }
        return rotationBitmap ?: bitmap
    }

    @Synchronized
    fun yuvToRgb(image: Image, output: Bitmap) {
        // Bits per pixel is an average for the whole image, so it's useful to compute the size
        // of the full buffer but should not be used to determine pixel offsets
        val pixelSizeBits = ImageFormat.getBitsPerPixel(image.format)

        //  Camera configuration(resolution, image format)등이 바뀐 경우
        //  buffer, allocation도 그에 맞게 새로 instantiation 해주어야한다
        pixelCount = image.cropRect.width() * image.cropRect.height()
        imageBytesSize = pixelCount * pixelSizeBits / 8

        // Ensure that the intermediate output byte buffer is allocated
        if (!::yuvBuffer.isInitialized || imageBytesSize != yuvBuffer.size) {
            yuvBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
        }

        // Get the YUV data in byte array form using NV21 format
        imageToByteArray(image, yuvBuffer)

        // Ensure that the RenderScript inputs and outputs are allocated
        if (!::inputAllocation.isInitialized || yuvBuffer.size != inputAllocation.bytesSize) {
            // Explicitly create an element with type NV21, since that's the pixel format we use
            val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
            inputAllocation = Allocation.createSized(rs, elemType.element, yuvBuffer.size)
        }
        if (!::outputAllocation.isInitialized || yuvBuffer.size != outputAllocation.bytesSize) {
            outputAllocation = Allocation.createFromBitmap(rs, output)
        }

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvBuffer)
        scriptYuvToRgb.setInput(inputAllocation)
        scriptYuvToRgb.forEach(outputAllocation)
        outputAllocation.copyTo(output)
    }

    @Synchronized
    fun yuvToRgb(byteArray: ByteArray, output: Bitmap) {
        yuvBuffer = byteArray
        // Ensure that the RenderScript inputs and outputs are allocated
        if (!::inputAllocation.isInitialized || yuvBuffer.size != inputAllocation.bytesSize) {
            // Explicitly create an element with type NV21, since that's the pixel format we use
            val elemType = Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create()
            inputAllocation = Allocation.createSized(rs, elemType.element, yuvBuffer.size)
        }
        if (!::outputAllocation.isInitialized || yuvBuffer.size != outputAllocation.bytesSize) {
            outputAllocation = Allocation.createFromBitmap(rs, output)
        }

        // Convert NV21 format YUV to RGB
        inputAllocation.copyFrom(yuvBuffer)
        scriptYuvToRgb.setInput(inputAllocation)
        scriptYuvToRgb.forEach(outputAllocation)
        outputAllocation.copyTo(output)
    }

    private fun imageToByteArray(image: Image, outputBuffer: ByteArray) {
//        assert(image.format == ImageFormat.YUV_420_888)

        val imageCrop = image.cropRect
        val imagePlanes = image.planes

        imagePlanes.forEachIndexed { planeIndex, plane ->
            // How many values are read in input for each output value written
            // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
            //
            // Y Plane            U Plane    V Plane
            // ===============    =======    =======
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            val outputStride: Int

            // The index in the output buffer the next value will be written at
            // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
            //
            // First chunk        Second chunk
            // ===============    ===============
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            var outputOffset: Int

            when (planeIndex) {
                0 -> {
                    outputStride = 1
                    outputOffset = 0
                }
                1 -> {
                    outputStride = 2
                    // For NV21 format, U is in odd-numbered indices
                    outputOffset = pixelCount + 1
                }
                2 -> {
                    outputStride = 2
                    // For NV21 format, V is in even-numbered indices
                    outputOffset = pixelCount
                }
                else -> {
                    // Image contains more than 3 planes, something strange is going on
                    return@forEachIndexed
                }
            }

            val planeBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            // We have to divide the width and height by two if it's not the Y plane
            val planeCrop = if (planeIndex == 0) {
                imageCrop
            } else {
                Rect(
                    imageCrop.left / 2,
                    imageCrop.top / 2,
                    imageCrop.right / 2,
                    imageCrop.bottom / 2
                )
            }

            val planeWidth = planeCrop.width()
            val planeHeight = planeCrop.height()

            // Intermediate buffer used to store the bytes of each row
            val rowBuffer = ByteArray(plane.rowStride)

            // Size of each row in bytes
            val rowLength = if (pixelStride == 1 && outputStride == 1) {
                planeWidth
            } else {
                // Take into account that the stride may include data from pixels other than this
                // particular plane and row, and that could be between pixels and not after every
                // pixel:
                //
                // |---- Pixel stride ----|                    Row ends here --> |
                // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
                //
                // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
                (planeWidth - 1) * pixelStride + 1
            }

            for (row in 0 until planeHeight) {
                // Move buffer position to the beginning of this row
                planeBuffer.position(
                    (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
                )

                if (pixelStride == 1 && outputStride == 1) {
                    // When there is a single stride value for pixel and output, we can just copy
                    // the entire row in a single step
                    planeBuffer.get(outputBuffer, outputOffset, rowLength)
                    outputOffset += rowLength
                } else {
                    // When either pixel or output have a stride > 1 we must copy pixel by pixel
                    planeBuffer.get(rowBuffer, 0, rowLength)
                    for (col in 0 until planeWidth) {
                        outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                        outputOffset += outputStride
                    }
                }
            }
        }
    }

    fun rotateYUV420Degree90(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
        val yuv = ByteArray(imageWidth * imageHeight)
        var i = 0
        for (x in 0 until imageWidth) {
            for (y in imageHeight - 1 downTo 0) {
                yuv[i] = data[y * imageWidth + x]
                i++
            }
        }
        return yuv
    }

    fun rotateYUV420Degree180(
        data: ByteArray,
        imageWidth: Int,
        imageHeight: Int,
    ): ByteArray {
        val yuv = ByteArray(imageWidth * imageHeight)
        var count = 0
        var i = imageWidth * imageHeight - 1
        while (i >= 0) {
            yuv[count] = data[i]
            count++
            i--
        }
        return yuv
    }

    fun rotateYUV420Degree270(
        data: ByteArray, imageWidth: Int,
        imageHeight: Int,
    ): ByteArray? {
        return rotateYUV420Degree180(rotateYUV420Degree90(data, imageWidth, imageHeight),
            imageWidth,
            imageHeight)
    }
}