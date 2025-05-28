package com.example.helmetdetection.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.helmetdetection.R

class DetectionService : Service() {

    private val CHANNEL_ID = "DetectionServiceChannel"
    private var handler: Handler? = null
    private lateinit var runnable: Runnable

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Deteksi Helm Aktif")
            .setContentText("Aplikasi sedang mendeteksi pelanggaran helm.")
            .setSmallIcon(R.drawable.ic_alert)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)

        // Mulai deteksi berkala
        startDetectionLoop()

        return START_STICKY
    }

    private fun startDetectionLoop() {
        runnable = object : Runnable {
            override fun run() {
                Log.d("DetectionService", "Running detection...")

                // TODO: Tambahkan logika deteksi helm di sini
                runHelmetDetection()

                // Jalankan kembali setelah 5 detik
                handler?.postDelayed(this, 5000)
            }
        }

        handler?.post(runnable)
    }

    private fun runHelmetDetection() {
        // Di sinilah kamu bisa:
        // - Ambil frame dari stream RTSP
        // - Jalankan model TFLite
        // - Kirim hasil ke server/database
        Log.d("DetectionService", "Simulasi deteksi dijalankan")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacks(runnable)
        Log.d("DetectionService", "Service dihentikan")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Channel Deteksi Helm",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}