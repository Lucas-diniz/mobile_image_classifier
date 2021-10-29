package com.diniz.classifier

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.Image
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.core.*
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ScheduledExecutorService


class MainActivity : AppCompatActivity() {

    private lateinit var classifier: Classifier
    private lateinit var imageCapture: ImageCapture

    private var analysis: Boolean = true
    private var topClass: String = ""
    private var ranking: MutableMap<String, Int> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initClassifier()
        initCamera()
        initListeners()
        saveTopClassBySecond()
    }

    private fun initClassifier(){
        classifier = Classifier(this)
    }

    private fun initCamera() {
        if (hasPermission()) {
            startCamera()
        }
    }

    private fun initListeners() {
        take_picture.setOnClickListener {
            takePicture()
        }
        turn_classifier_on.setOnClickListener {
            turnClassifierOn()
        }
        turn_classifier_off.setOnClickListener {
            turnClassifierOff()
        }
        show_top_list.setOnClickListener {
            showTopList()
        }
        back_to_classifier.setOnClickListener {
            showClassifier()
        }
    }

    private fun turnClassifierOn(){
        analysis = false
        turn_classifier_on.visibility = GONE
        turn_classifier_off.visibility = VISIBLE
        data.setBackgroundColor(ContextCompat.getColor(this, R.color.read))
        data.setBackgroundColor(ContextCompat.getColor(this, R.color.read))
        showToast(resources.getString(R.string.classifier_off),this)
    }

    private fun turnClassifierOff(){
        analysis = true
        turn_classifier_on.visibility = VISIBLE
        turn_classifier_off.visibility = GONE
        data.setBackgroundColor(ContextCompat.getColor(this, R.color.first))
        showToast(resources.getString(R.string.classifier_on),this)
    }

    private fun showTopList(){
        setTopList()
        top.visibility = VISIBLE
        data.visibility = GONE
        data2.visibility = GONE
        camera.visibility = GONE
    }

    private fun showClassifier(){
        data.visibility = VISIBLE
        data2.visibility = VISIBLE
        camera.visibility = VISIBLE
        top.visibility = GONE
    }

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

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        cameraProvider.bindToLifecycle(
            this,
            getCameraSelector(),
            getPreview(),
            getAnalysis(),
            getImageCapture()
        )
    }

    private fun saveTopClassBySecond(){
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
          if(topClass != "")
              addClassToRanking(topClass)
            saveTopClassBySecond()
        }, RANKING_TIME)
    }

    private fun addClassToRanking(name: String){
        var isName = false
        ranking.forEach {
            if(it.key == name){
                ranking[name] = it.value+1
                isName = true
                return@forEach
            }
        }
        if(!isName){
            ranking[name] = 1
        }
    }

    private fun setTopList(){
        var top = 1
        toplist.text = ""
        return ranking
            .toList()
            .sortedBy { it.second }
            .forEach{
            if(top<=10)
                toplist.text = "${it.first} appears ${it.second} times \n\n ${toplist.text}"
            top++
       }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun takePicture() {

        val photoFile = File(
            getOutputDirectory(),
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + IMG_EXTENSION
        )

        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "${resources.getString(R.string.foto_capture)} ${exc.message}", exc)
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = " ${resources.getString(R.string.foto_capture)} $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    private fun hasPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA
                ), REQUEST_CAMERA_PERMISSION
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (hasPermission()) {
                startCamera()
            } else {
                Toast.makeText(
                    this, resources.getString(R.string.camera_access),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val REQUEST_CAMERA_PERMISSION = 1
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val TAG = "CameraXExample"
        const val RANKING_TIME = 1000L
        const val IMG_EXTENSION = ".jpg"
    }
}