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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private var exoPlayer: ExoPlayer? = null
    private var videoRenderer: VideoRenderer? = null
    private var glSurfaceView: GLSurfaceView? = null
    
    private val handler = Handler(Looper.getMainLooper())
    
    // Launcher untuk memilih video dari galeri
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val videoUri: Uri? = result.data?.data
            if (videoUri != null) {
                playVideo(videoUri)
            }
        }
    }

    // Daftar Menu Editor
    private val editingTools = listOf(
        "Eksposur", "Highlight", "Shadow", "Kontras", "Brightness", 
        "Blackpoint", "Saturation", "Vibrance", "Warm", "Tint", "Definition"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inisialisasi OpenGL & Video Renderer
        glSurfaceView = findViewById<GLSurfaceView>(R.id.glSurfaceView)
        glSurfaceView?.setEGLContextClientVersion(2)
        
        videoRenderer = VideoRenderer(this)
        glSurfaceView?.setRenderer(videoRenderer)
        // Gunakan CONTINUOUSLY agar video berjalan mulus
        glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        // 2. Inisialisasi ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build()

        // 3. Setup Kontrol Player (Auto-Hide)
        val playerView = findViewById<PlayerView>(R.id.playerView)
        playerView.player = exoPlayer
        playerView.setShutterBackgroundColor(Color.TRANSPARENT)
        playerView.controllerShowTimeoutMs = 3000 // Sembunyikan setelah 3 detik
        
        // Listener saat kontrol muncul/hilang
        playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            if (visibility == View.VISIBLE) {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({ playerView.hideController() }, 3000)
            }
        })

        // 4. Tombol Pilih Video
        val btnPickVideo = findViewById<Button>(R.id.btnPickVideo)
        btnPickVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("video/*")
            pickVideoLauncher.launch(intent)
        }

        // 5. Generate Menu UI & Slider Logic
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
                
                // TODO: Nanti kita akan hubungkan ini ke VideoRenderer
                // when (currentTool) {
                //     "Kontras" -> videoRenderer?.setContrast(value)
                // }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // Fungsi untuk memutar video
    private fun playVideo(uri: Uri) {
        // Hubungkan ExoPlayer ke Surface GPU kita
        exoPlayer?.setVideoSurface(videoRenderer?.videoSurface)
        
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        
        // PENTING: Kirim ukuran video ke Renderer agar aspek rasio tidak gepeng
        exoPlayer?.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                videoRenderer?.setVideoSize(videoSize.width, videoSize.height)
            }
        })

        exoPlayer?.prepare()
        exoPlayer?.play()
        
        // Sembunyikan kontrol setelah 3 detik
        handler.postDelayed({ findViewById<PlayerView>(R.id.playerView).hideController() }, 3000)
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}
