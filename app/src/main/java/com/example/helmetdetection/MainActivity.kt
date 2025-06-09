package com.example.helmetdetection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.helmetdetection.network.LogResponse
import com.example.helmetdetection.network.RetrofitClient
import com.example.helmetdetection.service.DetectionService
import com.example.helmetdetection.ui.HomeFragment
import com.example.helmetdetection.ui.NotificationFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private var lastLogId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        loadFragment(HomeFragment())
        startLogPolling()

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> loadFragment(HomeFragment())
                R.id.notif -> loadFragment(NotificationFragment())
            }
            true
        }

        val serviceIntent = Intent(this, DetectionService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun startLogPolling() {
        val handler = Handler(Looper.getMainLooper())
        val pollingInterval = 10_000L // 10 detik

        val runnable = object : Runnable {
            override fun run() {
                RetrofitClient.instance.getLog().enqueue(object : Callback<LogResponse> {
                    override fun onResponse(call: Call<LogResponse>, response: Response<LogResponse>) {
                        if (response.isSuccessful) {
                            val filtered = response.body()?.log?.filter { it.status_helm == 0 } ?: emptyList()
                            val latest = filtered.maxByOrNull { it.id }
                            val sharedPref = getSharedPreferences("helmet_prefs", Context.MODE_PRIVATE)
                            val savedId = sharedPref.getInt("last_log_id", -1)

                            if (latest != null && latest.id > savedId) {
                                showNotification("Deteksi orang tanpa helm pada ${latest.waktu}")
                                sharedPref.edit().putInt("last_log_id", latest.id).apply()
                            }
                        }
                    }

                    override fun onFailure(call: Call<LogResponse>, t: Throwable) {
                        Log.e("LogPolling", "Gagal mengambil data log: ${t.message}", t)
                    }
                })

                handler.postDelayed(this, pollingInterval)
            }
        }

        handler.post(runnable)
    }

    private fun showNotification(message: String) {
        val channelId = "alert_channel_id"
        val notificationId = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alert Notification"
            val descriptionText = "Notifikasi pelanggaran helm"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle("Pelanggaran Helm!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }
}
