package com.aminsambung.videoeditor

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val seekBarContrast = findViewById<SeekBar>(R.id.seekBarContrast)
        val tvContrastValue = findViewById<TextView>(R.id.tvContrastValue)

        // Listener saat slider digeser
        seekBarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Ubah nilai progress (0-100) menjadi nilai kontras (-1.0 sampai 1.0)
                // 50 = Normal (0), 0 = -1.0, 100 = 1.0
                val contrastValue = (progress - 50) / 50.0f
                
                tvContrastValue.text = "Contrast: ${String.format("%.1f", contrastValue)}"
                
                // TODO: Kirim nilai contrastValue ini ke GPUImage/OpenGL Renderer
                // untuk mengubah warna video secara real-time.
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}
