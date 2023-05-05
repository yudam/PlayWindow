package com.play.window

import android.app.Application

/**
 * User: maodayu
 * Date: 2023/4/10
 * Time: 19:25
 */
class WindowApp : Application() {

    override fun onCreate() {
        super.onCreate()
        mWindowApp = this
        BaseNative.loadLibrary()
    }


    companion object {

        const val TAG = "WindowApp"

        private var mWindowApp: WindowApp? = null

        fun instance(): WindowApp {
            return mWindowApp!!
        }
    }
}