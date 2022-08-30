//
// Created by xlu on 2022/8/30.
//
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

#ifndef JPEGX_JNIUTIL_H
#define JPEGX_JNIUTIL_H

#endif //JPEGX_JNIUTIL_H

long getCurrentTime();

char* ConvertJByteaArrayToChars(JNIEnv *env, jbyteArray bytearray);

unsigned char* byteArray_to_unchar(JNIEnv *env, jbyteArray array);

jbyteArray unchar_to_byteArray(JNIEnv *env, unsigned char *buf, int len);
