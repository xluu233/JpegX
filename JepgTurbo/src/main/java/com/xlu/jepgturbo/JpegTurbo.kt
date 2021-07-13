package com.xlu.jepgturbo

import android.graphics.Bitmap

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

    external fun compressBitmap2File(bitmap: Bitmap, filePath: String): Boolean

    external fun compressBitmap(bitmap: Bitmap, quality: Int = 60, filePath: String)
}