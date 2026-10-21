package com.aminsambung.videoeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView // Tambahkan import ini

class MainActivity : AppCompatActivity() {

    private var exoPlayer: ExoPlayer? = null
    private var videoRenderer: VideoRenderer? = null
    private var glSurfaceView: GLSurfaceView? = null
    
    // FITUR AUTO-HIDE KONTROL (Handler untuk timer)
    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable {
        // Kode untuk menyembunyikan kontrol (kita panggil melalui PlayerView nanti)
        exoPlayer?.let { 
            // Jika kita menggunakan PlayerView, kita bisa pakai ini:
            // findViewById<PlayerView>(R.id.playerView).hideController()
        }
    }
    
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val videoUri: Uri? = result.data?.data
            if (videoUri != null) {
                playVideo(videoUri)
            }
        }
    }

    private val editingTools = listOf(
        "Eksposur", "Highlight", "Shadow", "Kontras", "Brightness", 
        "Blackpoint", "Saturation", "Vibrance", "Warm", "Tint", "Definition"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val videoContainer = findViewById<FrameLayout>(R.id.videoContainer)
        glSurfaceView = GLSurfaceView(this)
        glSurfaceView?.setEGLContextClientVersion(2)
        
        videoRenderer = VideoRenderer(this)
        glSurfaceView?.setRenderer(videoRenderer)
        glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        
        videoContainer.addView(glSurfaceView)

        // 1. Setup ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build()
        
        // =====================================================================
        // FITUR AUTO-HIDE KONTROL (MENGGUNAKAN CUSTOM PLAYER VIEW)
        // Karena kita menggunakan GLSurfaceView (bukan PlayerView), kita harus 
        // membuat "Controller" ExoPlayer muncul di atas GLSurfaceView.
        // =====================================================================
        
        // Kita gunakan PlayerView bawaan Media3, tapi kita sembunyikan videonya (karena sudah di-render GPU)
        val playerView = findViewById<PlayerView>(R.id.playerView)
        playerView.player = exoPlayer
        
        // Sembunyikan Surface video bawaan PlayerView (biarkan GLSurfaceView yang menampilkan)
        playerView.setShutterBackgroundColor(Color.TRANSPARENT)
        
        // Atur agar kontrol otomatis hilang setelah 3 detik
        playerView.controllerShowTimeoutMs = 3000 
        
        // Atur agar saat layar di-tap, kontrol muncul/kembali
        playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            if (visibility == View.VISIBLE) {
                // Jika kontrol muncul, jalankan timer 3 detik untuk menyembunyikannya lagi
                handler.removeCallbacks(hideControlsRunnable)
                handler.postDelayed({ playerView.hideController() }, 3000)
            }
        })

        // Tombol Pilih Video
        val btnPickVideo = findViewById<Button>(R.id.btnPickVideo)
        btnPickVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("video/*")
            pickVideoLauncher.launch(intent)
        }

        // Sisa kode UI (Menu & Slider) tetap sama seperti sebelumnya...
        val menuContainer = findViewById<LinearLayout>(R.id.menuContainer)
        val tvContrastValue = findViewById<TextView>(R.id.tvContrastValue)
        val seekBarContrast = findViewById<SeekBar>(R.id.seekBarContrast)

        var currentTool = "Kontras"

        for (tool in editingTools) {
            val textView = TextView(this)
            textView.text = tool
            textView.setTextColor(Color.WHITE)
            textView.textSize = 14f
            textView.setPadding(32, 16, 32, 16)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = 16
            textView.layoutParams = params

            if (tool == "Kontras") {
                textView.setTypeface(null, Typeface.BOLD)
                textView.setTextColor(Color.parseColor("#00FFFF"))
                tvContrastValue.text = "$tool: 0"
            }

            textView.setOnClickListener {
                for (i in 0 until menuContainer.childCount) {
                    val child = menuContainer.getChildAt(i) as TextView
                    child.setTextColor(Color.WHITE)
                    child.setTypeface(null, Typeface.NORMAL)
                }
                textView.setTextColor(Color.parseColor("#00FFFF"))
                textView.setTypeface(null, Typeface.BOLD)
                currentTool = tool
                tvContrastValue.text = "$currentTool: 0"
                seekBarContrast.progress = 50
            }
            menuContainer.addView(textView)
        }

        seekBarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress - 50).toFloat()
                tvContrastValue.text = "$currentTool: ${value.toInt()}"
                
                // Hapus semua pemanggilan filter untuk sementara
// until video display works
when (currentTool) {
    "Kontras" -> { /* TODO: Implement filter */ }
    "Brightness" -> { /* TODO: Implement filter */ }
    "Saturation" -> { /* TODO: Implement filter */ }
}
                glSurfaceView?.requestRender()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun playVideo(uri: Uri) {
        exoPlayer?.setVideoSurface(videoRenderer?.videoSurface)
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()
        
        // Sembunyikan kontrol saat video mulai diputar
        handler.postDelayed({ findViewById<PlayerView>(R.id.playerView).hideController() }, 3000)
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
        handler.removeCallbacks(hideControlsRunnable) // Hentikan timer saat aplikasi di background
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        handler.removeCallbacks(hideControlsRunnable)
    }
}
