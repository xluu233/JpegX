package com.xlu.jepgturbo.utils

import android.graphics.Bitmap

/**
 * @ClassName JpegNative
 * @Description jni方法
 * @Author LuHongCheng
 * @Date 2022/8/20 11:45
 */
object JpegNative {


    init {
        System.loadLibrary("jepg_compress")
    }

    fun init(){
        System.loadLibrary("jepg_compress")
    }

    /**
     * TODO 输入：Bitmap   输出：File
     * @param bitmap 输入源
     * @param quality 压缩质量
     * @param outputFilePath 输出文件路径
     * @return
     */
    external fun compressBitmap(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        quality: Int,
        outputFilePath: String,
    ):Boolean


    /**
     * TODO 输入：Bitmap  输出：Byte
     */
    external fun compressBitmap2Byte(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        quality: Int,
    ):ByteArray?


    /**
     * TODO 输入：byte[]   输出：byte[]
     * @param byte
     * @param width
     * @param height
     * @param quality
     * @return
     */
    external fun compressByte2Byte(
        byte: ByteArray,
        width: Int,
        height: Int,
        quality: Int,
    ):ByteArray?


    /**
     * TODO 输入：byte[]   输出：File
     * @param byte
     * @param width
     * @param height
     * @param quality
     * @param outputFilePath
     * @return
     */
    external fun compressByte2File(
        byte: ByteArray,
        width: Int,
        height: Int,
        quality: Int,
        outputFilePath: String,
    ):Boolean


    /**
     * TODO 输入:Jpeg文件，输出：覆盖原Jpeg文件
     * 相机照片可能会与原始角度不一致，解决方案：复制原文件的Exif信息
     * @param filePath 文件路径
     * @param width
     * @param height
     * @param quality
     * @return
     */
    external fun compressFile(
        filePath:String,
        width: Int,
        height: Int,
        quality: Int,
    ):Boolean


    /**
     * TODO 输入:Jpeg文件，输出：Jpeg文件
     * 相机照片可能会与原始角度不一致，解决方案：复制原文件的Exif信息
     * @param filePath 输入文件路径
     * @param outputFilePath 输出文件路径
     * @param width
     * @param height
     * @param quality
     * @return
     */
    external fun compressFile2File(
        filePath:String,
        outputFilePath: String,
        width: Int,
        height: Int,
        quality: Int,
    ):Boolean


    /**
     * TODO 输入：Jpeg文件，输出：ByteArray
     *  不涉及压缩，测试：byte 2 bitmap失败
     * @param filePath
     * @return
     */
    external fun readFile2Byte(
        filePath: String,
    ):ByteArray



}