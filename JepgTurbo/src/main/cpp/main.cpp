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
Java_com_xlu_jepgturbo_JepgTurbo_compressBitmap2File(JNIEnv *env, jobject thiz, jobject bitmap,jstring file_path) {



}