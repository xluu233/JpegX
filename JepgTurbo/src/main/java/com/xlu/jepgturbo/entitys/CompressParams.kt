package com.xlu.jepgturbo.entitys

import android.graphics.Bitmap

/**
 * @ClassName CompressParams
 * @Description 压缩参数
 * @Author LuHongCheng
 * @Date 2022/8/20 12:18
 */
data class CompressParams(

    //是否完成
    @Volatile
    var completed: Boolean = false,

    //输入源类型
    var inputType: OutputFormat? = null,
    //输出类型
    var outputType: OutputFormat? = null,

    var width: Int = -1,
    var height: Int = -1,

    //压缩质量：0-100
    var quality: Int = 60,

    //输入文件路径
    var inputFilePath: String? = null,
    //输出文件路径
    var outputFilePath: String? = null,

    //输入bitmap
    var inputBitmap: Bitmap? = null,
    //输出bitmap
    var outputBitmap: Bitmap? = null,

    //输入byte
    var inputByte: ByteArray? = null,
    //输出byte
    var outputByte: ByteArray? = null,

    //是否开启异步压缩
    var async: Boolean = false,
    //开始多线程分块压缩
    var multiPart: Boolean = false,

    //输出图像最大数据: 1024Kb
    var maxSize: Int = 1024,

    //文件缩放
    var scale:Float = 1.0f

) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompressParams

        if (completed != other.completed) return false
        if (inputType != other.inputType) return false
        if (outputType != other.outputType) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (quality != other.quality) return false
        if (inputFilePath != other.inputFilePath) return false
        if (outputFilePath != other.outputFilePath) return false
        if (inputBitmap != other.inputBitmap) return false
        if (outputBitmap != other.outputBitmap) return false
        if (inputByte != null) {
            if (other.inputByte == null) return false
            if (!inputByte.contentEquals(other.inputByte)) return false
        } else if (other.inputByte != null) return false
        if (outputByte != null) {
            if (other.outputByte == null) return false
            if (!outputByte.contentEquals(other.outputByte)) return false
        } else if (other.outputByte != null) return false
        if (async != other.async) return false
        if (multiPart != other.multiPart) return false
        if (maxSize != other.maxSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = completed.hashCode()
        result = 31 * result + (inputType?.hashCode() ?: 0)
        result = 31 * result + (outputType?.hashCode() ?: 0)
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + quality
        result = 31 * result + (inputFilePath?.hashCode() ?: 0)
        result = 31 * result + (outputFilePath?.hashCode() ?: 0)
        result = 31 * result + (inputBitmap?.hashCode() ?: 0)
        result = 31 * result + (outputBitmap?.hashCode() ?: 0)
        result = 31 * result + (inputByte?.contentHashCode() ?: 0)
        result = 31 * result + (outputByte?.contentHashCode() ?: 0)
        result = 31 * result + async.hashCode()
        result = 31 * result + multiPart.hashCode()
        result = 31 * result + maxSize
        return result
    }

}
