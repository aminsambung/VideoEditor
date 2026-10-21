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
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private var exoPlayer: ExoPlayer? = null
    private var videoRenderer: VideoRenderer? = null
    private var glSurfaceView: GLSurfaceView? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Launcher untuk memilih video dari galeri
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val videoUri: Uri? = result.data?.data
            if (videoUri != null) playVideo(videoUri)
        }
    }

    // Daftar Menu untuk Panel Adjust
    private val adjustTools = listOf(
        "Eksposur", "Highlight", "Shadow", "Kontras", "Brightness", 
        "Blackpoint", "Saturation", "Vibrance", "Warm", "Tint", "Definition"
    )

    // Daftar Menu untuk Panel Filters
    private val filterList = listOf(
        "Normal", "Vivid", "Retro", "Mono", "Warm", "Cool", "Vintage"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Setup OpenGL & Video Renderer
        glSurfaceView = findViewById(R.id.glSurfaceView)
        glSurfaceView?.setEGLContextClientVersion(2)
        
        videoRenderer = VideoRenderer(this)
        glSurfaceView?.setRenderer(videoRenderer)
        glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        // 2. Inisialisasi ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build()

        // 3. Setup Kontrol Player (Auto-Hide)
        val playerView = findViewById<PlayerView>(R.id.playerView)
        playerView.player = exoPlayer
        playerView.setShutterBackgroundColor(Color.TRANSPARENT)
        playerView.controllerShowTimeoutMs = 3000
        
        playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            if (visibility == View.VISIBLE) {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({ playerView.hideController() }, 3000)
            }
        })

        // 4. Tombol Pilih Video
        findViewById<Button>(R.id.btnPickVideo).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("video/*")
            pickVideoLauncher.launch(intent)
        }

        // 5. Tombol Navigasi Bawah: Buka Panel Adjust
        findViewById<Button>(R.id.btnAdjust).setOnClickListener {
            showAdjustDialog()
        }

        // 6. Tombol Navigasi Bawah: Buka Panel Filters
        findViewById<Button>(R.id.btnFilters).setOnClickListener {
            showFiltersDialog()
        }
    }

    // ==========================================
    // FUNGSI MEMUNCULKAN PANEL ADJUST
    // ==========================================
    private fun showAdjustDialog() {
        // Menggunakan tema BottomSheet yang sudah diperbaiki
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_adjust, null)
        dialog.setContentView(view)

        val tvCurrentTool = view.findViewById<TextView>(R.id.tvCurrentTool)
        val seekBarAdjust = view.findViewById<SeekBar>(R.id.seekBarAdjust)
        val menuContainer = view.findViewById<LinearLayout>(R.id.menuContainerAdjust)

        var currentTool = "Kontras"

        for (tool in adjustTools) {
            val textView = TextView(this)
            textView.text = tool
            textView.setTextColor(Color.WHITE)
            textView.textSize = 14f
            textView.setPadding(32, 16, 32, 16)
            
            if (tool == "Kontras") {
                textView.setTypeface(null, Typeface.BOLD)
                textView.setTextColor(Color.parseColor("#00FFFF"))
                tvCurrentTool.text = "$tool: 0"
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
                tvCurrentTool.text = "$currentTool: 0"
                seekBarAdjust.progress = 50
            }
            menuContainer.addView(textView)
        }

        seekBarAdjust.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = (progress - 50).toFloat()
                tvCurrentTool.text = "$currentTool: ${value.toInt()}"
                
                // TODO: Nanti kita sambungkan ke VideoRenderer untuk efek video
                // when (currentTool) {
                //     "Kontras" -> videoRenderer?.setContrast(value)
                // }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialog.show()
    }

    // ==========================================
    // FUNGSI MEMUNCULKAN PANEL FILTERS
    // ==========================================
    private fun showFiltersDialog() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_filters, null)
        dialog.setContentView(view)

        val menuContainer = view.findViewById<LinearLayout>(R.id.menuContainerFilters)

        for (filterName in filterList) {
            val textView = TextView(this)
            textView.text = filterName
            textView.setTextColor(Color.WHITE)
            textView.textSize = 14f
            textView.setPadding(32, 16, 32, 16)
            textView.setBackgroundColor(Color.parseColor("#444444"))

            // Beri margin antar tombol filter
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = 16
            textView.layoutParams = params

            textView.setOnClickListener {
                // TODO: Terapkan filter ke videoRenderer nanti
                // videoRenderer?.applyFilter(filterName)
                dialog.dismiss()
            }
            menuContainer.addView(textView)
        }

        dialog.show()
    }

    // ==========================================
    // FUNGSI MEMUTAR VIDEO
    // ==========================================
    private fun playVideo(uri: Uri) {
        exoPlayer?.setVideoSurface(videoRenderer?.videoSurface)
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        
        // Listener untuk mendapatkan ukuran video (agar tidak memanjang)
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
