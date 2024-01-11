package com.play.window

import android.Manifest
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import com.play.window.capture.AudioNativeEncoder
import com.play.window.capture.IWindowImpl
import com.play.window.capture.VideoPlayer
import com.play.window.databinding.ActivityWindowPlayBinding
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect
import com.play.window.model.PreviewInfo
import java.io.File

class WindowPlayActivity : AppCompatActivity() {

    private val videoPath = "https://storage.googleapis.com/exoplayer-test-media-1/mkv/android-screens-lavf-56.36.100-aac-avc-main-1280x720.mkv"

    private val mVideoPath1 = "file:///android_asset/byteflow/one_piece.mp4"
    private val mVideoPath2 = "file:///android_asset/byteflow/midway.mp4"
    private val mVideoPath3 = "file:///android_asset/byteflow/vr.mp4"

    private lateinit var binding: ActivityWindowPlayBinding

    private var window: IWindowImpl? = null

    private var isRecord: Boolean = false

    private var isPublish:Boolean = false

    private val streamInfo = mutableListOf<DisplayInfo>()

    private var recordAudio: AudioNativeEncoder? = null


    inner class PlaySurfaceListener(val view: View, var url: String?) : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            val centerX = width / 2f
            val centerY = height / 2f
            val rect = GLRect(centerX, centerY, width.toFloat(), height.toFloat(), width.toFloat(), height.toFloat())

            if (url == null) {
                initPlayer(surface,rect,null)
            } else {
                initPlayer(surface, rect, url!!)
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
        initStream()
        binding.playVideo.surfaceTextureListener = PlaySurfaceListener(binding.playVideo,null)
        binding.tvTopLeft.surfaceTextureListener = PlaySurfaceListener(binding.tvTopLeft, mVideoPath2)
        binding.tvTopRight.surfaceTextureListener = PlaySurfaceListener(binding.tvTopRight, mVideoPath1)

        binding.btnPlay.setOnClickListener {
            if (isRecord) {
                isRecord = false
                showToast("停止录制")
                window?.stopRecord()
            } else {
                isRecord = true
                showToast("开始录制")
                window?.startRecord(getVideoPath())
            }
        }

        binding.btnLive.setOnClickListener {
            if(!isPublish){
                isPublish = true
                showToast("开始推流")
                window?.startPublish()
            } else {
                isPublish = false
                showToast("结束推流")
                window?.stopPublish()
            }
        }

        binding.btnRecordAudio.setOnClickListener {
            if (recordAudio == null) {
                showToast("开始录音")
                recordAudio = AudioNativeEncoder()
                recordAudio?.startAudioRecord(getAudioPath())
            } else {
                showToast("结束录音")
                recordAudio?.stopAudioRecord()
                recordAudio = null
            }
        }

        binding.tvTopLeft.setOnClickListener {
            val previewSurfaceId = streamInfo[1].surfaceId
            window?.addScene(previewSurfaceId)
        }

        binding.tvTopRight.setOnClickListener {
            val previewSurfaceId = streamInfo[2].surfaceId
            window?.addScene(previewSurfaceId)
        }

        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
            ), 123)
    }



    private fun initStream(){
        window = IWindowImpl()
        window?.addOutPut()
    }


    private fun initPlayer(surfaceTexture: SurfaceTexture, rect: GLRect, url: String?) {

        val info = DisplayInfo(rect, 30).also {
            it.url = url
            it.surfaceTexture = surfaceTexture
            it.isOutPut = url == null
        }

        if (window == null) {
            window = IWindowImpl()
        }
        window?.playVideo(info)
        streamInfo.add(info)
        Log.i("SMP", "initPlayer.surfaceId: "+info.surfaceId)
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

    private fun getAudioPath(): String {
        val path = cacheDir.absolutePath + "/audiofile2.aac"
        val file = File(path)
        if (!file.exists()) {
            file.createNewFile()
        } else {
            file.delete()
            file.createNewFile()
        }
        return path
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}