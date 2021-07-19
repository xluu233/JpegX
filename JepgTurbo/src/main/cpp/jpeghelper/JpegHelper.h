
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

    private:
//    tjhandle handle;

    public:

//    int initHandle();
//    int destroyHandle();

    /**
     * TODO 读取Jpeg文件信息
     * @param filePath 文件路径
     * @param rgb_buffer 输出buffer指针
     * @param size
     * @param width
     * @param height
     * @return
     */
    int read_jpeg_file(const char *filePath, JSAMPLE **rgb_buffer, int *size, int *width, int *height);

    int read_jpeg_file(const char *filePath, JSAMPLE **rgb_buffer);

    /*buffer保存为jpeg*/
    int write_jpeg_file(const char *filename, int image_height, int image_width, int quality, JSAMPLE *image_buffer);

    /*bitmap保存为jpeg*/
    int GenerateBitmap2Jpeg(BYTE *data, int w, int h, int quality, const char *outfilename);

    /*bitmap生成buffer*/
    int GenerateBitmap2Buffer(BYTE *data, int w, int h, int quality, JSAMPLE **rgb_buffer);

    /*直接压缩Jpeg文件*/
    int compressJpeg2Jpeg(const char *filePath,const char *out_filePath,int quality, int width, int height);


};


#endif
