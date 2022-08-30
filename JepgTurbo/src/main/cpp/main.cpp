#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>
#include <csetjmp>
#include <cstdio>
#include <cmath>
#include <cstdint>
#include <ctime>
#include <zconf.h>
#include <sys/time.h>
#include <vector>

#include "jpeghelper/JpegHelper.h"

//
// Created by xlu on 2021/7/10.
//

typedef uint8_t BYTE;
typedef struct my_error_mgr *my_error_ptr;

JpegHelper jpegHelper;


//get system time
long getCurrentTime()
{
    struct timeval tv;
    gettimeofday(&tv,NULL);
    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

//jbyteArray to char
char* ConvertJByteaArrayToChars(JNIEnv *env, jbyteArray bytearray)
{
    char *chars;
    jbyte *bytes;
    bytes = env->GetByteArrayElements(bytearray, 0);
    int chars_len = env->GetArrayLength(bytearray);
    chars = new char[chars_len + 1];
    memset(chars,0,chars_len + 1);
    memcpy(chars, bytes, chars_len);
    chars[chars_len] = 0;

    env->ReleaseByteArrayElements(bytearray, bytes, 0);
    free(chars);
    return chars;
}

unsigned char* byteArray_to_unchar(JNIEnv *env, jbyteArray array) {
    int len = env->GetArrayLength (array);
    unsigned char* buf = new unsigned char[len];
    env->GetByteArrayRegion (array, 0, len, reinterpret_cast<jbyte*>(buf));
    return buf;
}

jbyteArray unchar_to_byteArray(JNIEnv *env, unsigned char *buf, int len) {
    jbyteArray array = env->NewByteArray(len);
    env->SetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(buf));
    return array;
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_xlu_jepgturbo_utils_JpegNative_compressBitmap(JNIEnv *env, jobject thiz, jobject bitmap, jint _width, jint _height, jint quality, jstring filePath) {

    const char *location = env->GetStringUTFChars(filePath,NULL);

    int ret;
    int color;
    BYTE r;
    BYTE g;
    BYTE b;
    BYTE *data;
    BYTE *tmpData;
    BYTE *bitmapPixels;
    AndroidBitmapInfo bitmapInfo;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo)) < 0) {
        LOGD("get bitmap info failed");
    }
    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGD("format is rgba_8888");
    }
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, reinterpret_cast<void **>(&bitmapPixels))) <0) {
        LOGD("lock pixels failed");
    }


    int width;
    int height;
    if (0 == _width){
        width = bitmapInfo.width;
    } else{
        width = _width;
    }

    if (0 == _height){
        height = bitmapInfo.height;
    } else{
        height = _height;
    }
    LOGD("wid id %d , height is %d",width,height);

    //setup1: 将bitmap转换为Byte
    long start = getCurrentTime();
    data = static_cast<BYTE *>(malloc(width * height * 3));
    tmpData = data;
    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            color = *((int *) bitmapPixels);
            r = ((color & 0x00FF0000) >> 16);
            g = ((color & 0x0000FF00) >> 8);
            b = color & 0x000000FF;
            *data = b;
            *(data + 1) = g;
            *(data + 2) = r;
            data = data + 3;
            bitmapPixels += 4;
        }
    }
    long end = getCurrentTime();
    long delta = end - start;
    LOGD("tran bitmap is %ld ms ",delta);

    //setup2 : 开始压缩
    int resultCode = jpegHelper.GenerateBitmap2Jpeg(tmpData, width, height,quality,location);

    if(resultCode==0){
        LOGD("Generate error");
    } else{
        LOGD("Generate success");
    }
    long end2 = getCurrentTime();
    long delta2 = end2 - end;
    LOGD("compress time is %ld ms ",delta2);

    free(tmpData);
    AndroidBitmap_unlockPixels(env, bitmap);
    return resultCode;
}


extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_xlu_jepgturbo_utils_JpegNative_compressByte2Byte(JNIEnv *env, jobject thiz, jbyteArray input_byte, jint width, jint height, jint quality) {

    jbyte *rgbBuffer = env->GetByteArrayElements(input_byte, 0);
    const unsigned char *srcBuf = (unsigned char*) rgbBuffer;
    unsigned char *dstBuf = NULL;
    unsigned long jpegSize = 0;

    LOGI("jbyte %d", sizeof(rgbBuffer));

    jpegHelper.initHandle();

    LOGI("compressRgba8888ToJpeg start");
    int compressResult = jpegHelper.compressByteToByte(srcBuf, width, height, &dstBuf, &jpegSize, quality);
    LOGI("compressRgba8888ToJpeg finished");


    if (compressResult != 0) {
        LOGE("compress failed , errorCode is %d", compressResult);
        return nullptr;
    } else {
        jbyteArray result = env->NewByteArray(jpegSize);
        env->SetByteArrayRegion(result, 0, jpegSize, reinterpret_cast<const jbyte *>(dstBuf));
        LOGE("compress success , jpegSize  %d", jpegSize);
        free(dstBuf);
        return result;
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_xlu_jepgturbo_utils_JpegNative_compressByte2File(JNIEnv *env, jobject thiz, jbyteArray byte, jint width, jint height, jint quality, jstring output_file_path) {

    LOGD("compressByte2Jpeg");
    const char *location = env->GetStringUTFChars(output_file_path,NULL);

    jbyte *rgbBuffer = env->GetByteArrayElements(byte, 0);
    unsigned char *dstBuffer = (unsigned char*)rgbBuffer;

    int result = jpegHelper.write_jpeg_file(location, height, width, quality, dstBuffer);

    if(result==0){
        LOGD("write file error");
    } else{
        LOGD("write file success");
    }

    env->ReleaseByteArrayElements(byte, rgbBuffer, 0);
    return result;
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_xlu_jepgturbo_utils_JpegNative_compressFile(JNIEnv *env, jobject thiz, jstring file_path, jint width, jint height, jint quality) {

    LOGD("compressFile");
    int file_width = 0;
    int file_height = 0;
    int file_size = 0;

    unsigned char *dstBuf = NULL;
    const char *location = env->GetStringUTFChars(file_path,NULL);

//    jpegHelper.initHandle();

    int result = jpegHelper.read_jpeg_file(location,&dstBuf, &file_size, &file_width, &file_height);
    if(result==0){
        LOGD("read file error");
    } else{
        LOGD("read file success");
    }

    int result2;
    if (width==0 || height==0){
        result2 = jpegHelper.write_jpeg_file(location, file_height, file_width, quality, dstBuf);
    } else{
        result2 = jpegHelper.write_jpeg_file(location, height, width, quality, dstBuf);
    }

    if(result2==0){
        LOGD("write file error");
    } else{
        LOGD("write file success");
    }

    return result2;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_xlu_jepgturbo_utils_JpegNative_compressFile2File(JNIEnv *env, jobject thiz, jstring file_path,
                                               jstring output_file_path, jint width, jint height,
                                               jint quality) {

    LOGD("compressFile2File");
    const char *location = env->GetStringUTFChars(file_path,NULL);
    const char *location_out = env->GetStringUTFChars(output_file_path,NULL);

    jpegHelper.initHandle();
    int result = jpegHelper.compressJpeg2Jpeg(location,location_out,quality,width,height);

    if(result==0){
        LOGD("compress jpeg file error");
    } else{
        LOGD("compress jpeg file success");
    }
    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_xlu_jepgturbo_utils_JpegNative_readFile2Byte(JNIEnv *env, jobject thiz, jstring file_path) {

    int file_width = 0;
    int file_height = 0;
    int file_size = 0;

    unsigned char *dstBuf = NULL;
    const char *location = env->GetStringUTFChars(file_path,NULL);

    int result = jpegHelper.read_jpeg_file(location,&dstBuf, &file_size, &file_width, &file_height);

    if(result==0){
        LOGD("convert error");
    } else{
        LOGD("convert success");
    }

    return unchar_to_byteArray(env,dstBuf,file_size);

}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_xlu_jepgturbo_utils_JpegNative_compressBitmap2Byte(JNIEnv *env, jobject thiz,jobject bitmap, jint _width, jint _height,jint quality) {

    jbyteArray result;
    int res = jpegHelper.bitmap2Byte(env,bitmap,&result,_width,_height,quality);

    if (res == 1){
        return result;
    } else {
        return nullptr;
    }
}
