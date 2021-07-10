
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
#include "logcat.h"


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
