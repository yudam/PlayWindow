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
    }


    companion object {
        private var mWindowApp: WindowApp? = null
        fun instance(): WindowApp {
            return mWindowApp!!
        }
    }
}