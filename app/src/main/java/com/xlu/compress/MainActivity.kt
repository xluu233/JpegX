package com.xlu.compress

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.xlu.compress.databinding.ActivityMainBinding
import com.xlu.compress.utils.FileSizeUtil
import com.xlu.compress.utils.FileUtil
import com.xlu.jepgturbo.CompressListener
import com.xlu.jepgturbo.Formats
import com.xlu.jepgturbo.JpegTurbo
import com.xlu.jepgturbo.utils.BitmapUtil
import kotlinx.coroutines.*
import java.io.*


const val PERMISSION_CAMERA_REQUEST_CODE:Int = 1001

const val PHOTO_ALBUM = 1000
const val PHOTO_CAMERA = 2000

const val TAG = "JPEG"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    val imageBefore by lazy {
        binding.imageViewBefore
    }
    val imageAfter by lazy {
        binding.imageViewAfter
    }


    private var file:File ?= null
    private var uri: Uri ?= null
    private var bitmap:Bitmap ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    /**
     * TODO 从相册选择图片
     */
    fun ChooseImage(view: View) {
        val intentToPickPic = Intent(Intent.ACTION_PICK, null)
        intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(intentToPickPic, PHOTO_ALBUM)
    }


    /**
     * TODO 打开相机拍照
     */
    fun OpenCamera(view: View) {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
                application,
                Manifest.permission.CAMERA
        )
        if (hasCameraPermission == PackageManager.PERMISSION_GRANTED) {
            //调起相机拍照。
            file = FileUtil.createJpegFile(this)
            uri = FileUtil.file2Uri(this, file)

            if (uri != null) {
                val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                //captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                startActivityForResult(captureIntent, PHOTO_CAMERA)
            }
        } else {
            //申请权限
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_CAMERA_REQUEST_CODE
            )
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PHOTO_ALBUM -> {
                    uri = data?.data
                    file = FileUtil.uri2File(this, uri!!)
                    sizeFile(file)
                }
                PHOTO_CAMERA -> {
                    sizeFile(file)
                }
                else -> super.onActivityResult(requestCode, resultCode, data)
            }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("SetTextI18n")
    private fun sizeFile(file: File?) {
        if (file==null) return

        Log.d(TAG, "uri:${uri.toString()}")
        Log.d(TAG, "file:${file.absolutePath}")

        imageBefore.setImageURI(uri)

        val outFileSize = FileSizeUtil.getFolderOrFileSize(
                file.absolutePath,
                FileSizeUtil.SIZETYPE_KB
        )
        binding.imageInfoBefore.text = "原图\n文件大小：$outFileSize KB"

        bitmap = BitmapUtil.convertToBitmap(file)
        if (bitmap == null) return

        CoroutineScope(Dispatchers.IO).launch {

            compressFile()
            //compressFileAndroid()
            //compressByte2Jpeg()

        }




    }

    /**
     * TODO LibJpegTurbo压缩
     * 输入Bitmap，输出File
     */
    @SuppressLint("SetTextI18n")
    private suspend fun compressFile() = withContext(Dispatchers.IO){
        //创建输出文件
        val outputFile = FileUtil.createJpegFile(this@MainActivity, "${file!!.name.replace(".jpg", "")}_compress.jpg")

        val time = System.currentTimeMillis()
/*        JpegTurbo.compressBitmap(
                bitmap = bitmap!!,
                outputFilePath = outputFile.absolutePath
        )*/


/*        JpegTurbo.setParams(
            input = bitmap,
            output = outputFile.absolutePath
        ).compress(object :CompressListener<String>{
            override fun onStart() {
                Log.d(TAG,"onStart")
            }

            override fun onCompleted(success: Boolean, result: String?) {
                Log.d(TAG,"onCompleted,success:$success, result:${result}")
            }
        })*/

        JpegTurbo.setParams(
            input = bitmap!!,
            outputType = Formats.Byte
        ).compress(object :CompressListener<ByteArray>{
            override fun onStart() {
                Log.d(TAG,"onStart")
            }

            override fun onCompleted(success: Boolean, result: ByteArray?) {
                Log.d(TAG,"onCompleted,success:$success, result:${result}")
/*                bitmap = BitmapUtil.deconvertByte(result)
                binding.imageViewAfter.setImageBitmap(bitmap)*/
            }
        })


/*        val outputByte :ByteArray ?= JpegTurbo.compressByteArray(
            byte = byte!!,
            height = height,
            width = width
        )

        val bitmap = BitmapUtil.deconvertByte(outputByte)*/

        //压缩后的文件大小
        val outFileSize = FileSizeUtil.getFolderOrFileSize(
                outputFile.absolutePath,
                FileSizeUtil.SIZETYPE_KB
        )

        withContext(Dispatchers.Main){
            //binding.imageViewAfter.setImageURI(Uri.parse(outputFile.absolutePath))
            binding.imageInfoAfter.text = "JpegTurbo bitmap压缩\n文件大小：$outFileSize KB\n压缩耗时:${System.currentTimeMillis()-time}ms"
        }
    }


    /**
     * TODO Android原生压缩
     */
    private suspend fun compressFileAndroid() = withContext(Dispatchers.IO){
        //创建输出文件
        val outputFile = FileUtil.createJpegFile(this@MainActivity, "${file!!.name.replace(".jpg", "")}_compress2.jpg")
        val bitmap = BitmapUtil.convertToBitmap(file) ?: return@withContext

        //Android原生压缩设置 quality<90 之后会有明显的质量下降
        try {
            val bos = BufferedOutputStream(FileOutputStream(outputFile))
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, bos)
            bos.flush()
            bos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val outFileSize = FileSizeUtil.getFolderOrFileSize(outputFile.absolutePath, FileSizeUtil.SIZETYPE_KB)
        withContext(Dispatchers.Main){
            binding.imageViewAfter2.setImageBitmap(bitmap)
            binding.imageInfoAfter2.text = "Android原生压缩\n文件大小：$outFileSize KB\n"
        }
    }

    /**
     * TODO LibJpegTurbo压缩
     * 输入byte[]，输出File
     */
    private suspend fun compressByte2Jpeg() = withContext(Dispatchers.IO){
        //模拟数据
        val outputFile = FileUtil.createJpegFile(this@MainActivity, "${file!!.name.replace(".jpg", "")}_compress3.jpg")
        var byte : ByteArray ?= null

        bitmap?.let {
            byte = BitmapUtil.convertToByteArray(it)
        }
        if (byte== null || bitmap==null) return@withContext

        val width = bitmap!!.width
        val height = bitmap!!.height

        //开始压缩
        val time = System.currentTimeMillis()
/*
        val result: Boolean = JpegTurbo.compressByte2Jpeg(
                byte = byte!!,
                height = height,
                width = width,
                outputFilePath = outputFile.absolutePath
        )
*/


        val outputByte :ByteArray ?= JpegTurbo.compressByte(
            byte = byte!!,
            height = height,
            width = width
        )

        val bitmap = BitmapUtil.deconvertByte(outputByte)

        val outFileSize = FileSizeUtil.getFolderOrFileSize(outputFile.absolutePath, FileSizeUtil.SIZETYPE_KB)
        withContext(Dispatchers.Main){
            binding.imageViewAfter3.setImageBitmap(bitmap)
            //binding.imageViewAfter3.setImageURI(Uri.parse(outputFile.absolutePath))
            binding.imageInfoAfter3.text = "JpegTurbo byte[]压缩\n文件大小：$outFileSize KB\n耗时：${System.currentTimeMillis()-time}ms"
        }

    }

}