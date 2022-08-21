
#include "JpegHelper.h"

#include <stdio.h>
#include <setjmp.h>

struct my_error_mgr {
    struct jpeg_error_mgr pub;
    jmp_buf setjmp_buffer;
};

typedef struct my_error_mgr *my_error_ptr;

void my_error_exit(j_common_ptr cinfo) {
    my_error_ptr myerr = (my_error_ptr) cinfo->err;

    (*cinfo->err->output_message)(cinfo);

    longjmp(myerr->setjmp_buffer, 1);
}



int JpegHelper::read_jpeg_file(const char *filePath, JSAMPLE **rgb_buffer, int *size, int *width, int *height) {

    LOGD("start read jpeg file path is %s", filePath);
    struct jpeg_decompress_struct cinfo;
    struct jpeg_error_mgr jerr;

    int nComponent = 4;
    JSAMPARRAY buffer;
    uint8_t *pixels = nullptr;
    JSAMPROW *row_pointer = nullptr;

    int row_stride = 0;
    unsigned char *tmp_buffer = nullptr;
    int rgb_size;

    FILE *fp = fopen(filePath, "rb");

    if (fp == nullptr) {
        LOGD("open file failed");
        return 0;
    }

    cinfo.err = jpeg_std_error(&jerr);

    jpeg_create_decompress(&cinfo);

    jpeg_stdio_src(&cinfo, fp);

    jpeg_read_header(&cinfo, TRUE);

    cinfo.out_color_components = nComponent;
    cinfo.out_color_space = JCS_EXT_RGBA; // 设置输出格式

    jpeg_start_decompress(&cinfo);

    row_stride = cinfo.image_width << 2;

    *width = cinfo.image_width;
    *height = cinfo.image_height;

    rgb_size = row_stride * cinfo.output_height; // 总大小

    *size = rgb_size;

    buffer = (*cinfo.mem->alloc_sarray)((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);

    *rgb_buffer = (unsigned char *) malloc(sizeof(char) * rgb_size);    // 分配总内存

    tmp_buffer = *rgb_buffer;

    while (cinfo.output_scanline < cinfo.output_height) // 解压每一行
    {
        jpeg_read_scanlines(&cinfo, buffer, 1);
        // 复制到内存
        memcpy(tmp_buffer, buffer[0], row_stride);
        tmp_buffer += row_stride;
    }

    jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);

    fclose(fp);
    return 1;
}

int JpegHelper::read_jpeg_file(const char *filePath, JSAMPLE **rgb_buffer) {


    FILE *fp = fopen(filePath, "rb");

    if (fp == nullptr) {
        LOGD("open file failed");
        return 0;
    }

    int row_stride;
    JSAMPARRAY buffer;
    struct jpeg_decompress_struct cinfo;
    struct jpeg_error_mgr jerr;

    cinfo.err = jpeg_std_error(&jerr);

    jpeg_create_decompress(&cinfo);


    jpeg_stdio_src(&cinfo, fp);
    jpeg_read_header(&cinfo, TRUE);

    LOGD("width is %d height is %d", cinfo.image_width, cinfo.image_height);

    jpeg_start_decompress(&cinfo);
    unsigned long width = cinfo.output_width;
    unsigned long height = cinfo.output_height;
    unsigned short depth = cinfo.output_components;
    row_stride = cinfo.output_width * cinfo.output_components;

    buffer = (*cinfo.mem->alloc_sarray)((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);

   // unsigned char *src_buff;
    *rgb_buffer = static_cast<unsigned char *>(malloc(width * height * depth));
    memset(*rgb_buffer, 0, sizeof(unsigned char) * width * height * depth);

    unsigned char *point = *rgb_buffer;

    while (cinfo.output_scanline < height) {
        jpeg_read_scanlines(&cinfo, buffer, 1);
        memcpy(point, *buffer, width * depth);
        point += width * depth;
    }

    jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);

    //free(src_buff);
    fclose(fp);

    return 1;
}


int JpegHelper::compressJpeg2Jpeg(const char *filePath, const char *out_filePath, int quality, int width,int height) {

    int nComponent = 4;

    //setup1:解压
    struct jpeg_decompress_struct cinfo;
    struct jpeg_error_mgr jerr;

    JSAMPARRAY buffer;
    int row_stride = 0;
    unsigned char *tmp_buffer = nullptr;

    FILE *inFile = fopen(filePath, "rb");
    if (inFile == nullptr) {
        LOGD("open file failed");
        return 0;
    }

    cinfo.err = jpeg_std_error(&jerr);
    jpeg_create_decompress(&cinfo);

    jpeg_stdio_src(&cinfo, inFile);
    jpeg_read_header(&cinfo, TRUE);
    cinfo.out_color_components = nComponent;
    cinfo.out_color_space = JCS_EXT_RGBA;

    jpeg_start_decompress(&cinfo);
    row_stride = cinfo.image_width << 2;

    int file_width = cinfo.image_width;
    int file_height = cinfo.image_height;

    int size = row_stride * cinfo.output_height;

    buffer = (*cinfo.mem->alloc_sarray)((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);

    JSAMPLE *rgb_buffer;
    rgb_buffer = (unsigned char *) malloc(sizeof(char) * size);    // 分配总内存
    tmp_buffer = rgb_buffer;


    while (cinfo.output_scanline < cinfo.output_height) // 解压每一行
    {
        jpeg_read_scanlines(&cinfo, buffer, 1);
        // 复制到内存
        memcpy(tmp_buffer, buffer[0], row_stride);
        tmp_buffer += row_stride;
    }

    jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);
    fclose(inFile);

    //-------------------------

    //setup2:压缩
    struct jpeg_compress_struct cinfo2;
    struct jpeg_error_mgr jerr2;

    JSAMPROW row_pointer[1];
    cinfo2.err = jpeg_std_error(&jerr2);

    /*压缩初始化*/
    jpeg_create_compress(&cinfo2);

    FILE *outfile = fopen(out_filePath, "wb");
    if (outfile == nullptr) {
        LOGD("open file failed");
        return 0;
    }

    jpeg_stdio_dest(&cinfo2, outfile);
    /*设置压缩各项图片参数*/
    int compress_width;
    int compress_height;

    if (width==0){
        compress_width = file_width;
    } else{
        compress_width = width;
    }

    if (height==0){
        compress_height = file_height;
    } else{
        compress_height = height;
    }

    cinfo2.image_width = compress_width;
    cinfo2.image_height = compress_height;
    cinfo2.input_components = nComponent;
    cinfo2.in_color_space = JCS_EXT_RGBA;

    jpeg_set_defaults(&cinfo2);
    jpeg_set_quality(&cinfo2, quality, TRUE);

    /*开始压缩*/
    jpeg_start_compress(&cinfo2, TRUE);
    /*逐行扫描压缩写入文件*/
    row_stride = compress_width * nComponent;
    while (cinfo2.next_scanline < cinfo2.image_height) {
        row_pointer[0] = &rgb_buffer[cinfo2.next_scanline * row_stride];
        jpeg_write_scanlines(&cinfo2, row_pointer, 1);
    }
    /*完成压缩*/
    jpeg_finish_compress(&cinfo2);
    jpeg_destroy_compress(&cinfo2);
    fclose(outfile);

    free(rgb_buffer);
    return 1;
}


/**
 * bitmap to file 测试ok
 * @param data
 * @param w
 * @param h
 * @param quality
 * @param outfilename
 * @return
 */
int JpegHelper::GenerateBitmap2Jpeg(BYTE *data, int w, int h, int quality, const char *outfilename) {

    struct jpeg_compress_struct jcs;
    struct my_error_mgr jem;

    int nComponent = 3;
    jcs.err = jpeg_std_error(&jem.pub);
    jem.pub.error_exit = my_error_exit;
    if (setjmp(jem.setjmp_buffer)) {
        return 0;
    }
    jpeg_create_compress(&jcs);

    FILE* f = fopen(outfilename, "wb");
    if (f == nullptr) {
        LOGD("file open failed");
        return 0;
    }

    jpeg_stdio_dest(&jcs, f);
    jcs.image_width = w;
    jcs.image_height = h;
    jcs.arith_code = false;
    jcs.input_components = nComponent; // 在此为1,表示灰度图， 如果是彩色位图，则为4
    jcs.in_color_space = JCS_RGB; //JCS_GRAYSCALE表示灰度图，JCS_RGB表示彩色图像
    jpeg_set_defaults(&jcs);
    jcs.optimize_coding = false;
    jpeg_set_quality(&jcs, quality, true);
    jpeg_start_compress(&jcs, TRUE);

    JSAMPROW row_pointer[1];
    int row_stride;
    row_stride = jcs.image_width * nComponent;
    while (jcs.next_scanline < jcs.image_height) {
        // 逐行读取像素内容
        row_pointer[0] = &data[jcs.next_scanline * row_stride];
        // 写入数据
        jpeg_write_scanlines(&jcs, row_pointer, 1);
    }

    jpeg_finish_compress(&jcs);
    jpeg_destroy_compress(&jcs);
    fclose(f);
    return 1;
}

int JpegHelper::GenerateBitmap2Buffer(BYTE *data, int w, int h, int quality, JSAMPLE **rgb_buffer) {

    struct jpeg_compress_struct jcs;
    struct my_error_mgr jem;

    int nComponent = 3;
    unsigned char *rgba; //输出buffer
    unsigned long size;  //buffer size


    jcs.err = jpeg_std_error(&jem.pub);
    jem.pub.error_exit = my_error_exit;

    if (setjmp(jem.setjmp_buffer)) {
        LOGD("setjmp failed");
        return 0;
    }

    jpeg_create_compress(&jcs);
    jpeg_mem_dest(&jcs, &rgba, &size);

    jcs.image_width = w;
    jcs.image_height = h;
    jcs.arith_code = false;
    jcs.input_components = nComponent;
    jcs.in_color_space = JCS_RGB;


    jpeg_set_defaults(&jcs);
    jcs.optimize_coding = true;
    jpeg_set_quality(&jcs, quality, true);
    jpeg_start_compress(&jcs, TRUE);

    JSAMPROW row_pointer[1];
    int row_stride = jcs.image_width * nComponent;

    *rgb_buffer = (unsigned char *) malloc(sizeof(char) * size);
    rgba = *rgb_buffer;

    while (jcs.next_scanline < jcs.image_height) {
        row_pointer[0] = &data[jcs.next_scanline * row_stride];
        jpeg_write_scanlines(&jcs, row_pointer, 1);
    }

    jpeg_finish_compress(&jcs);
    jpeg_destroy_compress(&jcs);

    return 1;
}

int JpegHelper::write_jpeg_file(const char *filename, int image_height, int image_width, int quality, JSAMPLE *image_buffer) {

    struct jpeg_compress_struct cinfo;
    struct jpeg_error_mgr jerr;

    int nComponent = 4;
    int row_stride;
    JSAMPROW row_pointer[1];

    cinfo.err = jpeg_std_error(&jerr);
    /*压缩初始化*/
    jpeg_create_compress(&cinfo);

    FILE *outfile = fopen(filename, "wb");
    if (outfile == nullptr) {
        LOGD("open file failed");
        return 0;
    }

    jpeg_stdio_dest(&cinfo, outfile);
    /*设置压缩各项图片参数*/
    cinfo.image_width = image_width;
    cinfo.image_height = image_height;
    cinfo.input_components = nComponent;
    cinfo.in_color_space = JCS_EXT_RGBA;

    jpeg_set_defaults(&cinfo);
    jpeg_set_quality(&cinfo, quality, TRUE);
    /*开始压缩*/
    jpeg_start_compress(&cinfo, TRUE);
    /*逐行扫描压缩写入文件*/
    row_stride = image_width * nComponent;
    while (cinfo.next_scanline < cinfo.image_height) {
        row_pointer[0] = &image_buffer[cinfo.next_scanline * row_stride];
        jpeg_write_scanlines(&cinfo, row_pointer, 1);
    }
    /*完成压缩*/
    jpeg_finish_compress(&cinfo);
    jpeg_destroy_compress(&cinfo);

    fclose(outfile);
    return 1;
}

int JpegHelper::bitmap2Byte(JNIEnv *env, jobject bitmap, jbyteArray *output, int w, int h, int quality) {

    AndroidBitmapInfo bitmapInfo;
    int errorCode = 0;
    if ((errorCode = AndroidBitmap_getInfo(env, reinterpret_cast<jobject>(bitmap), &bitmapInfo)) !=
        ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("get bitmapInfo failed, code is %d", errorCode);
        return 0;
    }
    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("bitmap format is not supported");
        return 0;
    }
    unsigned char *pixelData;
    AndroidBitmap_lockPixels(env, bitmap, reinterpret_cast<void **>(&pixelData));

    int width;
    int height;
    if (0 == w){
        width = bitmapInfo.width;
    } else{
        width = w;
    }

    if (0 == h){
        height = bitmapInfo.height;
    } else{
        height = h;
    }

    initHandle();

    unsigned char *jpegBuff;
    unsigned long jpegSize = 0;
    LOGI("compressRgba8888ToJpeg start");
    int compressResult = compressRgba8888ToJpeg(pixelData,width,height,&jpegBuff,&jpegSize,quality);
    LOGI("compressRgba8888ToJpeg finished");
    AndroidBitmap_unlockPixels(env, bitmap);

    if (compressResult != 0) {
        LOGE("compress failed , errorCode is %d", compressResult);
        return 0;
    } else {
        *output = env->NewByteArray(jpegSize);
        env->SetByteArrayRegion(*output, 0, jpegSize, reinterpret_cast<const jbyte *>(jpegBuff));
        LOGE("compress success , jpegSize  %d", jpegSize);
        free(jpegBuff);
        return 1;
    }
}

int JpegHelper::initHandle() {
    if (handle == nullptr){
        handle = tjInitCompress();
    }
    return handle == nullptr ? JNI_FALSE : JNI_TRUE;
}

int JpegHelper::destroyHandle() {
    if (handle == nullptr) {
        return -1;
    }
    int code = 0;
    if ((code = tjDestroy(handle)) == 0) {
        handle = nullptr;
        return JNI_TRUE;
    } else {
        return code;
    }
}

int JpegHelper::compressRgba8888ToJpeg(
        const unsigned char *srcBuf,
        int width,
        int height,
        unsigned char **jpegBuf,
        unsigned long *jpegSize,
        int quality){

    int subsamp = TJSAMP_420;
    int pixelFormat = TJPF_RGBA;

    return tjCompress2(handle, srcBuf, width, width << 2, height, pixelFormat, jpegBuf, jpegSize, subsamp, quality, 0);
}

int JpegHelper::compressI420ToJpeg(const unsigned char *srcBuf, int width, int height,unsigned char **jpegBuf, unsigned long *jpegSize, int quality) {

    const unsigned char *srcPlanes[3];
    int strides[3];
    srcPlanes[0] = srcBuf;
    srcPlanes[1] = srcBuf + width * height;
    srcPlanes[2] = srcBuf + width * height + width * height / 4;
    strides[0] = width;
    strides[1] = width / 2;
    strides[2] = width / 2 ;

    int subsamp = TJSAMP_420;

    return tjCompressFromYUVPlanes(handle, srcPlanes, width, strides, height, subsamp, jpegBuf,jpegSize, quality, 0);
}

int JpegHelper::compressByteToByte(const unsigned char *srcBuf, int width, int height,
                                   unsigned char **jpegBuf, unsigned long *jpegSize, int quality) {
    int subsamp = TJSAMP_420;
    int pixelFormat = TJPF_RGBA;

    return tjCompress2(handle, srcBuf, width, width << 2, height, pixelFormat, jpegBuf, jpegSize, subsamp, quality, 0);
}
