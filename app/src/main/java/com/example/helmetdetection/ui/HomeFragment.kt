package com.example.helmetdetection.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.helmetdetection.views.DetectionResult
import com.example.helmetdetection.utils.DetectionUtils
import com.example.helmetdetection.utils.ImageUtils
import com.example.helmetdetection.views.OverlayView
import com.example.helmetdetection.R
import com.example.helmetdetection.utils.TFLiteHelper
import com.example.helmetdetection.adapters.LogAdapter
import com.example.helmetdetection.models.LogPostRequest
import com.example.helmetdetection.network.LogResponse
import com.example.helmetdetection.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private lateinit var rvLog: RecyclerView
    private lateinit var txtNoHelm: TextView
    private lateinit var txtNoStrap: TextView
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    private lateinit var tfliteHelper: TFLiteHelper
    private var lastBoxes: List<FloatArray>? = null
    private lateinit var handler: Handler
    private lateinit var frameRunnable: Runnable
    private lateinit var overlayView: OverlayView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @SuppressLint("AuthLeak")
    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvLog = view.findViewById(R.id.rvLog)
        txtNoHelm = view.findViewById(R.id.txtNoHelm)
        txtNoStrap = view.findViewById(R.id.txtNoStrap)
        playerView = view.findViewById(R.id.player_view)
        overlayView = view.findViewById(R.id.overlayView)

        val rtspUrl = "rtsp://admin123:admin123@192.168.0.10/stream1"

        player = ExoPlayer.Builder(requireContext()).build()
        playerView.player = player

        val mediaItem = MediaItem.fromUri(rtspUrl)
        val mediaSource = RtspMediaSource.Factory().createMediaSource(mediaItem)

        player!!.setMediaSource(mediaSource)
        player!!.prepare()
        player!!.playWhenReady = true

        tfliteHelper = TFLiteHelper(requireContext())

        handler = Handler(Looper.getMainLooper())
        frameRunnable = object : Runnable {
            @OptIn(UnstableApi::class)
            override fun run() {
                val textureView = playerView.videoSurfaceView as? TextureView
                if (textureView != null && textureView.isAvailable) {
                    Log.d("Frame", "TextureView tersedia")
                    val bitmap = textureView.bitmap
                    if (bitmap != null) {
                        Log.d("Frame", "Bitmap berhasil diambil")
                        processFrame(bitmap)
                    } else {
                        Log.d("Frame", "Bitmap bernilai null")
                    }
//                    bitmap?.let { processFrame(it) }
                } else {
                    Log.d("Frame", "TextureView tidak tersedia atau tidak compatible")
                }
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(frameRunnable)

        getLogData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(frameRunnable)
        tfliteHelper.close()
        player?.release()
    }

    private fun getLogData() {
        RetrofitClient.instance.getLog().enqueue(object : Callback<LogResponse> {
            override fun onResponse(call: Call<LogResponse>, response: Response<LogResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()
                    rvLog.layoutManager = LinearLayoutManager(requireContext())
                    rvLog.adapter = LogAdapter(data!!.log)

                    txtNoHelm.text = data.totalHelm.toString()
                    txtNoStrap.text = data.totalStrap.toString()
                }
            }

            override fun onFailure(call: Call<LogResponse>, t: Throwable) {
                Log.e("API_ERROR", "Gagal ambil data: ${t.message}", t)
                Toast.makeText(requireContext(), "Gagal ambil data: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun sendLog(waktu: String, statusHelm: Int, statusStrap: Int) {
        val log = LogPostRequest(waktu, statusHelm, statusStrap)

        RetrofitClient.instance.insertLog(log).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful && response.body()?.get("status") == "success") {
                    Log.d("SendDB", "Data berhasil dikirim ke server")
                    getLogData()
                } else {
                    Log.e("SendDB", "Gagal kirim data: ${response.body()}")
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                Log.e("SendDB", "Koneksi gagal: ${t.message}")
            }
        })
    }

    @SuppressLint("UseKtx")
    private fun processFrame(bitmap: Bitmap) {
        val resized = Bitmap.createScaledBitmap(bitmap, 416, 416, false)
        val input = ImageUtils.bitmapToInputArray(resized)
        val output = tfliteHelper.runInference(input)[0] // [300][6]

        Log.d("ModelOutput", "Output size: ${output.size}, data: ${output.contentDeepToString()}")

        val detectionThreshold = 0.5f
        var hasDetection = false
        var helmDetected = false
        var noHelmDetected = false
        var strapDetected = false
        var noStrapDetected = false
        val numDetections = output.size
        val detectionResults = mutableListOf<DetectionResult>()
        val filteredOutput = output.filter { it[4] > detectionThreshold }
        val currentBoxes = convertOutputToBoxes(filteredOutput.toTypedArray())

        if (lastBoxes != null) {
            Log.d("DebugDuplikat", "lastBoxes TIDAK null, jumlah box: ${lastBoxes!!.size}")
            val isDuplikat = DetectionUtils.isDuplicate(currentBoxes, lastBoxes!!)

            if (isDuplikat) {
                Log.d("Deteksi", "Deteksi duplikat, frame di-skip")
                return
            }
        } else {
            Log.d("DebugDuplikat", "lastBoxes masih NULL")
        }

        lastBoxes = currentBoxes
        Log.d("Deteksi", "Output baru: ${output.contentDeepToString()}")

        for (i in 0 until numDetections) {
            val x = output[i][0]
            val y = output[i][1]
            val w = output[i][2]
            val h = output[i][3]
            val confidence = output[i][4]
            val classId = output[i][5].toInt()

            if (confidence > detectionThreshold) {
                hasDetection = true
                Log.d("Deteksi", "Deteksi ditemukan class=$classId confidence=$confidence")

                val rect = RectF(
                    x - w / 2, // left
                    y - h / 2, // top
                    x + w / 2, // right
                    y + h / 2  // bottom
                )
                val label = when (classId) {
                    0 -> "With Chin Strap"
                    1 -> "With Helmet"
                    2 -> "Without Chin Strap"
                    3 -> "Without Helmet"
                    else -> "Unknown"
                }
                detectionResults.add(
                    DetectionResult(
                        rect = rect,
                        label = label,
                        confidence = confidence
                    )
                )
                when (classId) {
                    0 -> strapDetected = true
                    1 -> helmDetected = true
                    2 -> noStrapDetected = true
                    3 -> noHelmDetected = true
                }
            }
        }
        for (d in detectionResults) {
            Log.d("Overlay", "Rect: ${d.rect}, Label: ${d.label}, Conf: ${d.confidence}")
        }
        overlayView.setDetections(detectionResults)

        Log.d("Status", "No Helm=$noHelmDetected , With Helm=$helmDetected")
        Log.d("Status", "No Strap=$noStrapDetected , With Strap=$strapDetected")
        val statusHelm = when {
            strapDetected -> 1 // jika strap terdeteksi, berarti helm juga
            helmDetected -> 1
            noHelmDetected -> 0
            else -> 0
        }
        val statusStrap = when {
            strapDetected -> 1
            noStrapDetected -> 0
            noHelmDetected -> 0 // tidak ada helm, otomatis tidak ada strap
            else -> 1 // default aman
        }

        if (!hasDetection) {
            Log.d("Deteksi", "Tidak ada objek valid, tidak dikirim ke server")
            return
        }

        val waktuSekarang = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())

        sendLog(waktuSekarang, statusHelm, statusStrap)
    }

    fun convertOutputToBoxes(output: Array<FloatArray>): List<FloatArray> {
        val boxes = mutableListOf<FloatArray>()
        for (i in output.indices) {
            boxes.add(output[i])
        }
        return boxes
    }
}
