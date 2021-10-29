package com.diniz.classifier

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutionException

class Camera {


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                try {
                    bindPreview(cameraProviderFuture.get())
                } catch (e: ExecutionException) {
                } catch (e: InterruptedException) {
                }
            },
            ActivityCompat.getMainExecutor(this)
        )
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        cameraProvider.bindToLifecycle(
            this,
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

        imageAnalysis.setAnalyzer(ActivityCompat.getMainExecutor(this),
            { image ->

                if(analysis){
                    val result = classifier.classify(image)

                    topClass = result["First"]!!.key
                    result1.text = topClass
                    result2.text = result["second"]!!.key + ":    " + result["second"]!!.value
                    result3.text = result["third"]!!.key + ":     " + result["third"]!!.value
                }

                image.close()
            })

        return imageAnalysis
    }

    private fun getPreview(): Preview {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(viewFinder.surfaceProvider)
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
}