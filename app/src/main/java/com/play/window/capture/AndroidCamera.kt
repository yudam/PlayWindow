package com.play.window.capture

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.util.Log
import android.view.SurfaceHolder
import com.play.window.BaseNative

/**
 * User: maodayu
 * Date: 2023/5/19
 * Time: 10:01
 */
class AndroidCamera : ICamera {

    /**
     * Camera默认返回的是NV21格式数据，也就是YUV420SP格式
     */
    private var camera: Camera? = null
    private val rtmpUrl = "rtmp://172.16.0.97:1935/live/room"
    private var preViewSize: Camera.Size? = null

    private var firstFind :Boolean= true

    override fun startPublish() {
        preViewSize?.let {
            BaseNative.startPublic(rtmpUrl, 1920, 1080)
        }
    }

    override fun openCamera(surface: SurfaceTexture) {
        camera = Camera.open(getCameraInfo())
        camera?.setPreviewTexture(surface)
        camera?.setPreviewCallback { data, camera ->
            BaseNative.setVideoData(data, data.size)
        }
        BaseNative.startPublic(rtmpUrl, 1920, 1080)
        camera?.startPreview()
    }

    override fun openCamera(surface: SurfaceHolder) {
        camera = Camera.open(getCameraInfo())
        val parameters = camera?.parameters
        parameters?.previewFormat = ImageFormat.NV21
        camera?.parameters = parameters
        camera?.setPreviewDisplay(surface)
        camera?.setDisplayOrientation(90)
        camera?.setPreviewCallback { data, camera ->
            if(firstFind){
                firstFind = false
                val rate = camera.parameters.previewFrameRate
                Log.i("MDY", "openCamera: "+rate)
            }
            BaseNative.setVideoData(data, data.size)
        }
        getFps()
        BaseNative.startPublic(rtmpUrl, 1920, 1080)
        camera?.startPreview()
    }

    override fun closeCamera() {
        BaseNative.stopPublish()
        camera?.stopPreview()
        camera?.release()
        camera = null
    }


    /**
     *  默认获取前置摄像头
     */
    private fun getCameraInfo(facing: Int = CameraInfo.CAMERA_FACING_FRONT): Int {
        val numberOfCameras = Camera.getNumberOfCameras()
        for (cameraId in 0 until numberOfCameras) {
            val cameraInfo = CameraInfo()
            Camera.getCameraInfo(cameraId, cameraInfo)
            if (cameraInfo.facing == facing) return cameraId
        }
        return -1
    }

    private fun getFps(){
      val frameRateList =   camera?.parameters?.supportedPreviewFrameRates
        frameRateList?.forEach {
            Log.i("MDY", "supportedPreviewFrameRates: " + it)
        }


      val fpsRangeList =   camera?.parameters?.supportedPreviewFpsRange
        fpsRangeList?.forEach {
            Log.i("MDUY", "supportedPreviewFpsRange: "+it[0]+"   "+it[1])
        }
    }
}