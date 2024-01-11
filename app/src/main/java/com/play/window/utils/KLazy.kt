package com.play.window.utils

/**
 * User: maodayu
 * Date: 2023/8/22
 * Time: 11:13
 */
object KLazy {

    val lock = Object()

    /**
     * Lazy懒加载默认同步加载
     */

    val manager:String by lazy {
        "are you ok"
    }


    val netSet : Int by lazy(lock){
        -1
    }

    /**
     *  LazyThreadSafetyMode.SYNCHRONIZED  默认加载方式，线程安全
     *  LazyThreadSafetyMode.PUBLICATION   采用CAS保证数据的安全，但可能多次调用初始化，至于第一次有效，线程安全
     *  LazyThreadSafetyMode.NONE 多次调用初始化，会更新数据，线程不安全
     */
    val nList : Long by lazy(LazyThreadSafetyMode.NONE){
        -1L
    }
}