package com.xlu.jepgturbo

/**
 * @ClassName CompressListener
 * @Description TODO
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/7/14 9:53
 */
interface CompressListener<T> {

    fun onStart()

    fun onCompleted(success: Boolean, result:T?)

}