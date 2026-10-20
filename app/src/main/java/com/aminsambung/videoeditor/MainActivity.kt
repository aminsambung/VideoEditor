package com.aminsambung.videoeditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

    // Variabel untuk ExoPlayer (Pemutar Video)
    private var exoPlayer: ExoPlayer? = null
    
    // Variabel untuk memilih video dari galeri
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val videoUri: Uri? = result.data?.data
            if (videoUri != null) {
                playVideo(videoUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inisialisasi Pemutar Video
        exoPlayer = ExoPlayer.Builder(this).build()
        val playerView = findViewById<PlayerView>(R.id.playerView)
        playerView.player = exoPlayer

        // 2. Tombol Pilih Video
        val btnPickVideo = findViewById<Button>(R.id.btnPickVideo)
        btnPickVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("video/*") // Hanya menampilkan file video
            pickVideoLauncher.launch(intent)
        }

        // 3. Logika Slider Kontras
        val seekBarContrast = findViewById<SeekBar>(R.id.seekBarContrast)
        val tvContrastValue = findViewById<TextView>(R.id.tvContrastValue)

        seekBarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val contrastValue = (progress - 50) / 50.0f
                tvContrastValue.text = "Contrast: ${String.format("%.1f", contrastValue)}"
                
                // TODO: Nanti kita akan pasang filter GPUImage di sini
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // Fungsi untuk memutar video
    private fun playVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play() // Otomatis mainkan video
    }

    // Hentikan pemutar saat aplikasi ditutup agar tidak boros baterai
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
