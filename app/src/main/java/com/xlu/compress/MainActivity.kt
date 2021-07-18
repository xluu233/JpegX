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
import com.xlu.jepgturbo.utils.FileUtil
import com.xlu.jepgturbo.CompressListener
import com.xlu.jepgturbo.Formats
import com.xlu.jepgturbo.JpegTurbo
import com.xlu.jepgturbo.utils.BitmapUtil
import kotlinx.coroutines.*
import java.io.*


const val PERMISSION_REQUEST_CODE:Int = 1001

const val PHOTO_ALBUM = 1000
const val PHOTO_CAMERA = 2000

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var file:File ?= null
    private var uri: Uri ?= null
    private var bitmap:Bitmap ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //申请权限
        ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
        )
    }

    /**
     * TODO 从相册选择图片
     */
    fun chooseImage(view: View) {
        val intentToPickPic = Intent(Intent.ACTION_PICK, null)
        intentToPickPic.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(intentToPickPic, PHOTO_ALBUM)
    }

    /**
     * TODO 打开相机拍照
     */
    fun openCamera(view: View) {
        //调起相机拍照。
        file = FileUtil.createJpegFile(this)
        uri = FileUtil.file2Uri(this, file)

        uri?.let {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, it)
            startActivityForResult(captureIntent, PHOTO_CAMERA)
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

        binding.imageViewBefore.setImageURI(uri)

        val outFileSize = FileSizeUtil.getFolderOrFileSize(
                file.absolutePath,
                FileSizeUtil.SIZETYPE_KB
        )
        binding.imageInfoBefore.text = "原图\n文件大小：$outFileSize KB"

        bitmap = BitmapUtil.convertToBitmap(file)
        if (bitmap == null) return

/*        CoroutineScope(Dispatchers.IO).launch {
            //compressFile()
            //compressFileAndroid()

        }*/
        compressSync()
        compressAsync()
    }

    /**
     * TODO LibJpegTurbo压缩
     * 调用JNI方法压缩
     */
    @SuppressLint("SetTextI18n")
    private suspend fun compressFile() = withContext(Dispatchers.IO){
        //创建输出文件
        val outputFile = FileUtil.createJpegFile(this@MainActivity, "${file!!.name.replace(".jpg", "")}_compress.jpg")

        val time = System.currentTimeMillis()

        //压缩文件
        //JpegTurbo.compressFile2File(filePath = file!!.absolutePath, outputFilePath = outputFile.absolutePath)

        //压缩bitmap
        JpegTurbo.compressBitmap(bitmap = bitmap!!,outputFilePath = outputFile.absolutePath)

        //压缩后的文件大小
        val outFileSize = FileSizeUtil.getFolderOrFileSize(
                outputFile.absolutePath,
                FileSizeUtil.SIZETYPE_KB
        )

        withContext(Dispatchers.Main){
            binding.imageViewAfter.setImageURI(Uri.parse(outputFile.absolutePath))
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
     * TODO 异步压缩
     * 可通过async参数设置
     */
    private fun compressAsync(){
        //创建输出文件
        val outputFile = FileUtil.createJpegFile(this@MainActivity, "${file!!.name.replace(".jpg", "")}_compress_${System.currentTimeMillis()}.jpg")
        val time = System.currentTimeMillis()

        JpegTurbo.setParams(
                input = bitmap!!,
                output = outputFile
        ).compress(object : CompressListener<String>{
            override fun onStart() {
                Log.d(TAG,"compress started")
            }

            override fun onCompleted(success: Boolean, result: String?) {
                Log.d(TAG,"output file path is ${result}")

                runOnUiThread {
                    val outFileSize = FileSizeUtil.getFolderOrFileSize(outputFile.absolutePath, FileSizeUtil.SIZETYPE_KB)
                    binding.imageViewAfter2.setImageURI(Uri.parse(result))
                    binding.imageInfoAfter2.text = "JpegTurbo 异步压缩\n文件大小：$outFileSize KB\n压缩耗时:${System.currentTimeMillis()-time}ms"
                }
            }
        })

    }

    /**
     * TODO 同步压缩
     * async = false
     */
    private fun compressSync(){
        val outputFile = FileUtil.createJpegFile(this@MainActivity, "${file!!.name.replace(".jpg", "")}_compress_${System.currentTimeMillis()}.jpg")
        val time = System.currentTimeMillis()

        val outFilePath : String ?= JpegTurbo.setParams(
                input = file!!,
                output = outputFile,
                async = false
        ).compress<String>()


        val outFileSize = FileSizeUtil.getFolderOrFileSize(outputFile.absolutePath, FileSizeUtil.SIZETYPE_KB)
        binding.imageViewAfter.setImageURI(Uri.parse(outFilePath))
        binding.imageInfoAfter.text = "JpegTurbo 同步压缩\n文件大小：$outFileSize KB\n压缩耗时:${System.currentTimeMillis()-time}ms"

    }

}
