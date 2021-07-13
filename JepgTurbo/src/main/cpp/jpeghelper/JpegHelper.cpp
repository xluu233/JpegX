
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



int JpegHelper::read_jpeg_file(const char *jpeg_file, unsigned char **rgb_buffer, int *size, int *width,int *height) {
    struct jpeg_decompress_struct cinfo;
    FILE *fp;

    JSAMPARRAY buffer;

    uint8_t *pixels = nullptr;
    JSAMPROW *row_pointer = nullptr;

    LOGD("start read jpeg file path is %s", jpeg_file);

    int row_stride = 0;

    unsigned char *tmp_buffer = nullptr;
    int rgb_size;

    fp = fopen(jpeg_file, "rb");

    if (fp == nullptr) {
        LOGD("open file failed");
        return 0;
    }

//    cinfo.err = jpeg_std_error(&jerr.pub);
//    jerr.pub.error_exit = my_error_exit;

//    if (setjmp(jerr.setjmp_buffer))
//    {
//        jpeg_destroy_decompress(&cinfo);
//        fclose(fp);
//        return -1;
//    }

    struct jpeg_error_mgr jerr;

    cinfo.err = jpeg_std_error(&jerr);

    jpeg_create_decompress(&cinfo);

    jpeg_stdio_src(&cinfo, fp);

    jpeg_read_header(&cinfo, TRUE);

    //cinfo.out_color_space = JCS_RGB; //JCS_YCbCr;  // 设置输出格式

    cinfo.out_color_components = 4;
    cinfo.out_color_space = JCS_EXT_RGBA;

    jpeg_start_decompress(&cinfo);

    row_stride = cinfo.image_width << 2;

    *width = cinfo.image_width;
    *height = cinfo.image_height;

//
//    pixels = static_cast<uint8_t *>(malloc(cinfo.image_width * cinfo.image_height * 4));
//
//    row_pointer = static_cast<JSAMPROW *>(malloc(sizeof(JSAMPROW) * cinfo.output_height));
//
//    for (int i = 0; i < cinfo.output_height; ++i) {
//        row_pointer[i] = &pixels[i * row_stride];
//    }
//
//    while (cinfo.output_scanline < cinfo.output_height) {
//        jpeg_read_scanlines(&cinfo, &row_pointer[cinfo.output_scanline],
//                            cinfo.output_height - cinfo.output_scanline);
//    }
//
//    *rgb_buffer = pixels;
//
//    *size = row_stride * cinfo.output_height;


    rgb_size = row_stride * cinfo.output_height; // 总大小

    *size = rgb_size;

    buffer = (*cinfo.mem->alloc_sarray)((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);

    *rgb_buffer = (unsigned char *) malloc(sizeof(char) * rgb_size);    // 分配总内存


    LOGD("debug--:\nrgb_size: %d, size: %d w: %d h: %d row_stride: %d \n", rgb_size,
           cinfo.image_width * cinfo.image_height * 3,
           cinfo.image_width,
           cinfo.image_height,
           row_stride);

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

    return 0;
}

int JpegHelper::write_jpeg_file(unsigned char *image_buffer, int quality, int image_height,int image_width) {
    struct jpeg_compress_struct cinfo;
    struct jpeg_error_mgr jerr;
    JSAMPROW row_pointer[1];

    cinfo.err = jpeg_std_error(&jerr);
    /*压缩初始化*/
    jpeg_create_compress(&cinfo);
    /*创建并打开输出的图片文件*/
    FILE* f = fopen("/storage/emulated/0/Android/data/com.siliconmotion.usbdisplay/cache/compress22.jpg", "wb"); ////打开（建立）一个可写的文件
    if (f == NULL) {
        LOGD("set jsc2");
        return 0;
    }

    jpeg_stdio_dest(&cinfo, f);
    /*设置压缩各项图片参数*/
    cinfo.image_width = image_width;
    cinfo.image_height = image_height;
    cinfo.input_components = 3;
    cinfo.in_color_space = JCS_RGB;
    jpeg_set_defaults(&cinfo);
    /*设置压缩质量*/
    jpeg_set_quality(&cinfo, quality, TRUE );
    /*开始压缩*/
    jpeg_start_compress(&cinfo, TRUE);
    /*逐行扫描压缩写入文件*/
    int row_stride = image_width * 3;
    while (cinfo.next_scanline < cinfo.image_height) {
        row_pointer[0] = & image_buffer[(cinfo.image_height-cinfo.next_scanline) * row_stride];
        jpeg_write_scanlines(&cinfo, row_pointer, 1);
    }
    /*完成压缩*/
    jpeg_finish_compress(&cinfo);
    jpeg_destroy_compress(&cinfo);
    fclose(f);
    /*释放存储的解压图像内容*/
    //free(image_buffer);
    return 0;
}

int JpegHelper::GenerateBitmap2Jpeg(BYTE *data, int w, int h, int quality, const char *outfilename) {

    int nComponent = 3;
    struct jpeg_compress_struct jcs;
    struct my_error_mgr jem;

    jcs.err = jpeg_std_error(&jem.pub);
    jem.pub.error_exit = my_error_exit;
    if (setjmp(jem.setjmp_buffer)) {
        return 0;
    }
    jpeg_create_compress(&jcs);

    FILE* f = fopen(outfilename, "wb");
    if (f == NULL) {
        LOGD("file open failed");
        return 0;
    }

    LOGD("set jsc");
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

int JpegHelper::GenerateBitmap2Buffer(BYTE *data, int w, int h, int quality, const char *outfilename) {
    LOGD("location is %s",outfilename);
    int nComponent = 3;
    struct jpeg_compress_struct jcs;
    struct my_error_mgr jem;

    unsigned char *rgba; //输出buffer
    unsigned long size;  //buffer size
    std::vector<unsigned char> buffer;


    jcs.err = jpeg_std_error(&jem.pub);
    jem.pub.error_exit = my_error_exit;
    if (setjmp(jem.setjmp_buffer)) {
        LOGD("return 0");
        return 0;
    }

    LOGD("start set jsc");
    jpeg_create_compress(&jcs);
    jpeg_mem_dest(&jcs, &rgba, &size);


    LOGD("test1");
    jcs.image_width = w;
    jcs.image_height = h;
    jcs.arith_code = false;
    jcs.input_components = nComponent;
    jcs.in_color_space = JCS_RGB;

    LOGD("test2");
    jpeg_set_defaults(&jcs);
    LOGD("test3");
    jcs.optimize_coding = true;
    LOGD("test4");
    jpeg_set_quality(&jcs, quality, true);
    LOGD("test5");
    jpeg_start_compress(&jcs, TRUE);
    LOGD("test6");
    JSAMPROW row_pointer[1];
    LOGD("test7");
    int row_stride;
    LOGD("test8");
    row_stride = jcs.image_width * nComponent;
    LOGD("test9");
    while (jcs.next_scanline < jcs.image_height) {
        row_pointer[0] = &data[jcs.next_scanline * row_stride];
        jpeg_write_scanlines(&jcs, row_pointer, 1);
    }
    LOGD("test10");

    if (jcs.optimize_coding) {
        LOGD("optimize==ture");
    } else {
        LOGD("optimize==false");
    }

    jpeg_finish_compress(&jcs);
    jpeg_destroy_compress(&jcs);

    //buffer的操作必须在struct destory之后。

    if(size > 0)
    {
        buffer.resize(size);
        for(int i=0;i<size;i++){
            buffer[i] = rgba[i];
        }

    }

    //LOGD("rgb_size = %lu",rgba);
    //LOGD("buffer_size = %lu",buffer.size());

    //buffer_size = 232883
    //227.42 kb

    //long start = getCurrentTime();
    write_jpeg_file(rgba,60,h,w);
    //long end = getCurrentTime();
    //long delta = end - start;
    //LOGD("222delta time is %ld ms ",delta);
    //28 ms
    //25 ms
    //24 ms


    free(rgba);
    //free(outbuffer);
    return 1;
}

