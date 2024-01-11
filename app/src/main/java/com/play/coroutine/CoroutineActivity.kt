package com.play.coroutine

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.play.window.R
import kotlinx.coroutines.*

/**
 *  用于研究协程的用法
 *
 */
class CoroutineActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "MDY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        yieldFun()
    }


    /**
     * delay的用法
     * 暂停当前协程，但不会影响当前线程
     */
    private fun delayFun(){
        Log.i(TAG, "delayFun: start")
        GlobalScope.launch {
            Log.i(TAG, "delayFun: 1")
            delay(2000)
            Log.i(TAG, "delayFun: 2")
        }
        Log.i(TAG, "delayFun: out")
    }

    /**
     * withContext的用法
     */
    private fun withContextFun(){
        MainScope().launch {
            Log.i(TAG, "withContextFun: start")
            withContext(Dispatchers.Default){
                delayFun()
            }
            Log.i(TAG, "withContextFun: end")
        }
    }

    /**
     * yield的用法
     * 挂起当前协程，等到外部协程执行完毕后再执行
     */
    private fun yieldFun(){
        runBlocking {

            Log.i(TAG, "yieldFun: start")
            val job1 = launch {
                Log.i(TAG, "job1:  1")
                yield()
                Log.i(TAG, "job1:  2")
            }


            val job2 = launch {
                Log.i(TAG, "job2:  1")
                yield()
                Log.i(TAG, "job2:  2")
            }

            Log.i(TAG, "yieldFun: end")
        }
    }
}