package com.xlu.compress.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import java.io.*
import java.lang.ref.SoftReference

const val testFileName = "test.jpg"

fun getAssertBitmap(context:Context) : Bitmap{
    var `in`: InputStream? = null
    try {
        `in` = context.resources.assets.open(testFileName)
    }catch (  e:java.io.IOException){
        e.printStackTrace()
    }
    return BitmapFactory.decodeStream(`in`)
}



fun getAssertFile(context: Context):File{
    val outFile = File(getExternalPicturesPath(),testFileName)
    if (!outFile.exists()) {
        copyFileFromAssets(
            context = context,
            assetName = testFileName,
            savePath = getExternalPicturesPath(),
            saveName = testFileName
        )
    }
    return outFile
}



/**
 * 测试文件目录
 */
fun getExternalPicturesPath(subDir: String = "JpegX"): String {
    val path = StringBuilder(Environment.getExternalStorageDirectory().absolutePath)
        .append(File.separator)
        .append(Environment.DIRECTORY_PICTURES)
        .append(File.separator).
        append(subDir)
        .append(File.separator)

    val dir = File(path.toString())
    if (!dir.exists()) dir.mkdir()
    return path.toString()
}


/**
 * 拷贝asset文件到指定路径
 *
 * @param context   context
 * @param assetName asset文件
 * @param savePath  目标路径
 * @param saveName  目标文件名
 */
fun copyFileFromAssets(
    context: Context,
    assetName: String,
    savePath: String,
    saveName: String,
) {
    // 若目标文件夹不存在，则创建
    val dir = File(savePath)
    if (!dir.exists()) {
        if (!dir.mkdir()) {
            return
        }
    }

    // 拷贝文件
    val filename = "$savePath/$saveName"
    val file = File(filename)
    if (!file.exists()) {
        try {
            val inStream: InputStream = context.assets.open(assetName)
            val fileOutputStream = FileOutputStream(filename)
            var byteread: Int
            val buffer = ByteArray(1024)
            while (inStream.read(buffer).also { byteread = it } != -1) {
                fileOutputStream.write(buffer, 0, byteread)
            }
            fileOutputStream.flush()
            inStream.close()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.d("FileUtils", "[copyFileFromAssets] copy asset file: $assetName to : $filename")
    } else {
        Log.d("FileUtils", "[copyFileFromAssets] file is exist: $filename")
    }
}


/**
 * 文件转byteArray
 */
fun File.toByteArray() : ByteArray?{
    if (!this.exists()){
        return null
    }
    val bytesArray = ByteArray(this.length().toInt())
    val fis = FileInputStream(this)
    fis.read(bytesArray)
    fis.close()
    return bytesArray
}

fun ByteArray.toBitmap() :Bitmap?{
    val options = BitmapFactory.Options()
    options.inSampleSize = 1
    val input = ByteArrayInputStream(this)
    val softRef = SoftReference(BitmapFactory.decodeStream(input, null, options)) //软引用防止OOM
    val bitmap: Bitmap ?= softRef.get()
    input.close()
    return bitmap
}


/**
 * 将Byte数组转换成文件
 */
fun ByteArray.toFile() :File?{
    var bos: BufferedOutputStream? = null
    var fos: FileOutputStream? = null
    var file: File? = null
    try {
        file = File(getExternalPicturesPath(),"${System.currentTimeMillis()}.jpg")
        fos = FileOutputStream(file)
        bos = BufferedOutputStream(fos)
        bos.write(this)
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        if (bos != null) {
            try {
                bos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (fos != null) {
            try {
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    return file
}