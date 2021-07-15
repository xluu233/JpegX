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


static double now_ms(void) {
    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    return 1000.0 * res.tv_sec + (double) res.tv_nsec / 1e6;
}

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

//jbyteArray to unsigned char
unsigned char* as_unsigned_char_array(JNIEnv *env, jbyteArray array) {
    int len = env->GetArrayLength (array);
    unsigned char* buf = new unsigned char[len];
    env->GetByteArrayRegion (array, 0, len, reinterpret_cast<jbyte*>(buf));
    return buf;
}



extern "C"
JNIEXPORT jboolean JNICALL
Java_com_xlu_jepgturbo_JpegTurbo_compressBitmap(JNIEnv *env, jobject thiz, jobject bitmap,jint _width,jint _height,jint quality, jstring filePath) {

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

    //setup1 : 将bitmap转换为Byte
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
    JpegHelper jpegHelper;
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
Java_com_xlu_jepgturbo_JpegTurbo_compressByteBuffer(JNIEnv *env, jobject thiz, jobject byte_buffer,
        jint width, jint height, jint quality) {


    uint8_t * rgbBuffer = ( uint8_t *) env->GetDirectBufferAddress(byte_buffer);

    tjhandle handle = NULL;
    int flags = 0;
    int pad = 4; //字节对齐
    int subsamp = TJSAMP_420;
    int pixelfmt = TJPF_RGBA;//TJPF_RGBA;//TJPF_ARGB;

    handle=tjInitCompress();
    if (NULL == handle){
        return NULL;
    }

    unsigned char* srcbuf = rgbBuffer;
    unsigned char* dstbuf = NULL;
    unsigned long outjpg_size = 0;


    int ret = tjCompress2(handle, srcbuf, width,0, height, pixelfmt, &dstbuf, &outjpg_size, subsamp, quality, flags);
    tjDestroy(handle);

    if (0 != ret) {
        return NULL;
    }

    jbyteArray data = env->NewByteArray(outjpg_size);
    env->SetByteArrayRegion(data, 0, outjpg_size, (jbyte *)dstbuf);

    tjFree(dstbuf);
    return data;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_xlu_jepgturbo_JpegTurbo_compressByteArray(JNIEnv *env, jobject thiz, jbyteArray byte,
                                                   jint width, jint height, jint quality) {

    jbyte * rgbBuffer = env->GetByteArrayElements(byte, 0);

    tjhandle handle = NULL;
    int flags = 0;
    int pad = 4; //字节对齐
    int subsamp = TJSAMP_420;
    int pixelfmt = TJPF_RGBA;

    handle = tjInitCompress();
    if (NULL == handle){
        return NULL;
    }

    unsigned char* srcbuf = (unsigned char*)rgbBuffer;
    unsigned char *dstBuf = NULL;
    unsigned long outjpg_size;


    int ret = tjCompress2(handle, srcbuf, width, 0, height, pixelfmt, &dstBuf, &outjpg_size, subsamp, quality, flags);


    tjDestroy(handle);
    if (0 != ret) {
        return NULL;
    }

    jbyteArray data = env->NewByteArray(outjpg_size);
    env->SetByteArrayRegion(data, 0, outjpg_size, reinterpret_cast<const jbyte *>(dstBuf));

    free(dstBuf);
    env->ReleaseByteArrayElements(byte, rgbBuffer, 0);

    return data;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_xlu_jepgturbo_JpegTurbo_compressByte2Jpeg(JNIEnv *env, jobject thiz, jbyteArray byte,
                                                   jint width, jint height, jint quality,
                                                   jstring output_file_path) {

    LOGD("compressByte2Jpeg");
    const char *location = env->GetStringUTFChars(output_file_path,NULL);

    jbyte *rgbBuffer = env->GetByteArrayElements(byte, 0);
    unsigned char *dstbuff = (unsigned char*)rgbBuffer;

    //unsigned char *dstbuff =  as_unsigned_char_array(env,byte);

    JpegHelper jpegHelper;
    int result = jpegHelper.write_jpeg_file(location, height, width, quality, dstbuff);

    env->ReleaseByteArrayElements(byte, rgbBuffer, 0);
    return result;
}