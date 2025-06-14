package com.example.helmetdetection.utils

import kotlin.math.sqrt

object DetectionUtils {

    fun isDuplicate(
        currentOutput: List<FloatArray>,
        lastOutput: List<FloatArray>,
        threshold: Float = 50f,
        minMatches: Int = 3
    ): Boolean {
        var matchCount = 0
        for (currBox in currentOutput) {
            for (lastBox in lastOutput) {
                val dist = distance(currBox, lastBox)
                if (dist < threshold) {
                    matchCount++
                    break
                }
            }
        }
        return matchCount >= minMatches
    }

    private fun distance(box1: FloatArray, box2: FloatArray): Float {
        val dx = box1[0] - box2[0]
        val dy = box1[1] - box2[1]
        return sqrt(dx * dx + dy * dy)
    }
}
