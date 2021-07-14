package com.xlu.jepgturbo

/**
 * @ClassName CompressListener
 * @Description TODO
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/7/14 9:53
 */
interface CompressListener {

    fun onStart()

    fun onCompleted(result:Boolean,filePath:String)

}