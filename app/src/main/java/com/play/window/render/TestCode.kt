package com.play.window.render

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.play.window.WindowApp
import com.play.window.capture.VideoPlayer
import com.play.window.render.gles.EglCore2

/**
 * User: maodayu
 * Date: 2023/4/17
 * Time: 20:13
 */
class TestCode(val url:String) : HandlerThread("TestCode") {

    private var mEglCore: EglCore2? = null
    private var mEGLSurface: EGLSurface? = null
    private var mHandler: Handler? = null

    init {
        start()
    }

    override fun onLooperPrepared() {
        super.onLooperPrepared()
        mEglCore = EglCore2()
       // mEGLSurface = mEglCore?.createWindowSurface(surface)
       // mEglCore?.makeCurrent(mEGLSurface!!)
        mHandler = Handler(looper) {


            true
        }

        val sceneId = getTexture(null,true)
        Log.i(WindowApp.TAG, "onLooperPrepared: "+sceneId)
        val  mSurfaceTexture = SurfaceTexture(sceneId)
        mSurfaceTexture.setOnFrameAvailableListener {
           it.updateTexImage()
        }
       // initCodec(mSurfaceTexture)

        VideoPlayer(url, Surface(mSurfaceTexture))
    }



    fun initCodec(surfaceTexture: SurfaceTexture) {
         val mMimeType = "video/avc"
         val mMediaCodec = MediaCodec.createDecoderByType(mMimeType)

        val mediaFormat = MediaFormat.createVideoFormat(mMimeType, 1080, 1920)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1080 * 1920 * 5)
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
        mMediaCodec.configure(mediaFormat, Surface(surfaceTexture), null, 0)
        mMediaCodec.start()
    }


    fun getTexture(bitmap: Bitmap? = null, isOES: Boolean = false): Int {
        val intArray = IntArray(1)
        GLES20.glGenTextures(1, intArray, 0)
        if (isOES) {
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, intArray[0])
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE)
            bitmap?.let {
                GLUtils.texImage2D(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0, it, 0)
            }
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, intArray[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE)
            bitmap?.let {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, it, 0)
            }
        }
        return intArray[0]
    }
}