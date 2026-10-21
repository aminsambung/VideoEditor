package com.aminsambung.videoeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VideoRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private var surfaceTexture: SurfaceTexture? = null
    var videoSurface: Surface? = null
        private set

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var textureBuffer: FloatBuffer

    private var videoProgram = 0
    private var photoProgram = 0

    private var videoWidth = 0
    private var videoHeight = 0
    private var screenWidth = 0
    private var screenHeight = 0

    // --- Variabel Filter ---
    private var exposure = 0.0f
    private var contrast = 1.0f
    private var saturation = 1.0f
    private var warmth = 0.0f
    private var tint = 0.0f
    private var brightness = 0.0f
    private var highlights = 0.0f // Nilai 0 = normal
    private var shadows = 0.0f    // Nilai 0 = normal

    private var isPhotoMode = false
    private var photoTextureId = -1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        videoProgram = createProgram(isPhoto = false)
        photoProgram = createProgram(isPhoto = true)

        val textures = floatArrayOf(
            0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
        )
        textureBuffer = ByteBuffer.allocateDirect(textures.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(textures); position(0) }

        val textureId = IntArray(1)
        GLES20.glGenTextures(1, textureId, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        surfaceTexture = SurfaceTexture(textureId[0])
        videoSurface = Surface(surfaceTexture)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        screenWidth = width
        screenHeight = height
        updateVertexBuffer()
    }

    fun setVideoSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            videoWidth = width
            videoHeight = height
            updateVertexBuffer()
        }
    }

    fun setImageBitmap(bitmap: Bitmap) {
        isPhotoMode = true
        if (photoTextureId != -1) {
            GLES20.glDeleteTextures(1, intArrayOf(photoTextureId), 0)
        }

        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        photoTextureId = texIds[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, photoTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        setVideoSize(bitmap.width, bitmap.height)
    }

    fun setVideoMode() {
        isPhotoMode = false
    }

    private fun updateVertexBuffer() {
        if (videoWidth == 0 || videoHeight == 0 || screenWidth == 0 || screenHeight == 0) return

        val videoAspect = videoWidth.toFloat() / videoHeight
        val screenAspect = screenWidth.toFloat() / screenHeight

        var scaleX = 1f
        var scaleY = 1f

        if (videoAspect > screenAspect) {
            scaleY = screenAspect / videoAspect
        } else {
            scaleX = videoAspect / screenAspect
        }

        val vertices = floatArrayOf(
            -scaleX, -scaleY, scaleX, -scaleY,
            -scaleX,  scaleY, scaleX,  scaleY
        )
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(vertices); position(0) }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (isPhotoMode) {
            drawPhoto()
        } else {
            drawVideo()
        }
    }

    private fun drawVideo() {
        surfaceTexture?.updateTexImage()
        GLES20.glUseProgram(videoProgram)
        setShaderParameters(videoProgram)
        drawQuad(videoProgram)
    }

    private fun drawPhoto() {
        if (photoTextureId == -1) return
        GLES20.glUseProgram(photoProgram)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, photoTextureId)
        setShaderParameters(photoProgram)
        drawQuad(photoProgram)
    }

    private fun drawQuad(program: Int) {
        val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val textureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(textureHandle)
        GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureHandle)
    }

    private fun setShaderParameters(program: Int) {
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uExposure"), exposure)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uContrast"), contrast)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uSaturation"), saturation)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uWarmth"), warmth)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uTint"), tint)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uBrightness"), brightness)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uHighlights"), highlights)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uShadows"), shadows)
    }

    // --- FUNGSI FILTER (Nilai -100 sampai 100) ---
    fun setExposure(value: Float) { exposure = value / 100.0f }
    fun setContrast(value: Float) { contrast = 1.0f + (value / 100.0f) }
    fun setSaturation(value: Float) { saturation = 1.0f + (value / 100.0f) }
    fun setWarmth(value: Float) { warmth = value / 100.0f }
    fun setTint(value: Float) { tint = value / 100.0f }
    
    fun setBrightness(value: Float) { brightness = value / 100.0f }
    fun setHighlights(value: Float) { highlights = value / 100.0f }
    fun setShadows(value: Float) { shadows = value / 100.0f }

    // PRESET FILTER VIVID (Mirip Gambar 2)
    fun applyVividFilter() {
        resetFilters()
        saturation = 1.8f   // Warna sangat pekat
        contrast = 1.3f     // Kontras tinggi
        exposure = 0.15f    // Sedikit lebih terang
        brightness = 0.1f   // Sedikit lebih cerah
        highlights = -0.3f  // Turunkan highlight agar awan tidak pecah
        shadows = 0.1f      // Naikkan shadow agar area gelap tidak terlalu hitam
    }

    fun applyBeautyFilter() {
        exposure = 0.15f
        contrast = 0.9f
        saturation = 0.9f
        warmth = -0.1f
        tint = 0.2f
    }

    fun resetFilters() {
        exposure = 0.0f
        contrast = 1.0f
        saturation = 1.0f
        warmth = 0.0f
        tint = 0.0f
        brightness = 0.0f
        highlights = 0.0f
        shadows = 0.0f
    }

    // --- PEMBUATAN SHADER (DENGAN HIGHLIGHT & SHADOW) ---
    private fun createProgram(isPhoto: Boolean): Int {
        val vertexShaderCode = """
            attribute vec4 aPosition;
            attribute vec2 aTextureCoord;
            varying vec2 vTextureCoord;
            void main() {
                gl_Position = aPosition;
                vTextureCoord = aTextureCoord;
            }
        """.trimIndent()

        val samplerType = if (isPhoto) "sampler2D" else "samplerExternalOES"
        val extension = if (isPhoto) "" else "#extension GL_OES_EGL_image_external : require\n"

        val fragmentShaderCode = """
            $extension
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform $samplerType sTexture;
            
            uniform float uExposure;
            uniform float uContrast;
            uniform float uSaturation;
            uniform float uWarmth;
            uniform float uTint;
            uniform float uBrightness;
            uniform float uHighlights;
            uniform float uShadows;

            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                vec3 rgb = color.rgb;

                // 1. Exposure & Brightness
                rgb = rgb + uExposure + (uBrightness * 0.5);

                // 2. Highlights & Shadows (Kunci efek Vivid)
                // Hitung luminance (tingkat kecerahan)
                float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
                
                // Terapkan Highlight (mempengaruhi area terang)
                rgb = rgb + (uHighlights * smoothstep(0.5, 1.0, luma));
                
                // Terapkan Shadow (mempengaruhi area gelap)
                rgb = rgb + (uShadows * (1.0 - smoothstep(0.0, 0.5, luma)));

                // 3. Contrast
                rgb = (rgb - 0.5) * uContrast + 0.5;

                // 4. Saturation
                float finalLuma = dot(rgb, vec3(0.299, 0.587, 0.114));
                rgb = mix(vec3(finalLuma), rgb, uSaturation);

                // 5. Warmth & Tint
                rgb.r = rgb.r + (uWarmth * 0.2);
                rgb.b = rgb.b - (uWarmth * 0.2);
                rgb.g = rgb.g - (uTint * 0.2);

                gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), color.a);
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}
