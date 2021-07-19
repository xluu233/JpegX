package com.xlu.compress

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.xlu.compress.databinding.ActivityMainBinding
import com.xlu.compress.utils.FileSizeUtil
import com.xlu.jepgturbo.CompressListener
import com.xlu.jepgturbo.Formats
import com.xlu.jepgturbo.JpegTurbo
import com.xlu.jepgturbo.utils.BitmapUtil
import com.xlu.jepgturbo.utils.FileUtil
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
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
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

        CoroutineScope(Dispatchers.IO).launch {
            //compressFile()
            //compressFileAndroid()
            //compressByte()
            //compressByte2()
            compressBitmap2Byte()
        }
        //compressSync()
        //compressAsync()
    }



    private fun compressBitmap2Byte() {
        val time = System.currentTimeMillis()
        JpegTurbo.setParams(
                input = bitmap!!,
                outputType = Formats.Byte
        ).compress(object :CompressListener<ByteArray>{
            override fun onStart() {

            }

            override fun onCompleted(success: Boolean, result: ByteArray?) {
                val outBit = BitmapUtil.deconvertByte(result)
                Log.d(TAG,"delta_time:${System.currentTimeMillis()-time}")
                binding.imageViewBefore.setImageBitmap(outBit)
            }
        })

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
        JpegTurbo.compressBitmap(bitmap = bitmap!!, outputFilePath = outputFile.absolutePath)

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
        ).compress(object : CompressListener<String> {
            override fun onStart() {
                Log.d(TAG, "compress started")
            }

            override fun onCompleted(success: Boolean, result: String?) {
                Log.d(TAG, "output file path is ${result}")

                runOnUiThread {
                    val outFileSize = FileSizeUtil.getFolderOrFileSize(outputFile.absolutePath, FileSizeUtil.SIZETYPE_KB)
                    binding.imageViewAfter2.setImageURI(Uri.parse(result))
                    binding.imageInfoAfter2.text = "JpegTurbo 异步压缩\n文件大小：$outFileSize KB\n压缩耗时:${System.currentTimeMillis() - time}ms"
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


    private suspend fun compressByte(){
        val width = bitmap?.width!!
        val height = bitmap?.height!!
        val input:ByteArray = BitmapUtil.convertToByteArray(bitmap)!!

        //input_size:48771072 == width*height*4
        Log.d(TAG, "input_size:${input.size}")

        val output = JpegTurbo.setParams(
                input = input,
                outputType = Formats.Byte,
                width = width,
                height = height,
                async = false
        ).compress<ByteArray>()

        val _bitmap = BitmapUtil.deconvertByte(output)
        withContext(Dispatchers.Main){
            binding.imageViewAfter.setImageBitmap(_bitmap)
        }
    }


    private suspend fun compressByte2(){
        val width = bitmap?.width!!
        val height = bitmap?.height!!
        val input:ByteArray = BitmapUtil.convertToByteArray(bitmap)!!

        //input_size:48771072 == width*height*4
        Log.d(TAG, "input_size:${input.size}")

        val height1 = if (height/2 == 0){
            height/2
        }else{
            height/2 + 1
        }
        val height2 = height-height1
        val inputByte1 = ByteArray(height1 * width * 4)
        val inputByte2 = ByteArray(height2 * width * 4)

        System.arraycopy(input, 0, inputByte1, 0, inputByte1.size)
        System.arraycopy(input, inputByte1.size, inputByte2, 0, inputByte2.size)

        Log.d(TAG, "input1_size:${inputByte1.size}")
        Log.d(TAG, "input2_size:${inputByte2.size}")

        val output1 = JpegTurbo.setParams(
                input = inputByte1,
                outputType = Formats.Byte,
                width = width,
                height = height1,
                async = false
        ).compress<ByteArray>()

        val output2 = JpegTurbo.setParams(
                input = inputByte2,
                outputType = Formats.Byte,
                width = width,
                height = height2,
                async = false
        ).compress<ByteArray>()

        //output_size:1355379
        //1323kb

/*        val _bitmap1 = BitmapUtil.deconvertByte(output1)
        val _bitmap2 = BitmapUtil.deconvertByte(output2)
        withContext(Dispatchers.Main){
            binding.imageViewAfter.setImageBitmap(_bitmap1)
            binding.imageViewAfter2.setImageBitmap(_bitmap2)
        }*/

        //ByteArray(output1!!.size + output2!!.size)
//        val outByte = ByteArray(output1!!.size + output2!!.size)
//        System.arraycopy(output1, 0, outByte, 0, output1.size)
//        System.arraycopy(output2, 0, outByte, output1.size, output2.size)

        val outByte = byteMerger(output1!!,output2!!)
        val _bitmap = BitmapUtil.deconvertByte(outByte)

        Log.d(TAG, "_bitmap:${_bitmap?.width}x${_bitmap?.height}")
        withContext(Dispatchers.Main){
            binding.imageViewAfter.setImageBitmap(_bitmap)
        }

        Log.d(TAG, "outByte:${outByte.size},output_size1:${output1.size},output2_size:${output2.size}")
//        Log.d(TAG,outByte.decodeToString())
//        Log.d(TAG,output1.decodeToString())
//        Log.d(TAG,output2.decodeToString())
    }

    /**
     * 合并byte[]数组 （不改变原数组）
     * @param byte_1
     * @param byte_2
     * @return 合并后的数组
     */
    fun byteMerger(byte_1: ByteArray, byte_2: ByteArray): ByteArray {
        val byte_3 = ByteArray(byte_1.size + byte_2.size)
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.size)
        System.arraycopy(byte_2, 0, byte_3, byte_1.size, byte_2.size)
        return byte_3
    }

}
