package com.play.window

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.play.window.databinding.ActivityMainBinding

/**
 * 视频的播放，录制，快速，变速
 *
 * 音频的播放，录制，混音，变速
 *
 * 画面的渲染，裁剪，各种绘制
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.sampleText.text = stringFromJNI()
    }

    /**
     * A native method that is implemented by the 'window' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'window' library on application startup.
        init {
            System.loadLibrary("window")
        }
    }
}