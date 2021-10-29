package com.diniz.classifier

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import java.util.concurrent.ExecutionException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.diniz.classifier.classifier.IClassifier
import com.diniz.classifier.classifier.IResultClassifier
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Camera (var context: AppCompatActivity) {

    var analysis: Boolean = true
    private lateinit var componentPreview: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var classifier: IClassifier
    private lateinit var classifierResult: IResultClassifier

    fun setPreview(preview: PreviewView): Camera {
        componentPreview = preview
        return this
    }

    fun setClassifier(classifier: IClassifier, result: IResultClassifier): Camera {
        this.classifier = classifier
        this.classifierResult = result
        return this
    }

    fun startCamera(): Camera {
        if(hasPermission()){
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    try {
                        bindPreview(cameraProviderFuture.get())
                    } catch (e: ExecutionException) {
                    } catch (e: InterruptedException) {
                    }
                },
                ActivityCompat.getMainExecutor(context)
            )
        }else{
            hasPermission()
        }
        return this
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        cameraProvider.bindToLifecycle(
            context,
            getCameraSelector(),
            getPreview(),
            getAnalysis(),
            getImageCapture()
        )
    }

    private fun getAnalysis(): ImageAnalysis {
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(ActivityCompat.getMainExecutor(context),
            { image ->

                if(analysis){
                    classifierResult.onResultClassifier(classifier.classify(image))
                }
                image.close()
            })

        return imageAnalysis
    }

    private fun getPreview(): Preview {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(componentPreview.surfaceProvider)
        return preview
    }

    private fun getImageCapture(): ImageCapture {
        val builder = ImageCapture.Builder()
        imageCapture = builder.build()
        return imageCapture
    }

    private fun getCameraSelector(): CameraSelector {
        return CameraSelector.Builder().requireLensFacing(
            CameraSelector.LENS_FACING_BACK
        ).build()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir
    }

    fun takePicture() {

        val photoFile = File(
            getOutputDirectory(),
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + IMG_EXTENSION
        )

        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "${context.resources.getString(R.string.foto_capture)} ${exc.message}", exc)
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = " ${context.resources.getString(R.string.foto_success)} $savedUri"
                    Toast.makeText(context.baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    private fun hasPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            context.requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA
                ), REQUEST_CAMERA_PERMISSION
            )
            return false
        }
        return true
    }

    companion object{
        private const val REQUEST_CAMERA_PERMISSION = 1
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val IMG_EXTENSION = ".jpg"
        const val TAG = "CameraXExample"
    }
}