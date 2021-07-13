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

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_xlu_jepgturbo_JpegTurbo_compressBitmap2File(JNIEnv *env, jobject thiz, jobject bitmap, jstring file_path) {



}

JNIEXPORT void JNICALL
Java_com_xlu_jepgturbo_JpegTurbo_compressBitmap(JNIEnv *env, jobject thiz, jobject bitmap,
                                                jint quality, jstring filePath) {

    const char *location = env->GetStringUTFChars(filePath,NULL);

    //LOGD("location is %s",location);

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
        //int32_t
        LOGD("format is rgba_8888");
    }
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, reinterpret_cast<void **>(&bitmapPixels))) <0) {
        LOGD("lock pixels failed");
    }

    int width = bitmapInfo.width;
    int height = bitmapInfo.height;
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
}



}
