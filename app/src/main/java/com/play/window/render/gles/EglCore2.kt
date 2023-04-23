package com.play.window.render.gles

import android.graphics.SurfaceTexture
import android.opengl.*
import android.util.Log
import android.view.Surface

/**
 *  EGL作为平台和OpenGL ES之间的中间件，使得OpenGL ES能够绘制到当前平台
 *  EGLContext一次只能附着在一个线程上
 */
class EglCore2(shareEGLContext: EGLContext = EGL14.EGL_NO_CONTEXT) {

    companion object {
        private const val TAG = "EglCore"
    }

     var mEGLDisplay = EGL14.EGL_NO_DISPLAY
    private var mEGLContext = EGL14.EGL_NO_CONTEXT
    private var mEGLConfig: EGLConfig?

    init {
        /**
         * 1. 创建与本地窗口系统之间的连接，返回的EGLDisplay对象，可以抽象理解为设备的显示设备
         */
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "EGL already set up")
        }

        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "创建EGLDisplay失败")
        }

        /**
         * 2. 初始化，可以获取EGL的主次版本号
         */
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null
            Log.e(TAG, "unable to initialize EGL14")
        }

        /**
         * 3. 选择渲染表面的配置
         */
        // 1. 定义EGLConfig配置
        val mEGLConfigAttr = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_STENCIL_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE, 0,
            EGL14.EGL_NONE
        )
        // 2. 所有符合配置的EGLConfig
        val configs = arrayOfNulls<EGLConfig>(1)
        // 3. 所有符合的EGLConfig个数
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(mEGLDisplay, mEGLConfigAttr, 0, configs, 0, configs.size, numConfigs, 0)) {
            Log.e(TAG, "EGLConfig选择配置失败")
        }
        mEGLConfig = configs[0]

        /**
         * 4. 有了EGLDisplay和EGLConfig就可以配置渲染表面EGLSurface和渲染上下文EGLContext
         */
        mEGLContext = EGL14.EGL_NO_CONTEXT
        //创建上下文环境时，需要创建属性信息用于指定OpenGL使用版本
        val mEGLContextAttr = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE
        )

        // 可以传入一个已经创建的EglContext，会返回一个共享的上下文
        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, mEGLConfig, shareEGLContext, mEGLContextAttr, 0)
        //GlUtils.checkGlError("eglCreateContext ")

        val values = intArrayOf(1)
        EGL14.eglQueryContext(mEGLDisplay, mEGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0)
        Log.e(TAG, "EGLContext created, client version: " + values[0])
    }

    /**
     * 创建渲染表面
     * EGL提供了两种创建渲染表面的方式，一种是可见的，渲染到屏幕上，一种是不可见的也即是离屏渲染
     */
    fun createPbufferSurface(width: Int, height: Int): EGLSurface? {
        val surfaceAttr = intArrayOf(
            EGL14.EGL_WIDTH, width,
            EGL14.EGL_HEIGHT, height,
            EGL14.EGL_NONE
        )
        val mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, surfaceAttr, 0)
        //GlUtils.checkGlError("eglCreatePbufferSurface ")
        return mEGLSurface
    }

    fun createWindowSurface(surface: Any): EGLSurface? {
        return if (surface is Surface || surface is SurfaceTexture) {
            val surfaceAttr = intArrayOf(EGL14.EGL_NONE)
            val mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface, surfaceAttr, 0)
           // GlUtils.checkGlError("eglCreateWindowSurface ")
            mEGLSurface
        } else {
            Log.e(TAG, "createWindowSurface: surface is invalid")
            null
        }
    }

    /**
     * 共享的EGLContext
     * 如果设置EGL14.EGL_NO_CONTEXT表示非共享的EGLContext
     * 在创建EGL环境时，可以设置共享的EGLContext，这样在共享的EGLContext中创建的纹理就可以在当前EGL下使用
     */
    fun getShareContext():EGLContext{

        return mEGLContext
    }

    /**
     * 绑定上下文环境
     * 绑定上下文环境后，就可以执行具体的绘制操作，调用OpenGL相关的方法绘制图形
     * 注意：必须在切换到当前上下文后，才可以执行OpenGL的函数
     */
    fun makeCurrent(eglSurface: EGLSurface?) {
        if(mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "makeCurrent: 需要先创建mEGLDisplay")
        }
        if(!EGL14.eglMakeCurrent(mEGLDisplay, eglSurface?:EGL14.EGL_NO_SURFACE, eglSurface?:EGL14.EGL_NO_SURFACE, mEGLContext)){
            Log.e(TAG, "makeCurrent: failed")
        }
    }

    /**
     * 交换缓冲
     * 绘制结束后，通过交换缓冲将绘制结果显示到屏幕上
     */
    fun swapBuffers(eglSurface: EGLSurface) {
        if (!EGL14.eglSwapBuffers(mEGLDisplay, eglSurface)) {
            Log.e(TAG, "swapBuffers 错误")
        }
    }

    /**
     * 释放操作,必须在创建Context的线程调用
     * 绘制结束时，进行的释放操作，将对象释放掉
     */
    fun release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(mEGLDisplay)
        }
        mEGLDisplay = EGL14.EGL_NO_DISPLAY
        mEGLContext = EGL14.EGL_NO_CONTEXT
        mEGLConfig = null
    }


    fun releaseSurface(eglSurface: EGLSurface) {
        if(mEGLDisplay != EGL14.EGL_NO_DISPLAY){
            EGL14.eglDestroySurface(mEGLDisplay, eglSurface)
        }
    }

}
