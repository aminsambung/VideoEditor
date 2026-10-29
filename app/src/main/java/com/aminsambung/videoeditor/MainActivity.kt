package com.aminsambung.videoeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private var exoPlayer: ExoPlayer? = null
    private var videoRenderer: VideoRenderer? = null
    private var glSurfaceView: GLSurfaceView? = null
    private var btnPlayPause: ImageButton? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val videoUri: Uri? = result.data?.data
            if (videoUri != null) playVideo(videoUri)
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) loadPhoto(imageUri)
        }
    }

    private val adjustTools = listOf(
        "Eksposur", "Highlight", "Shadow", "Kontras", "Brightness", 
        "Blackpoint", "Saturation", "Vibrance", "Warm", "Tint", "Definition"
    )
    
    private val filterList = listOf(
        "Normal", "Glow", "Vivid", "Retro", "Mono", "Warm", "Cool", "Vintage"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inisialisasi ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build()
        
        // 2. Tombol Play/Pause Manual
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnPlayPause?.setOnClickListener {
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
                btnPlayPause?.setImageResource(android.R.drawable.ic_media_play)
            } else {
                exoPlayer?.play()
                btnPlayPause?.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        // Listener untuk menampilkan tombol saat video siap
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    btnPlayPause?.visibility = View.VISIBLE
                    btnPlayPause?.setImageResource(android.R.drawable.ic_media_pause)
                }
            }
        })

        // 3. Setup Tombol Navigasi Bawah
        findViewById<Button>(R.id.btnVideo).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("video/*")
            pickVideoLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnPhoto).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            pickPhotoLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnAdjust).setOnClickListener { showAdjustDialog() }
        findViewById<Button>(R.id.btnFilters).setOnClickListener { showFiltersDialog() }
    }

    // 4. Inisialisasi OpenGL di onResume (Anti-Crash untuk Android 15)
    override fun onResume() {
        super.onResume()
        if (glSurfaceView == null) {
            try {
                glSurfaceView = findViewById(R.id.glSurfaceView)
                glSurfaceView?.setEGLContextClientVersion(2)
                videoRenderer = VideoRenderer(this)
                glSurfaceView?.setRenderer(videoRenderer)
                glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            } catch (e: Exception) {
                Toast.makeText(this, "OpenGL Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        } else {
            glSurfaceView?.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView?.onPause()
    }

    private fun loadPhoto(uri: Uri) {
        exoPlayer?.pause()
        exoPlayer?.clearMediaItems()
        btnPlayPause?.visibility = View.GONE
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            videoRenderer?.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showAdjustDialog() {
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
                val value = (progress - 50).toFloat() * 2
                tvCurrentTool.text = "$currentTool: ${value.toInt()}"
                
                when (currentTool) {
                    "Eksposur" -> videoRenderer?.setExposure(value)
                    "Kontras" -> videoRenderer?.setContrast(value)
                    "Saturation" -> videoRenderer?.setSaturation(value)
                    "Warm" -> videoRenderer?.setWarmth(value)
                    "Tint" -> videoRenderer?.setTint(value)
                    "Brightness" -> videoRenderer?.setBrightness(value)
                    "Highlight" -> videoRenderer?.setHighlights(value)
                    "Shadow" -> videoRenderer?.setShadows(value)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        dialog.show()
    }

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

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = 16
            textView.layoutParams = params

            textView.setOnClickListener {
                when (filterName) {
                    "Normal" -> videoRenderer?.resetFilters()
                    "Glow" -> videoRenderer?.applyBeautyFilter()
                    "Vivid" -> videoRenderer?.applyVividFilter()
                    "Warm" -> {
                        videoRenderer?.resetFilters()
                        videoRenderer?.setWarmth(50f)
                    }
                    "Cool" -> {
                        videoRenderer?.resetFilters()
                        videoRenderer?.setWarmth(-50f)
                    }
                    "Mono" -> {
                        videoRenderer?.resetFilters()
                        videoRenderer?.setSaturation(-100f)
                    }
                }
                dialog.dismiss()
            }
            menuContainer.addView(textView)
        }
        dialog.show()
    }

    private fun playVideo(uri: Uri) {
        videoRenderer?.setVideoMode() 
        
        // KUNCI UTAMA ANDROID 15: Kirim sinyal video langsung ke Renderer.
        exoPlayer?.setVideoSurface(videoRenderer?.videoSurface)
        
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        
        exoPlayer?.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                videoRenderer?.setVideoSize(videoSize.width, videoSize.height)
            }
        })

        exoPlayer?.prepare()
        exoPlayer?.play()
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
