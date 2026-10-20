package com.aminsambung.videoeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private var exoPlayer: ExoPlayer? = null
    
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val videoUri: Uri? = result.data?.data
            if (videoUri != null) {
                playVideo(videoUri)
            }
        }
    }

    // Daftar Menu Editing
    private val editingTools = listOf(
        "Eksposur", "Highlight", "Shadow", "Kontras", "Brightness", 
        "Blackpoint", "Saturation", "Vibrance", "Warm", "Tint", "Definition"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Setup Pemutar Video
        exoPlayer = ExoPlayer.Builder(this).build()
        val playerView = findViewById<PlayerView>(R.id.playerView)
        playerView.player = exoPlayer

        // 2. Tombol Pilih Video
        val btnPickVideo = findViewById<Button>(R.id.btnPickVideo)
        btnPickVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("video/*")
            pickVideoLauncher.launch(intent)
        }

        // 3. Generate Menu Editing Secara Otomatis
        val menuContainer = findViewById<LinearLayout>(R.id.menuContainer)
        val tvContrastValue = findViewById<TextView>(R.id.tvContrastValue)
        val seekBarContrast = findViewById<SeekBar>(R.id.seekBarContrast)

        // Loop untuk membuat tombol menu
        for (tool in editingTools) {
            val textView = TextView(this)
            textView.text = tool
            textView.setTextColor(Color.WHITE)
            textView.textSize = 14f
            textView.setPadding(32, 16, 32, 16)
            
            // Beri jarak antar menu
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = 16
            textView.layoutParams = params

            // Beri garis bawah (indikator) jika menu sedang dipilih
            if (tool == "Kontras") {
                textView.setTypeface(null, Typeface.BOLD)
                textView.setTextColor(Color.parseColor("#00FFFF")) // Warna Cyan
                tvContrastValue.text = "$tool: 0"
            }

            // Aksi saat menu diklik
            textView.setOnClickListener {
                // Reset semua warna teks
                for (i in 0 until menuContainer.childCount) {
                    val child = menuContainer.getChildAt(i) as TextView
                    child.setTextColor(Color.WHITE)
                    child.setTypeface(null, Typeface.NORMAL)
                }
                // Sorot menu yang diklik
                textView.setTextColor(Color.parseColor("#00FFFF"))
                textView.setTypeface(null, Typeface.BOLD)
                
                // Ubah teks judul slider
                tvContrastValue.text = "$tool: 0"
                // Reset posisi slider ke tengah
                seekBarContrast.progress = 50
            }
            
            menuContainer.addView(textView)
        }

        // 4. Logika Slider
        seekBarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress - 50 // Nilai dari -50 sampai 50
                val currentToolName = tvContrastValue.text.toString().split(":")[0]
                tvContrastValue.text = "$currentToolName: $value"
                
                // TODO: Terapkan nilai 'value' ini ke Engine Video (GPUImage)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun playVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
