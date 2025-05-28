package com.example.helmetdetection.models

data class DetectionResult(
    val classId: Int,
    val confidence: Float,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)
