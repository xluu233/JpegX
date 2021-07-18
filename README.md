## LibJpegTurbo

由于`Android`端原生对`Bitmap`的处理支持有限，提供的图片压缩方式效果并不太好。我们知道Android的图像引擎是`Skia`,`Skia`对图片的处理就是有利用到`Libjpeg`，但是显然`Libjpeg`已经过去了太久，目前大多对`Jpeg`的处理都是用到`LibJpegTurbo`

> 对LibJpegTurbo感兴趣的可以自己前往[官网](https://github.com/libjpeg-turbo)查看，其编译方式也很简单，使用前查看其文档`example.txt`可以帮助快速入手。


## JpegX

`JpegX`是一个Android端利用[LibJpegTurbo](https://github.com/libjpeg-turbo)的轻量级图片处理框，支持多种数据类型压缩, 支持同步、异步调用，支持多线程分区压缩（暂未完成）

#### 支持的数据类型

`Bitmap` `byte[]`  `File` `File path`

> 目前暂时只支持Jpeg，后续可能会加入其他图片格式的支持 


#### 快速接入：


Step 1. Add it in your root build.gradle at the end of repositories:


```
    allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Step 2. Add the dependency


```
    dependencies {
	        implementation 'com.github.xluu233:JpegX:$last_release'
	}
```

#### 简单调用：

可以直接调用JNI方法，以Bitmap压缩到Jpeg文件为例：

```
JpegTurbo.compressBitmap(bitmap,outputFilePath)
```

#### 同步调用


```
val outFilePath : String ?= JpegTurbo.setParams(
        input = bitmap,
        output = outputFile,
        async = false
).compress<String>()
```

#### 异步调用：

 通过`CompressListener`接口监听

```

JpegTurbo.setParams(
    input = bitmap,
    output = outputFile
).compress(object : CompressListener<String>{
    override fun onStart() {
        Log.d(TAG,"compress started")
    }

    override fun onCompleted(success: Boolean, result: String?) {
        Log.d(TAG,"output file path is ${result}")
    }
})
```



## 更多方法详解：

JpegTurbo类中提供了`setParams()`方法进行设置参数，`compress()`进行压缩和设置回调。以下为不同数据类型的调用方式：

#### setParams()参数介绍：



| 参数<div style="width: 70pt"> | <div style="width: 70pt"> |                             备注                             |
| :---------------------------: | :-----------------------: | :----------------------------------------------------------: |
|             input             |          输入源           | （必要）支持输入类型：Bitmap, ByteArray, File, File路径(String) |
|            output             |          输出源           |  （可选）支持输出类型：Bitmap, ByteArray, File路径(String)   |
|          outputType           |         输出类型          | （可选） 输出类型会根据输出源自动判断，**如果没有设置输出源，必须手动设置输出类型** |
|             width             |           宽度            |                           （可选）                           |
|            height             |           高度            |                           （可选）                           |
|            maxSize            |      输出文件最大值       |                （可选）仅对输出类型为File有效                |
|            quality            |         压缩质量          |                  （可选）范围0-100，默认60                   |
|             async             |       开启异步压缩        |                       （可选）默认开启                       |
|          multiThread          |    开启多线程分区压缩     |                       （可选）默认关闭                       |


**注意：**

> 通过`Formats`枚举类设置输出类型，类型有`Formats.Byte`, `Formats.Bitmap`, `Formats.File`

> 设置不同输出类型时的`CompressListener`回调类型也不一样，`Byte`对应为`ByteArray`或`byte[]`,`File`对应为`String`，`Bitmap`对应回调类型`Bitmap`



#### 1. 输入源为Jpeg文件

> 压缩`File`，可以设置输出文件路径，没有则将覆盖原文件

- 输出为ByteArray

```
        JpegTurbo.setParams(
                input = file,
                outputType = Formats.Byte
        ).compress(object :CompressListener<String>{
            override fun onStart() {
                Log.d(TAG,"onStart")
            }

            override fun onCompleted(success: Boolean, result: String?) {
                Log.d(TAG,"onCompleted")
            }
        })
```


- 输出为Bitmap


```
        JpegTurbo.setParams(
                input = file,
                outputType = Formats.Bitmap
        ).compress(object :CompressListener<Bitmap>{
            override fun onStart() {
                Log.d(TAG,"onStart")
            }

            override fun onCompleted(success: Boolean, result: Bitmap?) {
                Log.d(TAG,"onCompleted")
            }
        })
```


- 输出为Jpeg文件

```
        //异步压缩
        JpegTurbo.setParams(
                input = file,
                output = outputFile
        ).compress(object :CompressListener<String>{
            override fun onStart() {
                Log.d(TAG,"onStart")
            }

            override fun onCompleted(success: Boolean, result: String?) {
                Log.d(TAG,"onCompleted")
            }
        })
        
        //同步压缩并覆盖原文件
        val result_path:String ?= JpegTurbo.setParams(
                input = file,
                outputType = Formats.File
        ).compress<String>
        
```


#### 2. 输入源为byte数组

> 压缩 `Byte`必须指定`width`,`height`参数

- 输出Bitmap

```
        JpegTurbo.setParams(
                input = byte,
                width = 1080,
                height = 1920,
                outputType = Formats.Bitmap
        ).compress(object :CompressListener<Bitmap>{
            override fun onStart() {
                Log.d(TAG,"onStart")
            }

            override fun onCompleted(success: Boolean, result: Bitmap?) {
                Log.d(TAG,"onCompleted")
            }
        })
```
- 输出File

```
        JpegTurbo.setParams(
                input = byte,
                width = 1080,
                height = 1920,
                output = outputFile
        ).compress(object :CompressListener<String>{
            override fun onStart() {
                Log.d(TAG,"onStart")
            }

            override fun onCompleted(success: Boolean, result: String?) {
                Log.d(TAG,"onCompleted")
            }
        })
```

- 同步输出Byte

```

        val result:ByteArray ?= JpegTurbo.setParams(
                input = byte,
                width = 1080,
                height = 1920,
                outputType = Formats.Byte
        ).compress<ByteArray>

        //如果result为null,说明压缩失败
```

> 输入源为Bitmap的时候，调用方法与上面基本一致。

---


本库最开始的目的是学习Libjpegturbo的使用，目前Android端的图片压缩框架很多，比如Luban，但我依旧是重复造了一个轮子🤣，自己动手的过程中才会进步，目前还有诸多不完善之处，欢迎提交issue和pull request。求大佬们一个star🧡💛 💚
