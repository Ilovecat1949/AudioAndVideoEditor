package com.example.audioandvideoeditor.transcoder.video.gl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.os.Build
import android.util.Log
import android.view.Surface

/**
 * 离屏 EGL 上下文核心管理类（标准 EGL 1.4 高兼容版）
 * 负责在后台线程初始化 OpenGL ES 环境，并绑定 MediaCodec 的 Input Surface
 */
class EglCore(sharedContext: EGLContext? = null, flags: Int = 0) {

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglConfig: EGLConfig? = null

    init {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            throw IllegalStateException("EGL already set up")
        }

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            eglDisplay = EGL14.EGL_NO_DISPLAY
            throw RuntimeException("unable to initialize EGL14")
        }

        // 挑选配置（支持 Recordable 回退策略，确保兼容模拟器与各种芯片）
        eglConfig = chooseConfig(flags)

        val attribList = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )

        val rootContext = sharedContext ?: EGL14.EGL_NO_CONTEXT
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, rootContext, attribList, 0)
        checkEglError("eglCreateContext")
    }

    /**
     * 具备回退保护的 Config 查找机制
     */
    private fun chooseConfig(flags: Int): EGLConfig {
        val recordableKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            EGLExt.EGL_RECORDABLE_ANDROID
        } else {
            0x3142
        }

        // 优先使用带 RECORDABLE 标志的属性列表
        val attribListWithRecordable = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            recordableKey, 1,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)

        if (EGL14.eglChooseConfig(eglDisplay, attribListWithRecordable, 0, configs, 0, configs.size, numConfigs, 0)
            && numConfigs[0] > 0 && configs[0] != null) {
            return configs[0]!!
        }

        Log.w(TAG, "EGL_RECORDABLE_ANDROID not supported on this driver, falling back to standard RGB888.")

        // 降级机制：移除 RECORDABLE 约束，重新匹配标准 RGB888
        val attribListFallback = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )

        if (!EGL14.eglChooseConfig(eglDisplay, attribListFallback, 0, configs, 0, configs.size, numConfigs, 0)
            || numConfigs[0] <= 0 || configs[0] == null) {
            throw RuntimeException("unable to find RGB888 / 2 EGLConfig")
        }

        return configs[0]!!
    }

    /**
     * 创建基于 Native Window/Surface 的 EGLSurface（例如 MediaCodec Encoder 的 InputSurface）
     */
    fun createWindowSurface(surface: Any): EGLSurface {
        if (surface !is Surface && surface !is SurfaceTexture) {
            throw IllegalArgumentException("invalid surface: $surface")
        }

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttribs, 0)
        checkEglError("eglCreateWindowSurface")
        if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("surface was null")
        }
        return eglSurface
    }

    /**
     * 创建基于内存的离屏 Pbuffer Surface（供初始化或状态预热使用）
     */
    fun createOffscreenSurface(width: Int, height: Int): EGLSurface {
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        val eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttribs, 0)
        checkEglError("eglCreatePbufferSurface")
        if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("surface was null")
        }
        return eglSurface
    }

    /**
     * 将当前的渲染上下文切换到指定的 EGLSurface 上
     */
    fun makeCurrent(eglSurface: EGLSurface) {
        if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
            Log.d(TAG, "NOTE: makeCurrent w/o display")
        }
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    /**
     * 将当前线程的 EGL 上下文置空
     */
    fun makeNothingCurrent() {
        if (!EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    /**
     * 交换双缓冲区，将绘制好的纹理提交给 MediaCodec InputSurface
     */
    fun swapBuffers(eglSurface: EGLSurface): Boolean {
        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    /**
     * 为视频帧打上精确的 Presentation Time Stamp (单位: 纳秒)
     */
    fun setPresentationTime(eglSurface: EGLSurface, nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
    }

    /**
     * 销毁特定的 EGLSurface
     */
    fun releaseSurface(eglSurface: EGLSurface) {
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
    }

    /**
     * 释放所有 EGL 资源
     */
    fun release() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglConfig = null
    }

    private fun checkEglError(msg: String) {
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            throw RuntimeException("$msg: EGL error: 0x${Integer.toHexString(error)}")
        }
    }

    companion object {
        private const val TAG = "EglCore"
    }
}