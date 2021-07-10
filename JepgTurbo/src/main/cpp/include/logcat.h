#ifndef MOBILEVIDEOANALYSIS_ANDROID_LOG_H
#define MOBILEVIDEOANALYSIS_ANDROID_LOG_H

// 引入log头文件
#include  <android/log.h>
// log标签
#define  TAG    "--JNI-MUSE-TAG--"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
// 定义debug信息
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
// 定义error信息
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)


#endif
