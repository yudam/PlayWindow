package com.play.window

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.QuickViewHolder
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }


    class FunctionAdapter(dataList: List<Function>) : BaseQuickAdapter<Function, QuickViewHolder>(dataList) {

        override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder {

            return QuickViewHolder(R.layout.func_list_item, parent)
        }


        override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: Function?) {
            val btnItem = holder.getView<Button>(R.id.btn_item)
            btnItem.text = item?.name
        }

    }


    data class Function(
        val name: String,
        val target: String,
    )

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