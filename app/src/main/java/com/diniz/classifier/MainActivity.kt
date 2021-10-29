package com.diniz.classifier

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.content.ContextCompat
import androidx.camera.core.*
import com.diniz.classifier.classifier.Classifier
import com.diniz.classifier.classifier.IResultClassifier
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity(), IResultClassifier {

    private var topClass: String = ""
    private var ranking: MutableMap<String, Int> = mutableMapOf()
    private lateinit var camera: Camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initCamera()
        initListeners()
        saveTopClassBySecond()
    }

    private fun initCamera() {
        camera = Camera(this)
            .setPreview(viewFinder)
            .setClassifier(Classifier(this),this)
            .startCamera()
    }

    override fun onResultClassifier(result: Map<String, Map.Entry<String, Float>?>) {
        topClass = result["First"]!!.key
        result1.text = topClass
        result2.text = result["second"]!!.key + ":    " + result["second"]!!.value
        result3.text = result["third"]!!.key + ":     " + result["third"]!!.value
    }

    private fun initListeners() {
        take_picture.setOnClickListener {
            camera.takePicture()
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
        camera.analysis = false
        turn_classifier_on.visibility = GONE
        turn_classifier_off.visibility = VISIBLE
        data.setBackgroundColor(ContextCompat.getColor(this, R.color.read))
        data.setBackgroundColor(ContextCompat.getColor(this, R.color.read))
        showToast(resources.getString(R.string.classifier_off),this)
    }

    private fun turnClassifierOff(){
        camera.analysis = true
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
        view_camera.visibility = GONE
    }

    private fun showClassifier(){
        data.visibility = VISIBLE
        data2.visibility = VISIBLE
        view_camera.visibility = VISIBLE
        top.visibility = GONE
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

    companion object {
        const val RANKING_TIME = 1000L
    }
}