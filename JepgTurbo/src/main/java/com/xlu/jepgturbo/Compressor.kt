package com.xlu.jepgturbo

import android.content.Context
import android.media.ExifInterface
import android.util.Log
import com.xlu.jepgturbo.entitys.CompressParams
import com.xlu.jepgturbo.entitys.OutputFormat
import com.xlu.jepgturbo.itf.CompressListener
import com.xlu.jepgturbo.utils.BitmapUtil
import com.xlu.jepgturbo.utils.JpegNative
import com.xlu.jepgturbo.utils.getTempFile
import com.xlu.jepgturbo.utils.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @ClassName Compressor
 * @Description 压缩实现类
 * @Author LuHongCheng
 * @Date 2022/8/20 11:41
 */
class Compressor(private val params: CompressParams) {

    private val TAG = "Compressor"

    companion object{

        //压缩任务线程池
        val threadPool: ExecutorService by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ThreadPoolExecutor(1, 4, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
        }

    }

    /**
     * 异步压缩
     */
    fun <T> compress(
        listener: CompressListener<T> ?= null
    ){
        params.async = true

        if (params.inputType == null) throw Exception("intput type is null, or an unsupported type")
        if (params.outputType == null) throw Exception("output type is null, or an unsupported type")

        threadPool.execute {
            startCompress(listener)
        }
    }


    /**
     * 同步压缩
     */
    fun <T> compress() : T?{
        params.async = false

        if (params.inputType == null) throw Exception("intput type is null, or an unsupported type")
        if (params.outputType == null) throw Exception("output type is null, or an unsupported type")

        return startCompress<T>(null)
    }


    /**
     * 适用于协程和dsl语法的压缩方式
     */
    suspend fun <T> compress(
        onStart: () -> Unit = {},
        onSuccess: (result: T) -> Unit = {},
        onFailed: (error: String) -> Unit = {},
    ) = withContext(Dispatchers.IO){
        dslCompress(onStart, onSuccess, onFailed)
    }


    fun <T> compress(
        async:Boolean,
        onStart: () -> Unit = {},
        onSuccess: (result: T) -> Unit = {},
        onFailed: (error: String) -> Unit = {},
    ){
        params.async = async
        if (async){
            threadPool.execute {
                dslCompress(onStart, onSuccess, onFailed)
            }
        }else{
            dslCompress(onStart, onSuccess, onFailed)
        }
    }


    private fun <T> dslCompress(
        onStart: () -> Unit = {},
        onSuccess: (result: T) -> Unit = {},
        onFailed: (error: String) -> Unit = {},
    ){
        startCompress(object :CompressListener<T>{
            override fun onStart() {
                onStart.invoke()
            }

            override fun onSuccess(result: T) {
                onSuccess.invoke(result)
            }

            override fun onFailed(error: String) {
                onFailed.invoke(error)
            }
        })
    }


    private fun <T> startCompress(listener: CompressListener<T> ?= null) : T?{
        listener?.onStart()
        when(params.inputType){
            OutputFormat.File -> {
                if (params.inputFilePath.isNullOrEmpty()){
                    listener?.onFailed("inputFilePath is null, or an unsupported type")
                    return null
                }
                when (params.outputType) {
                    OutputFormat.File -> {
                        val result = compressFile()
                        if (result){
                            listener?.onSuccess(params.outputFilePath as T)
                        }else{
                            listener?.onFailed("file to file error")
                        }
                    }
                    OutputFormat.Bitmap -> {
                        params.outputBitmap = BitmapUtil.convertToBitmap(params.inputFilePath)
                        if (params.outputBitmap != null){
                            listener?.onSuccess(params.outputBitmap as T)
                        }else{
                            listener?.onFailed("file to bitmap error")
                        }
                    }
                    OutputFormat.Byte -> {
                        //先创建temp file压缩
                        /*params.outputFilePath = getTempFile(context).absolutePath
                        val result = JpegNative.compressFile(
                            srcFilePath = params.inputFilePath!!,
                            destFilePath = params.outputFilePath!!,
                            width = params.width,
                            height = params.height,
                            quality = params.quality)*/

                        //temp file to byte
                        params.outputByte = File(params.inputFilePath!!).toByteArray()
                        if (params.outputByte != null){
                            listener?.onSuccess(params.outputByte as T)
                        }else{
                            listener?.onFailed("file to byte error")
                        }
                    }
                    else -> {
                        listener?.onFailed("unknow action")
                    }
                }
            }
            OutputFormat.Byte -> {
                if (params.width < 1 || params.height < 1){
                    listener?.onFailed("when input is byte[], width and height can't be null")
                    return null
                }
                if (params.inputByte?.isNotEmpty() == false){
                    listener?.onFailed("input byte[] is null")
                    return null
                }
                when (params.outputType) {
                    OutputFormat.File -> {
                        if (params.outputFilePath.isNullOrEmpty()){
                            listener?.onFailed("outputFilePath is null")
                            return null
                        }
                        val result = JpegNative.compressByte2File(
                            byte = params.inputByte!!,
                            width = params.width,
                            height = params.height,
                            quality = params.quality,
                            outputFilePath = params.outputFilePath!!
                        )
                        if (result){
                            listener?.onSuccess(params.outputFilePath as T)
                        }else{
                            listener?.onFailed("byte to file error")
                        }
                    }
                    OutputFormat.Bitmap -> {
                        val outputByte = JpegNative.compressByte2Byte(
                            byte = params.inputByte!!,
                            width = params.width,
                            height = params.height,
                            quality = params.quality
                        )
                        params.outputBitmap = BitmapUtil.deconvertByte(outputByte)
                        if (params.outputBitmap == null){
                            listener?.onFailed("byte to bitmap error")
                        }else{
                            listener?.onSuccess(params.outputBitmap as T)
                        }
                    }
                    OutputFormat.Byte -> {
                        params.outputByte = JpegNative.compressByte2Byte(
                            byte = params.inputByte!!,
                            width = params.width,
                            height = params.height,
                            quality = params.quality
                        )
                        if (params.outputByte == null || params.outputByte!!.isEmpty()){
                            listener?.onFailed("byte to byte error")
                        }else{
                            listener?.onSuccess(params.outputByte as T)
                        }
                    }
                    else -> listener?.onFailed("unknow action")
                }
            }
            OutputFormat.Bitmap -> {
                if (params.inputBitmap == null){
                    listener?.onFailed("input bitmap is null")
                    return null
                }
                if (params.width == -1) params.width = params.inputBitmap!!.width
                if (params.height == -1) params.height = params.inputBitmap!!.height

                when (params.outputType) {
                    OutputFormat.File -> {
                        if (params.outputFilePath.isNullOrEmpty()){
                            listener?.onFailed("outputFilePath is null")
                            return null
                        }
                        val result = JpegNative.compressBitmap(
                            bitmap = params.inputBitmap!!,
                            width = params.width,
                            height = params.height,
                            quality = params.quality,
                            outputFilePath = params.outputFilePath!!
                        )
                        if (result){
                            listener?.onSuccess(params.outputFilePath as T)
                        }else{
                            listener?.onFailed("bitmap to file error")
                        }
                    }
                    OutputFormat.Bitmap -> {
                        //Bitmap压缩无意义，这里只做裁剪
                        listener?.onFailed("bitmap to bitmap is unuseful")
                    }
                    OutputFormat.Byte -> {
                        params.outputByte = JpegNative.compressBitmap2Byte(
                            bitmap = params.inputBitmap!!,
                            width = params.width,
                            height = params.height,
                            quality = params.quality
                        )
                        if (params.outputByte?.isNotEmpty() == true){
                            listener?.onSuccess(params.outputByte as T)
                        }else{
                            listener?.onFailed("bitmap to byte error")
                        }
                    }
                    else -> listener?.onFailed("unknow action")
                }
            }
            else -> {

            }
        }

        //返回同步压缩结果
        return when(params.outputType){
            OutputFormat.Bitmap -> {
                params.outputBitmap as T
            }
            OutputFormat.File -> {
                params.outputFilePath as T
            }
            OutputFormat.Byte -> {
                params.outputByte as T
            }
            else -> null
        }
    }


    /**
     * 文件压缩
     * @return
     */
    private fun compressFile() : Boolean{
        if (params.inputFilePath.isNullOrEmpty()) return false

        //拷贝原文件exif信息
        val sourceExifInterface = ExifInterface(params.inputFilePath!!)

        //如果输出地址为空，覆盖源文件
        if (params.outputFilePath.isNullOrEmpty()){
            params.outputFilePath = params.inputFilePath
            Log.e(TAG,"outputFilePath is null, compression will overwrite the input file")
        }
        val result = JpegNative.compressFile(
            srcFilePath = params.inputFilePath!!,
            destFilePath = params.outputFilePath!!,
            width = params.width,
            height = params.height,
            quality = params.quality)

        //复制原文件exif信息到输出文件,这里消耗2-4s时间
        if (result && params.reserveExifInfo){
            val target = ExifInterface(params.outputFilePath!!)
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


}