package com.xlu.jepgturbo

import android.media.ExifInterface

/**
 * 复制原图Exif信息到压缩后图片的Exif信息
 * @param sourcePath 原图路径
 * @param targetPath 目标图片路径
 */
private fun copyExif(sourcePath: String, targetPath: String) {
    try {
        val source = ExifInterface(sourcePath)
        val target = ExifInterface(targetPath)
        source.javaClass.declaredFields.forEach { member ->//获取ExifInterface类的属性并遍历
            member.isAccessible = true
            val tag = member.get(source)//获取原图EXIF实例种的成员变量
            if (member.name.startsWith("TAG_") && tag is String) {//判断变量是否以TAG开头，并且是String类型
                target.setAttribute(tag, source.getAttribute(tag))//设置压缩图的EXIF信息
                target.saveAttributes()//保存属性更改
            }
        }
    } catch (e: Exception) {
    }
}
