package com.play.window

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.app.ActivityCompat
import com.play.window.capture.AndroidCamera
import com.play.window.databinding.ActivityCameraBinding

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    private val camera = AndroidCamera()

    private val callback = object :SurfaceHolder.Callback{
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.i("MDY", "surfaceCreated: ")
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            Log.i("MDY", "surfaceChanged: ")
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Log.i("MDY", "surfaceDestroyed: ")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.surfaceview.holder.addCallback(callback)



        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
            ), 123)



        binding.startCamera.setOnClickListener {
            camera.openCamera(binding.surfaceview.holder)
        }

        binding.stopCamera.setOnClickListener {
            camera.closeCamera()
        }
    }



}