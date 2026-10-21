seekBarContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        val value = (progress - 50).toFloat() // Nilai -50 sampai 50
        tvContrastValue.text = "$currentTool: ${value.toInt()}"
        
        // Hubungkan setiap menu ke fungsinya masing-masing
        when (currentTool) {
            "Kontras" -> videoRenderer?.setContrast(value)
            "Brightness" -> videoRenderer?.setBrightness(value)
            "Saturation" -> videoRenderer?.setSaturation(value)
            "Eksposur" -> videoRenderer?.setExposure(value)
            "Vibrance" -> videoRenderer?.setVibrance(value)
            "Warm" -> videoRenderer?.setWarmth(value)
            "Highlight" -> videoRenderer?.setHighlight(value)
        }
        
        // Wajib dipanggil agar layar langsung berubah
        glSurfaceView?.requestRender()
    }
    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
})
