package com.xlu.jepgturbo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import java.io.*
import java.nio.ByteBuffer


/**
 * @ClassName BitmapUtil
 * @Description TODO
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/7/13 15:29
 */
object BitmapUtil {

    /**
     * TODO file转bitmap
     * BitmapFactory会导致图像旋转
     * 先记录旋转角度，在转为bitmap之后再旋转回来
     */
    fun convertToBitmap(file: File?): Bitmap? {
        if (file==null) return null
        val rotate = getRotateDegree(file.absolutePath)
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        return rotateBitmap(bitmap, rotate)
    }

    fun convertToBitmap(path: String?): Bitmap? {
        val rotate = getRotateDegree(path)
        val bitmap = BitmapFactory.decodeFile(path)
        return rotateBitmap(bitmap, rotate)
    }

    fun convertToBitmap(uri: Uri?, context: Context): Bitmap? {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        return bitmap
    }

    fun assert2Bitmap(fileName: String, context: Context) : Bitmap?{
        var ins: InputStream? = null
        try {
            ins = context.resources.assets.open(fileName)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return BitmapFactory.decodeStream(ins)
    }

    /**
     * TODO 获取图片的旋转角度
     * 只能通过原始文件获取，如果已经进行过bitmap操作无法获取。
     */
    private fun getRotateDegree(path: String?): Float {
        var result = 0f
        if (path.isNullOrEmpty()) return result

        try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> result = 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> result = 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> result = 270f
            }
        } catch (ignore: IOException) {
            return result
        }
        return result
    }


    /**
     * TODO 处理图片旋转
     */
    private fun rotateBitmap(bitmap: Bitmap?, rotate: Float): Bitmap? {
        if (bitmap == null) return null
        if (rotate==0f) return bitmap

        val w = bitmap.width
        val h = bitmap.height

        // Setting post rotate to 90
        val mtx = Matrix()
        mtx.postRotate(rotate)

        val outBit = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true)
        bitmap.recycle()
        return outBit
    }


    /**
     * TODO Bitmap压缩
     * @param quality 0-100
     */
    fun Bitmap.compressQuality(quality: Int = 90):Bitmap = apply{
        //val size = this.byteCount.toString() + "byte"
        val bos = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, quality, bos)
        val bytes = bos.toByteArray()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    fun convertToByteArray(bitmap: Bitmap?):ByteArray? {
        if (bitmap==null) return null

        val bytes: Int = bitmap.byteCount
        val buf: ByteBuffer = ByteBuffer.allocate(bytes)
        bitmap.copyPixelsToBuffer(buf)

        return buf.array()
    }

    fun convertToByteBuffer(bitmap: Bitmap?):ByteBuffer? {
        if (bitmap==null) return null

        val bytes: Int = bitmap.byteCount
        val buf: ByteBuffer = ByteBuffer.allocate(bytes)
        bitmap.copyPixelsToBuffer(buf)
        return buf
    }

    fun deconvertByte(byteArray: ByteArray?):Bitmap?{
        if (byteArray==null) return null
        var bitmap : Bitmap ?= null
        bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size);
        return bitmap
    }

}