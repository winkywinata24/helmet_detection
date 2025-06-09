package com.example.helmetdetection.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helmetdetection.R
import com.example.helmetdetection.adapters.NotificationAdapter
import com.example.helmetdetection.network.RetrofitClient
import com.example.helmetdetection.network.LogResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationFragment : Fragment() {
    private lateinit var rvNotification: RecyclerView
    private var lastLogId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)
        rvNotification = view.findViewById(R.id.rvNotification)
        rvNotification.layoutManager = LinearLayoutManager(requireContext())
        getNotifikasi()
        return view
    }

    private fun getNotifikasi() {
        RetrofitClient.instance.getLog().enqueue(object : Callback<LogResponse> {
            override fun onResponse(call: Call<LogResponse>, response: Response<LogResponse>) {
                if (response.isSuccessful) {
                    val allLog = response.body()?.log ?: emptyList()
                    val filtered = allLog.filter { it.status_helm == 0 }
                    rvNotification.adapter = NotificationAdapter(filtered)

                    val latest = filtered.maxByOrNull { it.id }
                    val storedLogId = getLastLogId()

                    if (latest != null && latest.id > storedLogId) {
                        showNotification("Deteksi orang tanpa helm pada ${latest.waktu}")
                        saveLastLogId(latest.id)
                    }
                }
            }

            override fun onFailure(call: Call<LogResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Gagal ambil notifikasi", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showNotification(message: String) {
        val channelId = "alert_channel_id"
        val notificationId = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alert Notification"
            val descriptionText = "Notifikasi pelanggaran helm"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle("Pelanggaran Helm!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
                return
            }
        }
        with(NotificationManagerCompat.from(requireContext())) {
            notify(notificationId, builder.build())
        }
    }

    private fun getLastLogId(): Int {
        val prefs = requireContext().getSharedPreferences("helmet_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("last_log_id", -1)
    }

    private fun saveLastLogId(id: Int) {
        val prefs = requireContext().getSharedPreferences("helmet_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("last_log_id", id).apply()
    }

}
