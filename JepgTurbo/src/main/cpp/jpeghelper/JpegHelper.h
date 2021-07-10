
#ifndef INSTANTGLSL_JPEGHELPER_H
#define INSTANTGLSL_JPEGHELPER_H

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

#include "jconfig.h"
#include "error.h"
#include "jmorecfg.h"
#include "jpeglib.h"
#include "turbojpeg.h"

#define TAG "JpegTurboTest"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型

typedef uint8_t BYTE;

class JpegHelper {

    public:

    int read_jpeg_file(const char *jpeg_file, unsigned char **rgb_buffer, int *size, int *width,int *height);

    //将buffer保存为jpeg
    int write_jpeg_file (unsigned char* image_buffer, int quality,int image_height, int image_width);

    //输入bitmap ,输出jpeg
    int GenerateBitmap2Jpeg(BYTE *data, int w, int h, int quality,const char* outfilename);

    //输入bitmap,输出buffer
    int GenerateBitmap2Buffer(BYTE *data, int w, int h, int quality, const char *outfilename);


};


#endif
