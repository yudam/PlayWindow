package com.play.window

import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.util.EGLSurfaceTexture
import com.google.android.exoplayer2.util.GlUtil
import com.play.window.capture.IWindowImpl
import com.play.window.capture.VideoPlayer
import com.play.window.databinding.ActivityWindowPlayBinding
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.thread

class WindowPlayActivity : AppCompatActivity() {

    private val url = "https://storage.googleapis.com/exoplayer-test-media-1/mkv/android-screens-lavf-56.36.100-aac-avc-main-1280x720.mkv"

    private lateinit var binding: ActivityWindowPlayBinding

    private var player: VideoPlayer? = null

    private  var window :IWindowImpl? = null

    private var surfaceId:Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWindowPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.i(WindowApp.TAG, "onCreate: ")

        binding.playVideo.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Log.i(WindowApp.TAG, "onSurfaceTextureAvailable: ")
                val rect = GLRect(width / 2f, height / 2f, width.toFloat(), height.toFloat(),width.toFloat(),height.toFloat())
                initPlayer(surface, rect)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }

        }

        binding.btnEnd.setOnClickListener {

           val option =  BitmapFactory.Options().apply {
                inSampleSize = 8
            }
            val bitmap =   BitmapFactory.decodeResource(resources,R.mipmap.ic_1)
            val width = binding.playVideo.width
            val height = binding.playVideo.height
            val rect = GLRect(width / 2f, height / 2f, bitmap.width.toFloat(), bitmap.height.toFloat(),width.toFloat(),height.toFloat())
            window?.addBitmap(bitmap,rect,surfaceId)
        }

        binding.startPublish.setOnClickListener {
            window?.startPublish()
        }


        binding.start2.setOnClickListener {
           val  surfaceTexture = binding.playVideo2.surfaceTexture
            Log.i(WindowApp.TAG, "surfaceTexture: $surfaceTexture")
            if(surfaceTexture != null){
                window?.playVideo(surfaceTexture)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(WindowApp.TAG, "onStart: " + player)
    }

    override fun onResume() {
        super.onResume()
        Log.i(WindowApp.TAG, "onResume: " + player)
    }

    override fun onPause() {
        super.onPause()
        Log.i(WindowApp.TAG, "onPause: " + player)
    }

    private fun initPlayer(surfaceTexture: SurfaceTexture, rect: GLRect) {
        Log.i(WindowApp.TAG, "initPlayer: "+Thread.currentThread().name)
        window = IWindowImpl()
        val info = DisplayInfo(url,rect,surfaceTexture,30)
        surfaceId = window!!.playVideo(info)
    }

}