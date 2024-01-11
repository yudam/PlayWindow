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
import com.google.android.exoplayer2.audio.AudioGet
import com.play.window.BaseNative
import com.play.window.WindowApp
import com.play.window.codec.AACEncoder
import com.play.window.codec.H264Encoder
import com.play.window.codec.IEncoderDataListener
import com.play.window.codec.MediaPacket
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect
import com.play.window.model.PreviewInfo
import com.play.window.muxer.RecordMedia
import com.play.window.render.SurfaceParam
import com.play.window.render.WindowRender
import com.play.window.render.gles.EglCore
import com.play.window.render.model.BitmapScene
import com.play.window.render.model.TextureInfo
import com.play.window.temp.GlUtils
import com.play.window.utils.MediaConfig
import com.play.window.utils.MuxerUtil
import com.play.window.utils.RecordUtil
import java.io.File
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
    private var aacEncoder:AACEncoder? = null
    private val lock = Object()

    private var record: RecordMedia? = null


    init {
        start()
        AudioGet.registerListener { buffer, size ->
            aacEncoder?.frameBuffer(buffer)
        }
    }

    private val listener = object :IEncoderDataListener{
        override fun notifyAvailableData(packet: MediaPacket) {
            Log.i(WindowApp.TAG, "notifyAvailableData: "+packet.data?.remaining()+"  pts: "+packet.pts+"  isVideo:"+packet.isVideo)

            if(record != null){
                record?.addPacket(packet)
            } else {
                BaseNative.sendPacket(packet)
            }
        }

        override fun notifyMediaFormat(format: MediaFormat, isVideo: Boolean) {
            record?.addTrack(format,!isVideo)
        }

        override fun notifyHeaderData(packet: MediaPacket) {
            Log.i("JNILOG", "sps: "+packet.csd0?.remaining()+
                "  pps:"+packet.csd1?.remaining()+"  pts:" + packet.pts)
            if(record != null){
                record?.addPacket(packet)
            } else {
                BaseNative.sendPacket(packet)
            }
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

                        OutPutSurface -> {
                          windowRender?.addOriginalSurface()
                        }

                        AddScene -> {
                            val surfaceId = msg.obj as Int
                            windowRender?.addScene(WindowRender.centreSurfaceId,surfaceId)

                        }
                        PREVIEWSURFACE -> {
                            val info = msg.obj as DisplayInfo
                            windowRender?.addDisplaySurface(info)
                        }

                        STARTPUBLISH -> {
                            Log.i(WindowApp.TAG, "STARTPUBLISH: ")

                            val config = msg.obj as MediaConfig
                            BaseNative.initPublish(config.publishUrl,config.videoBitRate,
                                config.videoFrameRate,config.videoWidth,config.videoHeight)
                            BaseNative.connect()
                            h264Encoder =  H264Encoder(MediaConfig()).apply {
                                setListener(listener)
                                windowRender?.setEncoderSurface( getEncoderSurface(),getRect())
                            }

//                            aacEncoder = AACEncoder(getAudioPath())
//                            aacEncoder?.setListener(listener)
//                            aacEncoder?.start()
                        }

                        STOPPUBLISH -> {
                            h264Encoder?.stopEncoder()
                            BaseNative.release()
                        }

                        RECORDAV -> {
                            val path = msg.obj as String
                            record = RecordMedia(path)
                            h264Encoder =  H264Encoder(MediaConfig()).apply {
                                setListener(listener)
                                windowRender?.setEncoderSurface( getEncoderSurface(),getRect())
                            }

                            aacEncoder = AACEncoder(getAudioPath())
                            aacEncoder?.setListener(listener)
                            aacEncoder?.start()
                        }

                        STOPRECOR -> {
                            record?.release()
                            h264Encoder?.stopEncoder()
                            aacEncoder?.stopEncoder()
                        }

                        PLAYVIDEO -> {
                            val info = msg.obj as DisplayInfo
                            windowRender?.addDisplaySurface(info)
                        }

                        PLAYAUDIO -> {
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


    fun addOutPut(){
        synchronized(lock) {
            if (windowHandler == null) {
                lock.wait()
            }
            val message = Message().apply {
                what = OutPutSurface
            }
            windowHandler?.sendMessage(message)
        }
    }

    /**
     * 添加流场景到Output中
     */
    fun addScene(surfaceId:Int){
        val message =  Message().apply {
            obj = surfaceId
            what = AddScene
        }
        windowHandler?.sendMessage(message)
    }

    override fun previewSurface(info : DisplayInfo) {
        val message =  Message().apply {
            obj = info
            what = PREVIEWSURFACE
        }
        windowHandler?.sendMessage(message)
    }

    override fun startPublish() {
        val message =  Message().apply {
            obj = MediaConfig()
            what = STARTPUBLISH
        }
        windowHandler?.sendMessage(message)
    }

    override fun stopPublish() {
        windowHandler?.sendEmptyMessage(STOPPUBLISH)
    }

    override fun startRecord(path: String) {
       val message =  Message().apply {
            obj = path
            what = RECORDAV
        }
        windowHandler?.sendMessage(message)
    }

    override fun stopRecord() {
        val message =  Message().apply {
            what = STOPRECOR
        }
        windowHandler?.sendMessage(message)
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

    private fun getAudioPath(): String {
        val path = WindowApp.instance().cacheDir.absolutePath + "/audioverify.aac"
        val file = File(path)
        if (!file.exists()) {
            file.createNewFile()
        } else {
            file.delete()
            file.createNewFile()
        }
        return path
    }

    companion object {
        private const val PLAYVIDEO = 0x11
        private const val PLAYAUDIO = 0x12
        private const val ADDBITMAP = 0x13
        private const val STARTPUBLISH = 0x14
        private const val STOPPUBLISH = 0x15
        private const val COPYSURFACE = 0x16
        private const val RECORDAV = 0x17
        private const val STOPRECOR = 0x18
        private const val PREVIEWSURFACE = 0x19

        private const val OutPutSurface = 1
        private const val AddScene = 2
    }
}