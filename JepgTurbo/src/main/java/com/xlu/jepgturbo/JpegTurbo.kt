package com.xlu.jepgturbo

import android.graphics.Bitmap
import android.media.ExifInterface
import android.util.Log
import com.xlu.jepgturbo.utils.BitmapUtil
import com.xlu.jepgturbo.utils.FileUtil
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * @ClassName JepgTurbo
 * @Description TODO
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/7/9 17:27
 */
object JpegTurbo {

    private const val TAG = "JpegTurbo"

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


    /**
     * TODO 输入:Jpeg文件，输出：覆盖原Jpeg文件
     * 相机照片可能会与原始角度不一致，解决方案：复制原文件的Exif信息
     * @param filePath 文件路径
     * @param width
     * @param height
     * @param quality
     * @return
     */
    private external fun compressFile(
        filePath:String,
        width: Int=0,
        height: Int=0,
        quality: Int = 60,
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
        width: Int=0,
        height: Int=0,
        quality: Int = 60,
    ):Boolean


    /**
     * TODO 输入：Jpeg文件，输出：ByteArray
     *  不涉及压缩，测试：byte 2 bitmap失败
     * @param filePath
     * @return
     */
    external fun readFile2Byte(
        filePath:String
    ):ByteArray


    //是否完成了压缩
    @Volatile
    private var completed:Boolean = false
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

    //是否开启异步压缩
    private var async:Boolean = true

    //开始多线程分块压缩
    private var multiThread:Boolean = false

    //输出图像最大数据: 1024Kb
    private var maxSize:Int = 1024

    //压缩任务线程池
    private val threadPool: ExecutorService by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        ThreadPoolExecutor(1, 4, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
    }


    /**
     * TODO 设置压缩参数
     *
     * @param input 支持输入类型：Bitmap, ByteArray, File, File路径(String)
     * @param output 支持输出类型：Bitmap, ByteArray, File路径(String)
     * @param outputType 输入、输出类型会根据输入、输出文件自动判断，但是如果没有设置输出，必须手动设置输出类型
     * @param width 宽度
     * @param height 高度
     * @param maxSize 输出图像最大大小，单位Kb
     * @param quality 压缩质量：0-100
     * @param async 异步压缩，默认开启
     * @return
     */
    fun setParams(
            input: Any,
            output: Any ?= null,
            outputType: Formats ?= null,
            width: Int = 0,
            height: Int = 0,
            maxSize:Int = 1024,
            quality: Int = 60,
            async:Boolean = true
    ): JpegTurbo = apply {
        setParams = true
        completed = false
        //设置输入类型
        when(input){
            is String -> {
                this.inputType = Formats.File
                inputFilePath = input
                checkFile(input)
            }
            is File -> {
                this.inputType = Formats.File
                inputFilePath = input.absolutePath
                checkFile(input.name)
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
        output?.let {
            when(output){
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
            }
        }
        outputType?.let {
            this.outputType = it
        }

        this.width = width
        this.height = height
        this.quality = quality
        this.async = async
        //this.multiThread = multiThread
        this.maxSize = maxSize
    }

    private fun checkFile(name: String) {
        if (name.contains("jpg") || name.contains(".jpeg")){
            Log.d(TAG,"fileName is:${name}")
        }else{
            throw Exception("file is not support")
        }
    }

    @JvmName("compress1")
    fun compress() : Any?{
        return this.compress<Any>()
    }

    inline fun <reified T> getType(value: T) {
        println("$value 的类型是 ${T::class.java}")
    }

    fun <T> compress(listener: CompressListener<T> ?= null) : T?{
        if (!setParams) throw Exception("you haven't set the parameters")
        if (inputType == null) throw Exception("intput type is null, or an unsupported type")
        if (outputType == null) throw Exception("output type is null, or an unsupported type")

        if (async){
            threadPool.execute {
                startCompress(listener)
                clear()
            }
            return null
        }else{
            val result =  startCompress(listener)
            clear()
            return result
        }
    }

    private fun <T> startCompress(listener: CompressListener<T> ?= null) : T?{
        Log.d(TAG,"startCompress")
        Log.d(TAG,"inputType:${inputType},outputType:${outputType}")
        when(inputType){
            Formats.File -> {
                if (inputFilePath.isNullOrEmpty()) throw Exception("inputFilePath is null, or an unsupported type")
                if (outputFilePath.isNullOrEmpty()){
                    //输出路径为空，覆盖原文件
                    Log.e(TAG,"outputFilePath is null, compression will overwrite the input file")
                }
                listener?.onStart()
                when (outputType) {
                    Formats.File -> {
                        val result = compressExifFile()
                        listener?.onCompleted(result, outputFilePath as T)
                    }
                    Formats.Bitmap -> {
                        outputBitmap = BitmapUtil.convertToBitmap(inputFilePath)
                        listener?.onCompleted(outputBitmap!=null, outputBitmap as T)
                    }
                    Formats.Byte -> {
                        outputByte = FileUtil.file2Byte(File(inputFilePath))
                        listener?.onCompleted(outputByte!=null, outputByte as T)
                    }
                }
            }
            Formats.Byte -> {
                if (width == 0 || height == 0) throw Exception("when input is byte[], width and height can't be null")
                listener?.onStart()
                when (outputType) {
                    Formats.File -> {
                        if (outputFilePath.isNullOrEmpty()) throw Exception("output file path is null")
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
                            val outputByte = compressByte(it, width, height, quality)
                            outputBitmap = BitmapUtil.deconvertByte(outputByte)
                        }
                        listener?.onCompleted(outputBitmap != null, outputBitmap as T)
                    }
                    Formats.Byte -> {
                        inputByte?.let {
                            outputByte = compressByte(it, width, height, quality)
                        }
                        listener?.onCompleted(outputByte != null, outputByte as T)
                    }
                }
            }
            Formats.Bitmap -> {
                if (inputBitmap == null) throw Exception("input bitmap is null")
                listener?.onStart()
                if (width==0) width = inputBitmap!!.width
                if (height==0) height = inputBitmap!!.height
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
                        //Bitmap压缩无意义，这里只做裁剪
                        outputBitmap = BitmapUtil.zoomImg(inputBitmap, width, height)
                        listener?.onCompleted(outputBitmap != null, outputBitmap as T)
                    }
                    Formats.Byte -> {
                        var time = System.currentTimeMillis()
                        val byte: ByteArray? = BitmapUtil.convertToByteArray(inputBitmap)
                        Log.d(TAG,"delta_time:${System.currentTimeMillis()-time}")
                        time = System.currentTimeMillis()
                        byte?.let {
                            outputByte = compressByte(it, width, height, quality)
                        }
                        Log.d(TAG,"delta_time2:${System.currentTimeMillis()-time}")
                        listener?.onCompleted(outputByte != null, outputByte as T)
                    }
                }
            }
        }
        completed = true
        when(outputType){
            Formats.Bitmap -> {
                return outputBitmap as T
            }
            Formats.File -> {
                return outputFilePath as T
            }
            Formats.Byte -> {
                return outputByte as T
            }
            else -> return null
        }
    }

    /**
     * TODO 文件压缩
     * @return
     */
    private fun compressExifFile():Boolean{
        //拷贝原文件exif信息
        val sourceExifInterface = ExifInterface(inputFilePath!!)

        //开始压缩File
        if (outputFilePath.isNullOrEmpty()){
            outputFilePath = inputFilePath
        }
        val result: Boolean = compressFile2File(inputFilePath!!, outputFilePath!!,quality = quality)

        //复制原文件exif信息到输出文件
        if (result){
            val target = ExifInterface(outputFilePath!!)
            sourceExifInterface.javaClass.declaredFields.forEach { member ->//获取ExifInterface类的属性并遍历
                member.isAccessible = true
                val tag = member.get(sourceExifInterface)//获取原图EXIF实例种的成员变量
                if (member.name.startsWith("TAG_") && tag is String) {//判断变量是否以TAG开头，并且是String类型
                    target.setAttribute(tag, sourceExifInterface.getAttribute(tag))//设置压缩图的EXIF信息
                    target.saveAttributes()//保存属性更改
                }
            }
        }
        return result
    }

    /**
     * TODO 清除参数
     */
    @Synchronized
    private fun clear() {
        setParams = false
        completed = false

        async = true
        multiThread = false

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