package com.aminsambung.videoeditor

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
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
    private var program = 0

    private var videoWidth = 0
    private var videoHeight = 0
    private var screenWidth = 0
    private var screenHeight = 0

    // Variabel untuk Filter (Default: Normal)
    private var exposure = 0.0f
    private var contrast = 1.0f
    private var saturation = 1.0f
    private var warmth = 0.0f // Positif = Hangat/Kuning, Negatif = Dingin/Biru
    private var tint = 0.0f   // Positif = Magenta/Pink (bagus untuk kulit), Negatif = Hijau

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        program = createProgram()

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
        surfaceTexture?.updateTexImage()

        GLES20.glUseProgram(program)

        // Kirim nilai filter ke GPU (Fragment Shader)
        val exposureHandle = GLES20.glGetUniformLocation(program, "uExposure")
        val contrastHandle = GLES20.glGetUniformLocation(program, "uContrast")
        val saturationHandle = GLES20.glGetUniformLocation(program, "uSaturation")
        val warmthHandle = GLES20.glGetUniformLocation(program, "uWarmth")
        val tintHandle = GLES20.glGetUniformLocation(program, "uTint")

        GLES20.glUniform1f(exposureHandle, exposure)
        GLES20.glUniform1f(contrastHandle, contrast)
        GLES20.glUniform1f(saturationHandle, saturation)
        GLES20.glUniform1f(warmthHandle, warmth)
        GLES20.glUniform1f(tintHandle, tint)

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

    // --- FUNGSI UNTUK MENGUBAH FILTER ---
    fun setExposure(value: Float) {
        // Slider -50 s/d 50 diubah jadi -1.0 s/d 1.0
        exposure = value / 50.0f
    }
    
    fun setContrast(value: Float) {
        contrast = 1.0f + (value / 50.0f)
    }

    fun setSaturation(value: Float) {
        saturation = 1.0f + (value / 50.0f)
    }

    fun setWarmth(value: Float) {
        // Positif = Hangat, Negatif = Dingin
        warmth = value / 50.0f
    }

    fun setTint(value: Float) {
        // Positif = Pink/Magenta (mencerahkan kulit), Negatif = Hijau
        tint = value / 50.0f
    }

    // Fungsi Preset: Filter "Glow" untuk Kulit Cerah (Mirip Gambar 2 & 3)
    fun applyBeautyFilter() {
        exposure = 0.15f   // Sedikit lebih terang
        contrast = 0.9f    // Sedikit kurangi kontras agar bayangan tidak terlalu tajam
        saturation = 0.9f  // Sedikit kurangi saturasi agar warna tidak terlalu "norak"
        warmth = -0.1f     // Sedikit lebih dingin (mengurangi warna kuning/kusam)
        tint = 0.2f        // Tambahkan sedikit warna pink/magenta (kunci kulit glowing)
    }
    
    // Fungsi Reset ke Normal
    fun resetFilters() {
        exposure = 0.0f
        contrast = 1.0f
        saturation = 1.0f
        warmth = 0.0f
        tint = 0.0f
    }

    private fun createProgram(): Int {
        val vertexShaderCode = """
            attribute vec4 aPosition;
            attribute vec2 aTextureCoord;
            varying vec2 vTextureCoord;
            void main() {
                gl_Position = aPosition;
                vTextureCoord = aTextureCoord;
            }
        """.trimIndent()

        // --- INI ADALAH BAGIAN PALING PENTING (FRAGMENT SHADER) ---
        val fragmentShaderCode = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform samplerExternalOES sTexture;
            
            uniform float uExposure;
            uniform float uContrast;
            uniform float uSaturation;
            uniform float uWarmth;
            uniform float uTint;

            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                vec3 rgb = color.rgb;

                // 1. Exposure
                rgb = rgb + uExposure;

                // 2. Contrast
                rgb = (rgb - 0.5) * uContrast + 0.5;

                // 3. Saturation
                float luminance = dot(rgb, vec3(0.299, 0.587, 0.114));
                rgb = mix(vec3(luminance), rgb, uSaturation);

                // 4. Warmth (Geser Red & Blue)
                rgb.r = rgb.r + (uWarmth * 0.2);
                rgb.b = rgb.b - (uWarmth * 0.2);

                // 5. Tint (Geser Green & Red/Blue untuk Pink/Magenta)
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
