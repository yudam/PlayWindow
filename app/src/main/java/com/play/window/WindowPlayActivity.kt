package com.play.window

import android.Manifest
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.TextureView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.play.window.capture.IWindowImpl
import com.play.window.capture.VideoPlayer
import com.play.window.databinding.ActivityWindowPlayBinding
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect
import java.io.File

class WindowPlayActivity : AppCompatActivity() {

    private val videoPath = "https://storage.googleapis.com/exoplayer-test-media-1/mkv/android-screens-lavf-56.36.100-aac-avc-main-1280x720.mkv"

    private val mVideoPath1 = Environment.getExternalStorageDirectory().absolutePath + "/byteflow/one_piece.mp4"
    private val mVideoPath2 = Environment.getExternalStorageDirectory().absolutePath + "/byteflow/midway.mp4"
    private val mVideoPath3 = Environment.getExternalStorageDirectory().absolutePath + "/byteflow/vr.mp4"


    private lateinit var binding: ActivityWindowPlayBinding

    private var player: VideoPlayer? = null

    private var window: IWindowImpl? = null

    private var isRecord:Boolean = false

    private val streamInfo = mutableListOf<DisplayInfo>()


    inner class PlaySurfaceListener(val view: View,var url:String = videoPath) : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            val centerX = width / 2f
            val centerY = height / 2f
            val rect = GLRect(centerX, centerY, width.toFloat(), height.toFloat(), width.toFloat(), height.toFloat())

            if(view == binding.playVideo){
                initPlayer(surface, rect, url)
            } else {
                binding.root.postDelayed({
                    initPlayer(surface, rect, url)
                },3000)
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWindowPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.i(WindowApp.TAG, "onCreate: ")

        binding.playVideo.surfaceTextureListener = PlaySurfaceListener(binding.playVideo,videoPath)
//        binding.tvTopLeft.surfaceTextureListener = PlaySurfaceListener(binding.tvTopLeft,videoPath)
//        binding.tvTopRight.surfaceTextureListener = PlaySurfaceListener(binding.tvTopRight,videoPath)
//        binding.tvBottomLeft.surfaceTextureListener = PlaySurfaceListener(binding.tvBottomLeft,videoPath)
//        binding.tvBottomRight.surfaceTextureListener = PlaySurfaceListener(binding.tvBottomRight,videoPath)

        binding.btnTopLeft.setOnClickListener {
        }

        binding.btnTopRight.setOnClickListener {

        }

        binding.btnBottomLeft.setOnClickListener {

        }

        binding.btnBottomRight.setOnClickListener {

        }

        binding.btnPlay.setOnClickListener {
           if(isRecord){
               isRecord = false
               window?.stopRecord()
           } else {
               isRecord = true
               window?.startRecord(getVideoPath())
           }
        }

        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
            ), 123)
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

    private fun initPlayer(surfaceTexture: SurfaceTexture, rect: GLRect, url: String) {
        val info = DisplayInfo(rect, 30).also {
            it.url = url
            it.surfaceTexture = surfaceTexture
        }

        if (window == null) {
            window = IWindowImpl()
        }
        window?.playVideo(info)
        streamInfo.add(info)
    }


    private fun getVideoPath(): String {
        val path = cacheDir.absolutePath + "/audiofile2.mp4"
        val file = File(path)
        if (!file.exists()) {
            file.createNewFile()
        } else {
            file.delete()
            file.createNewFile()
        }
        return path
    }

}