package com.example.audioandvideoeditor.transcoder.video.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * GPU 离屏纹理渲染器（标准 GLES20 高兼容版）
 * 负责接收解码器 SurfaceTexture (OES 纹理) 图像，并忠实地直通渲染/拉伸至目标 EGL Surface
 */
class TextureRender {

    private val vertexBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer

    private var program = 0
    private var aPositionHandle = 0
    private var aTextureCoordHandle = 0
    private var uSTMatrixHandle = 0

    init {
        // 1. 顶点坐标 (铺满全屏 2D 矩形)
        vertexBuffer = ByteBuffer.allocateDirect(FULL_RECTANGLE_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(FULL_RECTANGLE_COORDS)
        vertexBuffer.position(0)

        // 2. 纹理坐标 (0.0 到 1.0)
        textureBuffer = ByteBuffer.allocateDirect(FULL_RECTANGLE_TEX_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(FULL_RECTANGLE_TEX_COORDS)
        textureBuffer.position(0)
    }

    /**
     * 生成并绑定 OES 外部纹理 ID
     */
    fun createTextureObject(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        checkGlError("glBindTexture textureId")

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        checkGlError("glTexParameter")

        return textureId
    }

    /**
     * 初始化 Shader 着色器程序
     */
    fun surfaceCreated() {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) {
            throw RuntimeException("failed creating program")
        }

        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        checkLocation(aPositionHandle, "aPosition")
        aTextureCoordHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        checkLocation(aTextureCoordHandle, "aTextureCoord")
        uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")
        checkLocation(uSTMatrixHandle, "uSTMatrix")
    }

    /**
     * 渲染单帧图像到当前目标 Viewport
     * @param targetWidth 目标输出宽
     * @param targetHeight 目标输出高
     */
    fun drawFrame(
        st: SurfaceTexture,
        textureId: Int,
        transformMatrix: FloatArray,
        targetWidth: Int = 0,
        targetHeight: Int = 0
    ) {
        checkGlError("onDrawFrame start")

        // 提取 SurfaceTexture 变换矩阵
        st.getTransformMatrix(transformMatrix)

        // 显式重置 Viewport，防止跨 Context 渲染时缩放错乱
        if (targetWidth > 0 && targetHeight > 0) {
            GLES20.glViewport(0, 0, targetWidth, targetHeight)
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)
        checkGlError("glUseProgram")

        // 激活 OES 纹理单元
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // 传顶点坐标
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aPositionHandle, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPositionHandle)

        // 传纹理坐标
        textureBuffer.position(0)
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer)
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle)

        // 传变换矩阵
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, transformMatrix, 0)

        // 执行直通绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTextureCoordHandle)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUseProgram(0)
    }

    /**
     * 显式回收 GPU Program 资源
     */
    fun release() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = GLES20.glCreateShader(shaderType)
        checkGlError("glCreateShader type=$shaderType")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader $shaderType:")
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            shader = 0
        }
        return shader
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) return 0

        var program = GLES20.glCreateProgram()
        checkGlError("glCreateProgram")
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)
            checkGlError("glAttachShader")
            GLES20.glAttachShader(program, pixelShader)
            checkGlError("glAttachShader")
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ")
                Log.e(TAG, GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }

    private fun checkLocation(location: Int, label: String) {
        if (location < 0) {
            throw RuntimeException("Unable to locate '$label' in program")
        }
    }

    private fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$op: glError 0x${Integer.toHexString(error)}")
        }
    }

    companion object {
        private const val TAG = "TextureRender"

        private val FULL_RECTANGLE_COORDS = floatArrayOf(
            -1.0f, -1.0f, // 左下
            1.0f, -1.0f, // 右下
            -1.0f,  1.0f, // 左上
            1.0f,  1.0f  // 右上
        )

        private val FULL_RECTANGLE_TEX_COORDS = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )

        private const val VERTEX_SHADER =
            "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n"

        private const val FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n"
    }
}