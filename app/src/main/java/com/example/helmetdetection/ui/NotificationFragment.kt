package com.example.helmetdetection.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
                }
            }

            override fun onFailure(call: Call<LogResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Gagal ambil notifikasi", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
