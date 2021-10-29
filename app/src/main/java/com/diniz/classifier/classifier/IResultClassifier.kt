package com.diniz.classifier

interface IResultClassifier {
    fun onResultClassifier(result: Map<String, Map.Entry<String, Float>?>)
}