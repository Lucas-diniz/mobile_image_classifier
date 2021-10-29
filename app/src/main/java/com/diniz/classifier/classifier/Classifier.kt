package com.diniz.classifier.classifier

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.diniz.classifier.ImageTransform
import com.diniz.classifier.R
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer

open class Classifier(var context: Context): IClassifier {

    var associatedAxisLabels: List<String>? = null
    var tflite: Interpreter? = null

    init {
        initLabel()
        initModel()
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun classify(image: ImageProxy): Map<String, Map.Entry<String, Float>?> {

        val bitmap: Bitmap = ImageTransform.imageToBitmap(image.image!!)
        val probabilityBuffer = getProbabilityBuffer()

        tflite!!.run(
            getTensorImageBuffer(bitmap,image),
            probabilityBuffer.buffer)

        return getResult(probabilityBuffer)
    }

    private fun initLabel(){
        try {
            associatedAxisLabels = FileUtil.loadLabels(context, ASSOCIATED_AXIS_LABELS)
        } catch (e: IOException) {
            Log.e(TFLITESUPORT, context.resources.getString(R.string.error_label), e)
        }
    }

    private fun initModel(){
        try {
            val tfliteModel = FileUtil.loadMappedFile(
                context,
                MODELFILEPATH
            )
            tflite = Interpreter(tfliteModel)
        } catch (e: IOException) {
            Log.e(TFLITESUPORT, context.resources.getString(R.string.error_model), e)
        }
    }

    private fun getImageProcessor(image: ImageProxy, bitmap: Bitmap): ImageProcessor {
        val rotation = ImageTransform.getImageRotation(image)
        val width = bitmap.width
        val height = bitmap.height

        val size = if (height > width) width else height
        return ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(size, size))
            .add(ResizeOp(TARGETHEIGHT, TARGETWIDTH, ResizeOp.ResizeMethod.BILINEAR))
            .add(Rot90Op(rotation))
            .build()
    }

    private fun getTensorImageBuffer(bitmap: Bitmap,image: ImageProxy): ByteBuffer? {
        var tensorImage = TensorImage(DataType.UINT8)
        tensorImage.load(bitmap)
        tensorImage = getImageProcessor(image,bitmap).process(tensorImage)
        return tensorImage.buffer
    }

    private fun getProbabilityBuffer(): TensorBuffer {
        return TensorBuffer.createFixedSize(intArrayOf(1, NUM_LABEL), DataType.UINT8)
    }

    private fun getProbabilityProcessor(): TensorProcessor {
        return TensorProcessor.Builder().add(NormalizeOp(MEAN, STDDEV)).build()
    }

    private fun getResult(probabilityBuffer: TensorBuffer): Map<String, Map.Entry<String, Float>?> {

        var result: Map<String, Map.Entry<String, Float>?> = mapOf()
        if (associatedAxisLabels != null) {
            val labels = TensorLabel(
                associatedAxisLabels!!,
                getProbabilityProcessor().process(probabilityBuffer)
            )
            val floatMap = labels.mapWithFloatValue
            result = writeResults(floatMap)
        }

        return result
    }

    private fun writeResults(mapResults: Map<String, Float>): Map<String, Map.Entry<String, Float>?> {

        var entryMax: Map.Entry<String, Float>? = null
        var entryMax1: Map.Entry<String, Float>? = null
        var entryMax2: Map.Entry<String, Float>? = null

        for (entry in mapResults.entries) {

            if (entryMax == null || entry.value.compareTo(entryMax.value) > 0) {

                entryMax = entry

            } else if (entryMax1 == null || entry.value.compareTo(entryMax1.value) > 0) {

                entryMax1 = entry

            } else if (entryMax2 == null || entry.value.compareTo(entryMax2.value) > 0) {

                entryMax2 = entry

            }
        }

        return mapOf(FIRST to entryMax, SECOND to entryMax1, THIRD to entryMax2)
    }

    companion object{
        const val ASSOCIATED_AXIS_LABELS = "labels_mobilenet_quant_v1_224.txt"
        const val TFLITESUPORT = "tfliteSupport"
        const val MODELFILEPATH = "mobilenet_v1_1.0_224_quant.tflite"
        const val TARGETHEIGHT = 224
        const val TARGETWIDTH = 224
        const val MEAN = 0F
        const val STDDEV = 0F
        const val FIRST = "First"
        const val SECOND = "second"
        const val THIRD = "third"
        const val NUM_LABEL = 1001
    }
}