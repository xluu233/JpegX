package com.xlu.jepgturbo

import android.graphics.Bitmap
import com.xlu.jepgturbo.utils.BitmapUtil
import java.io.File
import kotlin.concurrent.thread

/**
 * @ClassName JepgTurbo
 * @Description TODO
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/7/9 17:27
 */
object JpegTurbo {

    const val TAG = "JpegTurbo"

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
     * TODO 输入：byte[]   输出：byte[]
     * @param byte
     * @param width
     * @param height
     * @param quality
     * @return
     */
    external fun compressByte(
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


    // byte[] 2 bitmap
/*    external fun compressByte2Bitmap(
        byte: ByteArray,
        width: Int,
        height: Int,
        quality: Int = 60
    ):Bitmap*/

    external fun compressFile(
        filePath:String,
        outputFilePath: String,
        width: Int,
        height: Int,
        quality: Int = 60,
    )

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
     *
     * @param input 支持输入类型：Bitmap, ByteArray, File, File路径(String)
     * @param output 支持输出类型：Bitmap, ByteArray, File路径(String)
     * @param outputType 输入、输出类型会根据输入、输出文件自动判断，但是如果没有设置输出，必须手动设置输出类型
     * @param width
     * @param height
     * @param quality 压缩质量：0-100
     * @return
     */
    fun setParams(
        input: Any,
        outputType: Formats,
        width: Int = 0,
        height: Int = 0,
        quality: Int = 60
    ): JpegTurbo = apply {
        setParams = true
        //设置输入类型
        when(input){
            is String -> {
                this.inputType = Formats.File
                inputFilePath = input
            }
            is File -> {
                this.inputType = Formats.File
                inputFilePath = input.absolutePath
            }
            is Bitmap -> {
                this.inputType = Formats.Bitmap
                inputBitmap = input
            }
            is ByteArray -> {
                this.inputType = Formats.Byte
                inputByte = input
            }
            else -> {
                throw Exception("input is null, or an unsupported type")
            }
        }

        //设置输出类型
/*        when(output){
            is String -> {
                this.outputType = Formats.File
                outputFilePath = output
            }
            is File -> {
                this.outputType = Formats.File
                outputFilePath = output.absolutePath
            }
            is Bitmap -> {
                this.outputType = Formats.Bitmap
                outputBitmap = output
            }
            is ByteArray -> {
                this.outputType = Formats.Byte
                outputByte = output
            }
            else -> {
                throw Exception("output is null, or an unsupported type")
            }
        }*/

        this.width = width
        this.height = height
        this.quality = quality
        this.outputType = outputType
    }

    @JvmName("compress1")
    fun compress(listener: CompressListener<Any> ?= null){
        this.compress<Any>(listener)
    }

    inline fun <reified T> getType(value: T) : Any{
        println("$value 的类型是 ${T::class.java}")
        return T::class.java
    }

    @Synchronized
    fun <T> compress(listener: CompressListener<T> ?= null) = thread{
        if (!setParams) throw Exception("you haven't set the parameters")
        if (inputType == null) throw Exception("intput type is null, or an unsupported type")
        if (outputType == null) throw Exception("output type is null, or an unsupported type")

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
                if (width == 0 || height == 0) throw Exception("when input is byte[], width and height can't be null")
                when (outputType) {
                    Formats.File -> {
                        if (outputFilePath.isNullOrEmpty()) throw Exception("output file path is null")
                        listener?.onStart()
                        val result = compressByte2Jpeg(
                            inputByte!!,
                            width,
                            height,
                            quality,
                            outputFilePath!!
                        )
                        listener?.onCompleted(result, outputFilePath as T)
                    }
                    Formats.Bitmap -> {
                        inputByte?.let {
                            listener?.onStart()
                            val outputByte = compressByte(it, width, height, quality)
                            outputBitmap = BitmapUtil.deconvertByte(outputByte)
                        }
                        listener?.onCompleted(outputBitmap != null, outputBitmap as T)
                    }
                    Formats.Byte -> {
                        inputByte?.let {
                            listener?.onStart()
                            outputByte = compressByte(it, width, height, quality)
                        }
                        listener?.onCompleted(outputByte != null, outputByte as T)
                    }
                }
            }
            Formats.Bitmap -> {
                if (inputBitmap == null) throw Exception("input bitmap is null")
                when (outputType) {
                    Formats.File -> {
                        if (outputFilePath.isNullOrEmpty()) throw Exception("output file path is null")
                        listener?.onStart()
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
                        //TODO 待完善
                        val byte: ByteArray? = BitmapUtil.convertToByteArray(inputBitmap)
                        byte?.let {
                            listener?.onStart()
                            if (width==0) width = inputBitmap!!.width
                            if (height==0) height = inputBitmap!!.height
                            val outputByte = compressByte(it, width, height, quality)
                            outputBitmap = BitmapUtil.deconvertByte(outputByte)
                        }
                        listener?.onCompleted(outputBitmap != null, outputBitmap as T)
                    }
                    Formats.Byte -> {
                        val byte: ByteArray? = BitmapUtil.convertToByteArray(inputBitmap)
                        byte?.let {
                            listener?.onStart()
                            if (width==0) width = inputBitmap!!.width
                            if (height==0) height = inputBitmap!!.height
                            outputByte = compressByte(it, width, height, quality)
                        }
                        listener?.onCompleted(outputByte != null, outputByte as T)
                    }
                }
            }
        }

        clear()
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