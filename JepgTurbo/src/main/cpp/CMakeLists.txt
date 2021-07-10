# 设定所要求能运行的最低版本
cmake_minimum_required(VERSION 3.4.1)


include_directories(#将头文件所在目录告诉编译器，在里面添加
        ${CMAKE_SOURCE_DIR}/../cpp/include
        )


add_library( # Sets the name of the library.
        jepg_compress
        SHARED

        jpeghelper/JpegHelper.h
        jpeghelper/JpegHelper.cpp
        )


add_library(jpeg SHARED IMPORTED)
set_target_properties(jpeg PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libjpeg.so)

add_library(jpegturbo SHARED IMPORTED)
set_target_properties(jpegturbo PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libturbojpeg.so)


find_library( # Sets the name of the path variable.
              log-lib
              log )


target_link_libraries( # Specifies the target library.
        jepg_compress

        jpeg
        jpegturbo

        android
        jnigraphics
        ${log-lib} )