package com.play.window.temp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import com.play.window.capture.VideoPlayer
import com.play.window.databinding.ActivityVideoPlayBinding
import kotlin.concurrent.thread

/**
 *  本地视频解码播放
 *
 */
class VideoPlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.videoView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Log.i("VideoPlayActivity", "onSurfaceTextureAvailable: ")
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Log.i("VideoPlayActivity", "onSurfaceTextureSizeChanged: ")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.i("VideoPlayActivity", "onSurfaceTextureDestroyed: ")
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                Log.i("VideoPlayActivity", "onSurfaceTextureUpdated: ")
            }

        }

        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA),
            123)

        binding.btnStart.setOnClickListener {
            val resList = getVideoFromSDCard()
            val videoPath = resList.find { it.endsWith("33.mp4") } ?: resList[0]
            //val audioPlayer = AudioPlayer()
            //val audioDecoder = AudioDecoder(videoPath, audioPlayer)

            val surfaceTexture = binding.videoView.surfaceTexture


            val pipScene = PipScene(surfaceTexture!!)
            pipScene.start()
            val surface = Surface(pipScene.getSurfaceTexture())
//            val videoDecoder = VideoDecoder(videoPath, surface)
//            setVideoDisplay(pipScene, videoDecoder.getVideoWidth(), videoDecoder.getVideoHeight())
////            thread {
////                audioDecoder.run()
////            }
//
//            thread {
//                videoDecoder.run()
//            }

            VideoPlayer(videoPath,surface)
        }


        binding.btnEnd.setOnClickListener {

        }
    }


    private fun setVideoDisplay(pipScene: PipScene,videoWidth: Int, videoHeight: Int) {
        val params = binding.videoView.layoutParams
        val aspect = videoWidth.toDouble() / videoHeight
        val viewAspect = binding.videoView.width.toDouble() / binding.videoView.height
        val diff = aspect / viewAspect - 1

        Log.e("MDY", "viewAspect: "+viewAspect+"    wid:"+binding.videoView.width+"   hei:"+binding.videoView.height)
        if (diff > 0) {
            params.height = (binding.videoView.width.toDouble() / aspect).toInt()
            params.width = binding.videoView.width
        } else {
            params.width = (binding.videoView.height * aspect).toInt()
            params.height = binding.videoView.height
        }
        pipScene.setVideoSize(params.width,params.height)
        binding.videoView.layoutParams = params
    }


    private fun getVideoFromSDCard(): List<String> {
        var list = ArrayList<String>(10)
        var projection: Array<String> = arrayOf(MediaStore.Video.Media.DATA)
        var cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null,
            null, null) ?: return emptyList()
        while (cursor.moveToNext()) {
            var path = cursor.getString(cursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
            list.add(path)
        }
        cursor.close()
        return list
    }
}