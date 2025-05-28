package com.example.helmetdetection.utils

import android.graphics.Bitmap
import android.graphics.Color

object ImageUtils {

    fun bitmapToInputArray(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val input = Array(1) { Array(416) { Array(416) { FloatArray(3) } } }

        for (y in 0 until 416) {
            for (x in 0 until 416) {
                val pixel = bitmap.getPixel(x, y)
                input[0][y][x][0] = (Color.red(pixel) / 255.0f)
                input[0][y][x][1] = (Color.green(pixel) / 255.0f)
                input[0][y][x][2] = (Color.blue(pixel) / 255.0f)
            }
        }
        return input
    }
}
