package com.xlu.jepgturbo

import android.content.Context
import android.graphics.Bitmap
import com.xlu.jepgturbo.entitys.CompressParams
import com.xlu.jepgturbo.entitys.OutputFormat
import com.xlu.jepgturbo.utils.JpegNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @ClassName JpegX
 * @Description 调用api
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/7/9 17:27
 */
object JpegX {

    private const val TAG = "JpegTurbo"


    @Volatile
    private var isInitialized = false


    /**
     * 初始化jni
     */
    fun init(context: Context) {
        if (!isInitialized) {
            JpegNative.init()
        }
    }


    suspend fun params2(
        input: Any,
        output: Any? = null,
        outputType: OutputFormat? = null,
        width: Int = -1,
        height: Int = -1,
        scale: Float = 1.0f,
        quality: Int = 60,
        maxSize: Int = 1024,
    ): Compressor = withContext(Dispatchers.IO) {
        return@withContext initParams(input,
            output,
            outputType,
            width,
            height,
            scale,
            quality,
            maxSize)
    }

    fun params(
        input: Any,
        output: Any? = null,
        outputType: OutputFormat? = null,
        width: Int = -1,
        height: Int = -1,
        scale: Float = 1.0f,
        quality: Int = 60,
        maxSize: Int = 1024,
    ): Compressor{
        return initParams(input,
            output,
            outputType,
            width,
            height,
            scale,
            quality,
            maxSize)
    }


    private fun initParams(
        input: Any,
        output: Any? = null,
        outputType: OutputFormat? = null,
        width: Int = -1,
        height: Int = -1,
        scale: Float = 1.0f,
        quality: Int = 60,
        maxSize: Int = 1024,
    ): Compressor {
        val params = CompressParams()

        //设置输入类型
        when (input) {
            is String -> {
                params.inputType = OutputFormat.File
                params.inputFilePath = input
                //checkFile(input)
            }
            is File -> {
                params.inputType = OutputFormat.File
                params.inputFilePath = input.absolutePath
                //checkFile(input.name)
            }
            is Bitmap -> {
                params.inputType = OutputFormat.Bitmap
                params.inputBitmap = input
            }
            is ByteArray -> {
                params.inputType = OutputFormat.Byte
                params.inputByte = input
            }
            else -> {
                throw Exception("input is null, or an unsupported type")
            }
        }

        //设置输出类型
        outputType?.let { params.outputType = it }
        output?.let {
            when (output) {
                is String -> {
                    params.outputType = OutputFormat.File
                    params.outputFilePath = output
                }
                is File -> {
                    params.outputType = OutputFormat.File
                    params.outputFilePath = output.absolutePath
                }
                is Bitmap -> {
                    params.outputType = OutputFormat.Bitmap
                    params.outputBitmap = output
                }
                is ByteArray -> {
                    params.outputType = OutputFormat.Byte
                    params.outputByte = output
                }
            }
        }
        params.width = width
        params.height = height
        params.scale = scale
        params.maxSize = maxSize
        params.quality = quality

        return Compressor(params)
    }

}