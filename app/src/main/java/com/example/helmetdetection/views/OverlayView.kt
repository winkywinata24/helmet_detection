package com.example.helmetdetection.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var detections: List<DetectionResult> = emptyList()

    fun setDetections(detections: List<DetectionResult>) {
        this.detections = detections
        Log.d("Overlay", "Detections diterima: ${detections.size}")
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d("Overlay", "onDraw terpanggil. Jumlah deteksi: ${detections.size}")

        val paintBox = Paint().apply {
            color = Color.RED
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }

        val paintText = Paint().apply {
            color = Color.YELLOW
            textSize = 40f
            style = Paint.Style.FILL
        }

        for (det in detections) {
            val scaleX = width / 416f
            val scaleY = height / 416f
            val scaledRect = RectF(
                det.rect.left * scaleX,
                det.rect.top * scaleY,
                det.rect.right * scaleX,
                det.rect.bottom * scaleY
            )
            canvas.drawRect(scaledRect, paintBox)
            canvas.drawText("${det.label} %.2f".format(det.confidence), scaledRect.left, scaledRect.top - 10, paintText)

        }
    }
}

data class DetectionResult(
    val rect: RectF,
    val label: String,
    val confidence: Float
)
