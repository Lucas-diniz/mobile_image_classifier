package com.diniz.classifier.classifier

interface IResultClassifier {
    fun onResultClassifier(result: Map<String, Map.Entry<String, Float>?>)
}