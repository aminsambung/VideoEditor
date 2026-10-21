package com.aminsambung.videoeditor

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.Surface
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VideoRenderer(context: Context) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private val gpuImage = GPUImage(context)
    private var surfaceTexture: SurfaceTexture? = null
    var videoSurface: Surface? = null
        private set

    // --- DAFTAR FILTER ---
    private val contrastFilter = GPUImageContrastFilter(1.0f)
    private val brightnessFilter = GPUImageBrightnessFilter(0.0f)
    private val saturationFilter = GPUImageSaturationFilter(1.0f)
    
    private val exposureFilter = GPUImageExposureFilter(0.0f)
    private val whiteBalanceFilter = GPUImageWhiteBalanceFilter(5000f, 0.0f)
    private val highlightShadowFilter = GPUImageHighlightShadowFilter(0.0f, 1.0f)
    
    // Vibrance tidak ada di GPUImage standar, kita gunakan Saturation sebagai pengganti sementara
    // agar menu Vibrance bisa berfungsi (efeknya mirip).
    private val vibranceFilter = GPUImageSaturationFilter(1.0f) 

    init {
        gpuImage.setFilter(contrastFilter)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        gpuImage.init()
        
        val textureId = gpuImage.getTextureId()
        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture?.setOnFrameAvailableListener(this)
        videoSurface = Surface(surfaceTexture)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        gpuImage.onOutputSizeChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        surfaceTexture?.updateTexImage()
        gpuImage.onDrawFrame(null)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {}

    // ==========================================
    // FUNGSI UNTUK MENGUBAH NILAI FILTER
    // ==========================================
    
    fun setContrast(value: Float) {
        contrastFilter.setContrast(1.0f + (value / 50.0f))
        gpuImage.setFilter(contrastFilter)
    }

    fun setBrightness(value: Float) {
        brightnessFilter.setBrightness(value / 50.0f)
        gpuImage.setFilter(brightnessFilter)
    }

    fun setSaturation(value: Float) {
        saturationFilter.setSaturation(1.0f + (value / 50.0f))
        gpuImage.setFilter(saturationFilter)
    }

    fun setExposure(value: Float) {
        exposureFilter.setExposure(value / 50.0f)
        gpuImage.setFilter(exposureFilter)
    }

    fun setVibrance(value: Float) {
        // Menggunakan filter Saturation sebagai pengganti Vibrance
        vibranceFilter.setSaturation(1.0f + (value / 50.0f))
        gpuImage.setFilter(vibranceFilter)
    }

    fun setWarmth(value: Float) {
        // 5000 adalah suhu normal. Nilai negatif membuat video lebih biru (dingin), positif lebih kuning (hangat)
        val temperature = 5000f + (value * 50f)
        whiteBalanceFilter.setTemperature(temperature)
        gpuImage.setFilter(whiteBalanceFilter)
    }

    fun setHighlight(value: Float) {
        // Highlight dari 0.0 (gelap) ke 1.0 (normal)
        highlightShadowFilter.setHighlights(1.0f - (value / 100.0f))
        gpuImage.setFilter(highlightShadowFilter)
    }
}
