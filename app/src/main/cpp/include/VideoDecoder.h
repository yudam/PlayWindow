//
// Created by 毛大宇 on 2023/4/23.
//

#ifndef PLAYWINDOW_VIDEODECODER_H
#define PLAYWINDOW_VIDEODECODER_H

#include <iostream>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>

extern "C" {
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
#include "include/libswscale/swscale.h"
#include "include/libavutil/imgutils.h"
};


class VideoDecoder {

public:
    void init(char* media_file,JNIEnv * jniEnv,jobject object);

    void startDecoder();

    void onFrameAvailable(AVFrame *avFrame);

    void release();

private:

    char *filename = nullptr;
    int64_t stream_index = -1;
    int m_videoWidth = 0;
    int m_videoHeight = 0;

    AVFormatContext *avFormatContext = nullptr;
    AVCodecContext *avCodecContext = nullptr;
    AVPacket *avPacket = nullptr;
    AVFrame *avFrame = nullptr;
    SwsContext *swsContext = nullptr;
    AVFrame *rgbFrame = nullptr;
    uint8_t *frameBuffer = nullptr;


    ANativeWindow * aNativeWindow = nullptr;

    JNIEnv *jni_env;
    jobject java_surface;
};


#endif //PLAYWINDOW_VIDEODECODER_H
