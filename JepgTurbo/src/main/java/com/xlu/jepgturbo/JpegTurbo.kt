package com.xlu.jepgturbo

import android.graphics.Bitmap
import com.xlu.jepgturbo.utils.BitmapUtil
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

    //是否设置了参数
    private var setParams:Boolean = false

    //输入源类型
    private var inputType :Formats ?= null
    //输出类型
    private var outputType :Formats ?= null

    private var width:Int = 0
    private var height:Int = 0

    //压缩质量：0-100
    private var quality:Int = 60

    //输入文件路径
    private var inputFilePath :String ?= null
    //输出文件路径
    private var outputFilePath :String ?= null

    //输入bitmap
    private var inputBitmap :Bitmap ?= null
    //输出bitmap
    private var outputBitmap : Bitmap ?= null

    //输入byte
    private var inputByte:ByteArray ?= null
    //输出byte
    private var outputByte:ByteArray ?= null


    /**
     * TODO 设置压缩参数
     * @param input 支持输入类型：Bitmap, ByteArray, File, File路径
     * @param output 支持输出类型：Bitmap, ByteArray, File路径
     * @param width
     * @param height
     * @param quality 压缩质量：0-100
     * @return
     */
    fun setParams(
        input: Any? = null,
        output: Any? = null,
        width: Int = 0,
        height: Int = 0,
        quality: Int = 60
    ): JpegTurbo = apply {
        setParams = true
        //设置输入类型
        when(input){
            is String -> {
                inputType = Formats.File
                inputFilePath = input
            }
            is File -> {
                inputType = Formats.File
                inputFilePath = input.absolutePath
            }
            is Bitmap -> {
                inputType = Formats.Bitmap
                inputBitmap = input
            }
            is ByteArray -> {
                inputType = Formats.Byte
                inputByte = input
                if (width == 0 || height == 0) {
                    throw Exception("when input is byte[], width and height can't be null")
                }
            }
            else -> {
                throw Exception("input is null, or an unsupported type")
            }
        }

        //设置输出类型
        when(output){
            is String -> {
                outputType = Formats.File
                outputFilePath = output
            }
            is File -> {
                inputType = Formats.File
                outputFilePath = output.absolutePath
            }
            is Bitmap -> {
                outputType = Formats.Bitmap
                outputBitmap = output
            }
            is ByteArray -> {
                outputType = Formats.Byte
                outputByte = output
            }
            else -> {
                throw Exception("output is null, or an unsupported type")
            }
        }

        this.width = width
        this.height = height
        this.quality = quality

    }

    @JvmName("compress1")
    fun compress(listener: CompressListener<Any> ?= null) : Any?{
        return this.compress<Any>(listener)
    }

    fun <T> compress(listener: CompressListener<T> ?= null) : Any?{
        when(inputType){
            Formats.File -> {
                when (outputType) {
                    Formats.File -> {

                    }
                    Formats.Bitmap -> {

                    }
                    Formats.Byte -> {

                    }
                }
            }
            Formats.Byte -> {
                when (outputType) {
                    Formats.File -> {

                    }
                    Formats.Bitmap -> {

                    }
                    Formats.Byte -> {

                    }
                }
            }
            Formats.Bitmap -> {
                if (inputBitmap == null) throw Exception("input bitmap is null")
                when (outputType) {
                    Formats.File -> {
                        if (outputFilePath.isNullOrEmpty()) throw Exception("output file path is null")
                        val result = compressBitmap(
                            inputBitmap!!,
                            width,
                            height,
                            quality,
                            outputFilePath!!
                        )
                        listener?.onCompleted(result, outputFilePath as T)
                    }
                    Formats.Bitmap -> {

                    }
                    Formats.Byte -> {
                        if (width == 0 || height == 0) throw Exception("when input is byte[], width and height can't be null")
                        val byte: ByteArray? = BitmapUtil.convertToByteArray(inputBitmap)
                        byte?.let {
                            compressByteArray(byte, width, height, quality)
                        }


                    }
                }
            }
        }

        clear()
        return null
    }


    /**
     * TODO 清除参数
     */
    private fun clear() {
        setParams = false

        inputType = null
        outputType = null

        width = 0
        height = 0
        quality = 60

        inputByte = null
        inputBitmap = null
        inputFilePath = null

        outputBitmap = null
        outputFilePath = null
        outputByte = null
    }

}