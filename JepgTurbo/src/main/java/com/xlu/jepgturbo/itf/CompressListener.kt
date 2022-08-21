package com.xlu.jepgturbo.itf

/**
 * @ClassName CompressListener
 * @Description 压缩result监听
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/7/14 9:53
 */
interface CompressListener<T> {

    fun onStart()

    fun onSuccess(result:T)

    fun onFailed(error:String)

}