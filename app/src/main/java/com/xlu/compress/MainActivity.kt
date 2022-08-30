package com.xlu.compress

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.xlu.compress.databinding.ActivityMainBinding
import com.xlu.compress.utils.*
import com.xlu.jepgturbo.JpegX
import com.xlu.jepgturbo.entitys.OutputFormat
import com.xlu.jepgturbo.itf.CompressListener
import com.xlu.jepgturbo.utils.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding


    private val testBitmap by lazy {
        getAssertBitmap(this@MainActivity)
    }

//    private val testFile by lazy {
//        getAssertFile(this@MainActivity)
//    }
    
    private val testFile by lazy {
        File("/data/data/com.xlu.compress/files/test.jpg")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //申请权限
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),1001)

        //初始化框架
        JpegX.init(this)


        binding.startCompress.setOnClickListener {
//            compressBitmap2File()
//            compressBitmapDSL()
//            compressBitmap2Byte()


            compressFile2File()
//            compressFile2Bitmap()
//            compressFile2Byte()


//            compressByte2Byte()
//            compressByte2File()

        }
    }


    /**
     * TODO SUCCESS
     * bitmap to file
     */
    private fun compressBitmap2File(){
        val outputFile = File(getExternalPicturesPath(),"${System.currentTimeMillis()}.jpg")

        JpegX.params(
            input = testBitmap,
            output = outputFile,
            outputType = OutputFormat.File
        ).compress(
            //设置listener后，默认为异步压缩
            listener = object : CompressListener<String> {
                override fun onStart() {
                    Log.d(TAG, "onStart，压缩前文件大小：${FileSizeUtil.getAutoFolderOrFileSize(testFile)}")
                }

                override fun onSuccess(result: String) {
                    Log.d(TAG, "onSuccess $result   压缩后文件大小:${FileSizeUtil.getAutoFolderOrFileSize(outputFile)}")
                    showResult(result)
                }

                override fun onFailed(error: String) {
                    Log.d(TAG, "onFailed,  $error")
                }
            }
        )
    }

    /**
     * TODO SUCCESS
     * bitmap to file,dsl style
     * @return
     */
    private fun compressBitmapDSL() = MainScope().launch{
        val outputFile = File(getExternalPicturesPath(),"${System.currentTimeMillis()}.jpg")
        JpegX.params(
            input = testBitmap,
            output = outputFile,
            outputType = OutputFormat.File
        ).compress<String>(
            onStart = {
                Log.d(TAG, "onStart，压缩前文件大小：${FileSizeUtil.getAutoFolderOrFileSize(testFile)}")
            },
            onSuccess = {
                Log.d(TAG, "onSuccess $it   压缩后文件大小:${FileSizeUtil.getAutoFolderOrFileSize(outputFile)}")
                showResult(it)
            },
            onFailed = {

            }
        )
    }


    /**
     * TODO SUCCESS
     * bitmap to byte success
     */
    private fun compressBitmap2Byte(){
        JpegX.params(
            input = testBitmap,
            outputType = OutputFormat.Byte,
            //width = 600,
            //height = 400
        ).compress<ByteArray>(
            async = true,
            onStart = {
                Log.d(TAG, "onStart，压缩前文件大小：${FileSizeUtil.getAutoFolderOrFileSize(testFile)}")
            },
            onFailed = {
                Log.d(TAG,"onFailed")
            },
            onSuccess = {
                Log.d(TAG, "onSuccess  压缩后 size:${it.size}  大小:${it.size/1024f/1024f} MB")
                showResult(it)
            },
        )
    }

    private fun compressByte2File() {
        val inputByte = testFile.toByteArray() ?: return
        val outputFile = File(getExternalPicturesPath(),"${System.currentTimeMillis()}.jpg")

        //对byte[]压缩必须指定宽高
        JpegX.params(
            input = inputByte,
            output = outputFile,
            outputType = OutputFormat.File,
            width = 6000,
            height = 4000,
        ).compress<String>(
            async = true,
            onStart = {
                Log.d(TAG, "onStart 压缩前文件大小：${FileSizeUtil.getAutoFolderOrFileSize(testFile)},  byte size:${inputByte.size}")
            },
            onSuccess = {
                Log.d(TAG, "onSuccess $it   压缩后文件大小:${FileSizeUtil.getAutoFolderOrFileSize(outputFile)}")

                runOnUiThread {
                    binding.imageViewAfter.setImageURI(Uri.fromFile(outputFile))
                }
            },
            onFailed = {
                Log.e(TAG,"onFailed:$it")
            }
        )

    }



    private fun compressByte2Byte() {
        val inputByte = testFile.toByteArray() ?: return

        //对byte[]压缩必须指定宽高
        JpegX.params(
            input = inputByte,
            outputType = OutputFormat.Byte,
            width = 6000,
            height = 4000,
        ).compress<ByteArray>(
            async = true,
            onStart = {
                Log.d(TAG, "onStart 压缩前文件大小：${FileSizeUtil.getAutoFolderOrFileSize(testFile)},  byte size:${inputByte.size}")

                runOnUiThread {
                    binding.imageViewBefore.setImageBitmap(testBitmap)
                }
            },
            onSuccess = {
                Log.d(TAG, "onSuccess  压缩后 size:${it.size}  大小:${it.size/1024f/1024f} MB")

                //  压缩后 size:1564332  大小:1.4918633 MB

                runOnUiThread {
                    binding.imageViewAfter.setImageURI(Uri.fromFile(it.toFile()))
                }
            },
            onFailed = {
                Log.e(TAG,"onFailed:$it")
            }
        )

    }

    private fun compressFile2File() {
        val outputFile = File(getExternalPicturesPath(), "${System.currentTimeMillis()}.jpg")

        JpegX.params(
            input = testFile,
            output = outputFile,
            outputType = OutputFormat.File
        ).compress<String>(
            async = false,
            onStart = {
                Log.d(TAG, "onStart，压缩前文件大小：${FileSizeUtil.getAutoFolderOrFileSize(testFile)}")
            },
            onSuccess = {
                Log.d(TAG, "onSuccess $it   压缩后文件大小:${FileSizeUtil.getAutoFolderOrFileSize(outputFile)}")
                showResult(it)
            },
            onFailed = {
                Log.e(TAG,"onFailed")
            }
        )
    }

    private fun compressFile2Byte() {
        JpegX.params(
            input = testFile,
            outputType = OutputFormat.Byte
        ).compress(listener = object :CompressListener<ByteArray>{
            override fun onStart() {

            }

            override fun onSuccess(result: ByteArray) {
                showResult(result)
            }

            override fun onFailed(error: String) {
                Log.e(TAG,"onFailed")
            }
        })
    }

    /**
     * TODO success
     */
    private fun compressFile2Bitmap() {
        JpegX.params(
            input = testFile,
            outputType = OutputFormat.Bitmap
        ).compress<Bitmap>(
            async = true,
            onStart = {
            },
            onSuccess = {
                showResult(it)
            },
            onFailed = {
                Log.e(TAG,"onFailed")
            }
        )
    }

    private fun showResult(bytes: ByteArray) = runOnUiThread{
        binding.imageViewAfter.setImageBitmap(bytes.toBitmap())
    }

    private fun showResult(bitmap: Bitmap) = runOnUiThread{
        binding.imageViewAfter.setImageBitmap(bitmap)
    }

    private fun showResult(filePath:String){
        runOnUiThread {
            binding.imageViewAfter.setImageURI(Uri.fromFile(File(filePath)))
        }
    }

}
