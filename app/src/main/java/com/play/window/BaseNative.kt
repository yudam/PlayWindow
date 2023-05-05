package com.play.window

/**
 * User: maodayu
 * Date: 2023/4/25
 * Time: 10:18
 */

object BaseNative {

   fun  loadLibrary(){
       System.loadLibrary("window")
   }


    external fun stringFromJNI(): String

    /**
     * 本地视频RTMP推流
     */
    external fun app_Open(infilename:String,outfilename:String)
    external fun app_Push()
    external fun app_close()

}