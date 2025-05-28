package com.example.helmetdetection.utils

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteHelper(context: Context) {

    private lateinit var interpreter: Interpreter
    init {
        try {
            interpreter = Interpreter(loadModelFile(context, "weights_float32.tflite"))
            Log.d("TFLite", "Model berhasil dimuat")
        } catch (e: Exception) {
            Log.e("TFLite", "Gagal memuat model", e)
        }
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun runInference(input: Array<Array<Array<FloatArray>>>): Array<Array<FloatArray>> {
        val output = Array(1) { Array(300) { FloatArray(6) } }
        interpreter.run(input, output)
        return output
    }

    fun close() {
        interpreter.close()
    }
}
