package com.play.window

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.play.window.capture.AudioNativeEncoder
import com.play.window.databinding.ActivityMainBinding
import com.play.window.utils.RecordUtil
import java.io.File

/**
 * 视频的播放，录制，快速，变速
 * 画面的渲染，裁剪，各种绘制
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var aacEncoder: AudioNativeEncoder? = null
    private var isEncoder = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 123)

        binding.btnTest.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 123)
            } else {
                val resList = getVideoFromSDCard()
                val videoPath = resList.find { it.endsWith("045.mp4") } ?: resList[0]
                if (resList.isNotEmpty()) {
                    BaseNative.app_Open(videoPath, "rtmp://172.16.0.97:1935/live/room")
                }
            }
        }


        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        ), 123)


        binding.btnAac.setOnClickListener {
            if(isEncoder){
                isEncoder = false
                aacEncoder?.stopAudioRecord()
            } else {
                if(aacEncoder == null){
                    aacEncoder = AudioNativeEncoder()
                }
                aacEncoder?.startAudioRecord(getAudioPath())
                isEncoder = true
            }
        }
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
}