package com.xlu.jepgturbo

import android.graphics.Bitmap
import java.io.File
import java.nio.ByteBuffer

/**
 * @ClassName JepgTurbo
 * @Description TODO
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/7/9 17:27
 */
object JpegTurbo {


    init {
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
        width: Int = 0,
        height: Int = 0,
        quality: Int = 60,
        outputFilePath: String
    ):Boolean

    /**
     * TODO 输入：ByteBuffer   输出：byte[]
     *  暂未测试
     * @param byteBuffer
     * @param width
     * @param height
     * @param quality
     * @return
     */
    private external fun compressByteBuffer(
        byteBuffer: ByteBuffer,
        width: Int,
        height: Int,
        quality: Int = 60
    ):ByteArray?


    /**
     * TODO 输入：byte[]   输出：byte[]
     *  暂未测试
     * @param byte
     * @param width
     * @param height
     * @param quality
     * @return
     */
    external fun compressByteArray(
        byte: ByteArray,
        width: Int,
        height: Int,
        quality: Int = 60
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
    external fun compressByte2Jpeg(
            byte: ByteArray,
            width: Int,
            height: Int,
            quality: Int = 60,
            outputFilePath: String
    ):Boolean



    //压缩文件
    private var file: File ?= null
    private var bitmap:Bitmap ?= null

    //输出文件路径
    private var outputPath:String ?= null
        set(value) {
            field = value
            outputFile = File(value)
        }

    //输出文件
    private var outputFile:File ?= null
        set(value) {
            field = value
            outputPath = value?.absolutePath
        }

    //默认压缩质量，范围0-100
    private var quality:Int = 60

    private var listener:CompressListener ?= null

    //压缩格式
    private var compressFormat = CompressFormat.JPEG

    fun setSource(file: File):JpegTurbo = apply{
        this.file = file
    }

    fun setSource(bitmap: Bitmap):JpegTurbo = apply{
        this.bitmap = bitmap
    }

    fun setQuality(quality: Int):JpegTurbo = apply{
        this.quality = quality
    }

    fun setOutput(outputFile: File):JpegTurbo = apply {
        this.outputFile = outputFile
    }

    fun setOutput(outputPath: String):JpegTurbo = apply {
        this.outputPath = outputPath
    }

    fun compress(listener: CompressListener? = null){
        this.listener = listener
        startCompress()
    }

    private fun startCompress() {

    }


}