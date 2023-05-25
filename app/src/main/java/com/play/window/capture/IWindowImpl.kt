package com.play.window.capture

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaFormat
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLES32
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.view.Surface
import com.play.window.BaseNative
import com.play.window.WindowApp
import com.play.window.codec.H264Encoder
import com.play.window.codec.IEncoderDataListener
import com.play.window.codec.MediaPacket
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect
import com.play.window.render.SurfaceParam
import com.play.window.render.WindowRender
import com.play.window.render.gles.EglCore
import com.play.window.render.model.BitmapScene
import com.play.window.render.model.TextureInfo
import com.play.window.temp.GlUtils
import com.play.window.utils.MediaConfig
import javax.microedition.khronos.egl.EGL10
import kotlin.concurrent.thread


/**
 * User: maodayu
 * Date: 2023/4/14
 * Time: 11:55
 */
class IWindowImpl() : HandlerThread("IWindowImpl"), IWindow {

    private var windowHandler: Handler? = null
    private var windowRender:WindowRender? = null
    private var h264Encoder:H264Encoder? = null
    private val lock = Object()

    init {
        start()
    }

    private val listener = object :IEncoderDataListener{
        override fun notifyAvailableData(packet: MediaPacket) {
            Log.i(WindowApp.TAG, "notifyAvailableData: "+packet.data?.remaining()+"  pts: "+packet.pts)
            BaseNative.sendPacket(packet)
        }

        override fun notifyMediaFormat(format: MediaFormat, isVideo: Boolean) {

        }

        override fun notifyHeaderData(packet: MediaPacket) {
            Log.i(WindowApp.TAG, "sps: "+packet.csd0?.remaining()+
                "  pps:"+packet.csd1?.remaining()+"  pts:" + packet.pts)
            val config =  MediaConfig()
            BaseNative.initPublish(config.publishUrl,config.videoBitRate,config.videoFrameRate,config.videoWidth,config.videoHeight)
            BaseNative.connect()
        }

        override fun notifyEnd() {
            BaseNative.release()
        }
    }


    override fun onLooperPrepared() {
        synchronized(lock) {
            windowRender = WindowRender()
            windowHandler = object : Handler(looper) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {

                        STARTPUBLISH -> {
                            h264Encoder =  H264Encoder(MediaConfig()).apply {
                                setListener(listener)
                                windowRender?.setEncoderSurface( getEncoderSurface())
                            }
                        }

                        STOPPUBLISH -> {
                            h264Encoder?.stopEncoder()
                        }
                        PLAYVIDEO -> {
                            val info = msg.obj as DisplayInfo
                            windowRender?.addDisplaySurface(info)
                        }

                        PLAYAUDIO -> {
                           // val process = AudioProcess()
                        }

                        ADDBITMAP -> {
                            val scene = msg.obj as BitmapScene
                            windowRender?.addBitmap(scene)
                        }

                        COPYSURFACE -> {
                            val surface = msg.obj as SurfaceTexture
                            windowRender?.copyPreViewSurface(surface)
                        }
                    }
                }
            }
            lock.notifyAll()
        }
    }

    override fun startPublish() {
        windowHandler?.sendEmptyMessage(STARTPUBLISH)
    }

    override fun stopPublish() {
        windowHandler?.sendEmptyMessage(STOPPUBLISH)
    }

    override fun playVideo(surfaceTexture: SurfaceTexture): Int {
        windowHandler?.sendMessage(windowHandler!!.obtainMessage(COPYSURFACE, surfaceTexture))
        return surfaceTexture.hashCode()
    }

    override fun playVideo(info: DisplayInfo):Int{
        synchronized(lock) {
            if(windowHandler == null){
                lock.wait()
            }
            windowHandler?.let {
                it.sendMessage(it.obtainMessage(PLAYVIDEO, info))
                it.sendMessage(it.obtainMessage(PLAYAUDIO))
            }
        }
        return info.surfaceId
    }

    override fun addBitmap(bitmap: Bitmap, rect: GLRect,surfaceId:Int): Int {
        windowHandler?.let {
            it.sendMessage(it.obtainMessage(ADDBITMAP, BitmapScene(bitmap,rect).apply { this.surfaceId = surfaceId }))
        }
        return 1
    }

    override fun updateBitmap(sceneId: Int, bitmap: Bitmap, rect: GLRect, surfaceId: Int): Int {
        TODO("Not yet implemented")
    }

    override fun removeBitmap(sceneId: Int, surfaceId: Int) {
        TODO("Not yet implemented")
    }

    override fun release() {

    }


    companion object {
        private const val PLAYVIDEO = 0x11
        private const val PLAYAUDIO = 0x12
        private const val ADDBITMAP = 0x13
        private const val STARTPUBLISH = 0x14
        private const val STOPPUBLISH = 0x15
        private const val COPYSURFACE = 0x16
    }
}