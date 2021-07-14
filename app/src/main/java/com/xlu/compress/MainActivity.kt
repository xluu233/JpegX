package com.xlu.compress

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.xlu.compress.databinding.ActivityMainBinding
import com.xlu.compress.utils.BitmapUtil
import com.xlu.compress.utils.FileSizeUtil
import com.xlu.compress.utils.FileUtil
import com.xlu.jepgturbo.JpegTurbo
import kotlinx.coroutines.*
import java.io.*
import java.nio.ByteBuffer


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

        compressFile()
    }

    /**
     * TODO LibJpegTurbo压缩Bitmap
     */
    @SuppressLint("SetTextI18n")
    private fun compressFile() = CoroutineScope(Dispatchers.IO).launch{
        //创建输出文件
        val outputFile = FileUtil.createJpegFile(this@MainActivity, "${file!!.name.replace(".jpg", "")}_compress.jpg")

        val time = System.currentTimeMillis()

        JpegTurbo.compressBitmap(
                bitmap = bitmap!!,
                outputFilePath = outputFile.absolutePath
        )

        //压缩后的文件大小
        val outFileSize = FileSizeUtil.getFolderOrFileSize(
                outputFile.absolutePath,
                FileSizeUtil.SIZETYPE_KB
        )

        withContext(Dispatchers.Main){
            binding.imageViewAfter.setImageURI(Uri.parse(outputFile.absolutePath))
            binding.imageInfoAfter.text = "JpegTurbo压缩\n文件大小：$outFileSize KB\n压缩耗时:${System.currentTimeMillis()-time}ms"

            compressFileAndroid()
        }
    }


    /**
     * TODO Android原生压缩 File
     * @param file
     */
    private fun compressFileAndroid() = CoroutineScope(Dispatchers.IO).launch{
        //创建输出文件
        val outputFile = FileUtil.createJpegFile(this@MainActivity, "${file!!.name.replace(".jpg", "")}_compress2.jpg")
        val bitmap = BitmapUtil.convertToBitmap(file)
        if (bitmap == null) this.cancel()

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

            compressByteBuffer2Jpeg()
        }
    }


    private fun compressByteBuffer2Jpeg() = CoroutineScope(Dispatchers.IO).launch{
        //模拟生成ByteBuffer
        val outputFile = FileUtil.createJpegFile(this@MainActivity, "${file!!.name.replace(".jpg", "")}_compress3.jpg")
        var byte : ByteArray ?= null
        val bitmap = BitmapUtil.convertToBitmap(file)

        bitmap?.let {
            byte = BitmapUtil.convertToByteArray(it)
        }
        if (byte== null || bitmap==null) this.cancel()


        val resultCode = JpegTurbo.compressByte2Jpeg(
                byte = byte!!,
                height = bitmap!!.height,
                width = bitmap.width,
                outputFilePath = outputFile.absolutePath
        )


        val outFileSize = FileSizeUtil.getFolderOrFileSize(outputFile.absolutePath, FileSizeUtil.SIZETYPE_KB)
        withContext(Dispatchers.Main){
            binding.imageViewAfter2.setImageURI(Uri.parse(outputFile.absolutePath))
            binding.imageInfoAfter2.text = "Android原asdasdas生压缩\n文件大小：$outFileSize KB\n"
        }

    }

}