package com.xlu.compress.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * @ClassName TimeUtil
 * @Description TODO
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/7/13 14:43
 */
object TimeUtil {

    /**
     * 日期格式字符串转换成时间戳
     * @param date 字符串日期
     * @param format 如：yyyy-MM-dd HH:mm:ss
     * @return
     */
    fun getCurentTime(): String {
        val timeStamp = System.currentTimeMillis() //获取当前时间戳
        //SimpleDateFormat("yyyy 年 MM 月 dd 日 HH 时 mm 分 ss 秒")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val sd = sdf.format(Date(timeStamp.toString().toLong())) // 时间戳转换成时间
        return sd
    }


}