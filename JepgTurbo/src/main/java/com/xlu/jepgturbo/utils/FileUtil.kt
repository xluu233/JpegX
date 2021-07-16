package com.xlu.jepgturbo.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.xlu.jepgturbo.getCurentTime
import java.io.*
import java.util.*


/**
 * @ClassName FileUtil
 * @Description TODO
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/7/12 16:11
 */
object FileUtil {

    /**
     * TODO Uri转File
     *
     * @param context
     * @param uri
     * @return
     */
    fun uri2File(context: Context, uri: Uri): File? {
        var file:File ?= null

        file = File(uri.toString())
        if (file.exists()) return file

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            when (uri.scheme) {
                ContentResolver.SCHEME_FILE -> {
                    file = File(requireNotNull(uri.path))
                }
                ContentResolver.SCHEME_CONTENT -> {
                    //把文件保存到沙盒
                    val contentResolver = context.contentResolver
                    val displayName = "${System.currentTimeMillis()}.${
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(
                                contentResolver.getType(uri)
                        )
                    }"
                    val ios = contentResolver.openInputStream(uri)
                    if (ios != null) {
                        file = File(getJpgPathEndWithSeparator(context), displayName).apply {
                            val fos = FileOutputStream(this)
                            FileUtils.copy(ios, fos)
                            fos.close()
                            ios.close()
                        }
                    }
                }
                else -> {

                }
            }
            return file
        }else{
            var path: String? = null
            when(uri.scheme){
                "file" -> {
                    path = uri.encodedPath
                    if (path != null) {
                        path = Uri.decode(path)
                        val cr = context.contentResolver
                        val buff = StringBuffer()
                        buff.append("(").append(MediaStore.Images.ImageColumns.DATA).append("=")
                                .append("'$path'").append(")")
                        val cur: Cursor? = cr.query(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                arrayOf(
                                        MediaStore.Images.ImageColumns._ID,
                                        MediaStore.Images.ImageColumns.DATA
                                ),
                                buff.toString(),
                                null,
                                null
                        )
                        var index = 0
                        var dataIdx = 0
                        cur?.let {
                            cur.moveToFirst()
                            while (!cur.isAfterLast()) {
                                index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID)
                                index = cur.getInt(index)
                                dataIdx = cur.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                                path = cur.getString(dataIdx)
                                cur.moveToNext()
                            }
                            cur.close()
                        }
                        if (index == 0) {
                        } else {
                            val u = Uri.parse("content://media/external/images/media/$index")
                            println("temp uri is :$u")
                        }
                    }
                }
                "content" -> {
                    // 4.2.2以后
                    val proj = arrayOf(MediaStore.Images.Media.DATA)
                    val cursor: Cursor? = context.contentResolver.query(uri, proj, null, null, null)
                    cursor?.let {
                        if (cursor.moveToFirst()) {
                            val columnIndex: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                            path = cursor.getString(columnIndex)
                        }
                        cursor.close()
                    }
                }
                else -> {
                    //Log.i(TAG, "Uri Scheme:" + uri.getScheme());
                }
            }
            return File(path)
        }
    }


    /**
     * TODO File转Uri
     *
     * @param context
     * @param file
     * @return
     */
    fun file2Uri(context: Context, file: File?):Uri?{
        if (file==null) return null

        var uri:Uri ?= null
        uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //适配Android 7.0文件权限，通过FileProvider创建一个content类型的Uri
            FileProvider.getUriForFile(context, "${context.packageName}.fileProvider", file)
        } else {
            Uri.fromFile(file)
        }
        return uri
    }

    /**
     * @return /data/data/com.xlu.compress/files/jpg/
     */
    fun getJpgPathEndWithSeparator(context: Context): String {
        val path = StringBuilder(context.filesDir.absolutePath)
        path.append(File.separator).append("jpg").append(File.separator)
        val dir = File(path.toString())
        if (!dir.exists()) dir.mkdir()
        return path.toString()
    }

    /**
     * @return /data/data/com.xlu.compress/files/temp/
     */
    fun getTempPathEndWithSeparator(context: Context): String {
        val path = StringBuilder(context.filesDir.absolutePath)
        path.append(File.separator).append("temp").append(File.separator)
        val dir = File(path.toString())
        if (!dir.exists()) dir.mkdir()
        return path.toString()
    }

    /**
     * TODO Cache缓存目录
     * @return /data/data/com.xlu.compress/cache
     */
    fun getCachePath(context: Context):String{
        val path = StringBuilder(context.cacheDir.absolutePath)
        //path.append(File.separator).append("temp").append(File.separator)
        val dir = File(path.toString())
        if (!dir.exists()) dir.mkdir()
        return path.toString()
    }

    /**
     * TODO 创建一个用于拍照的Jpeg文件
     * @param context
     * @param name
     * @return
     */
    fun createJpegFile(context: Context, name: String = getCurentTime() + ".jpg"):File{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //api >= android10 ,存储在沙盒
            return File(getTempPathEndWithSeparator(context), name)
        } else {
            //适用于Api < Android 10，保存Download目录下
            val dir: File = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/${context.packageName}")!!
            if (!dir.exists()) {
                dir.mkdir()
            }
            return File(dir, name)
        }
    }


    /**
     * 将文件转换成byte数组
     * @param filePath
     * @return
     */
    fun file2Byte(file: File?): ByteArray? {
        if (file==null) return null

        var buffer: ByteArray? = null
        try {
            val fis = FileInputStream(file)
            val bos = ByteArrayOutputStream()
            val b = ByteArray(1024)
            var n: Int
            while (fis.read(b).also { n = it } != -1) {
                bos.write(b, 0, n)
            }
            fis.close()
            bos.close()
            buffer = bos.toByteArray()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return buffer
    }

}