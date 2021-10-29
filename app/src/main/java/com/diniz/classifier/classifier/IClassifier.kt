package com.diniz.classifier

import androidx.camera.core.ImageProxy

interface IClassifier {
    fun classify(image: ImageProxy): Map<String, Map.Entry<String, Float>?>
}