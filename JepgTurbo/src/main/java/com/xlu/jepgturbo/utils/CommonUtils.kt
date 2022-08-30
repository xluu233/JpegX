package com.xlu.jepgturbo.utils

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.*
import java.lang.ref.SoftReference


/**
 * 复制原图Exif信息到压缩后图片的Exif信息
 * @param sourcePath 原图路径
 * @param targetPath 目标图片路径
 */
fun copyExif(sourcePath: String, targetPath: String) {
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


/**
 * 文件转byteArray
 */
fun File.toByteArray() : ByteArray?{
    if (!this.exists()){
        return null
    }
    val bytesArray = ByteArray(this.length().toInt())
    val fis = FileInputStream(this)
    fis.read(bytesArray)
    fis.close()
    return bytesArray
}

fun ByteArray.toBitmap() : Bitmap?{
    val options = BitmapFactory.Options()
    options.inSampleSize = 1
    val input = ByteArrayInputStream(this)
    val softRef = SoftReference(BitmapFactory.decodeStream(input, null, options)) //软引用防止OOM
    val bitmap: Bitmap?= softRef.get()
    input.close()
    return bitmap
}
