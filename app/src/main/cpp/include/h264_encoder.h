//
// Created by 毛大宇 on 2023/4/23.
//

#ifndef PLAYWINDOW_H264_ENCODER_H
#define PLAYWINDOW_H264_ENCODER_H

#include "iostream"
#include "android/log.h"


extern "C" {
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
#include "include/libavutil/imgutils.h"
#include "include/libavutil/log.h"
#include "include/libavutil/time.h"
#include "libyuv.h"
};

class H264Encoder {

public:

    void convertToI420(uint8_t *nv21Buffer);

    void startPublish(const char* url,int width,int height);

    void encoderBuffer(uint8_t * nv21Buffer,int len);

    void stopPublish();

private:

    int width;
    int height;
    uint8_t *out_buffer;
    int index = 0;
    int fps = 30;

    const char *rtmpUrl;

    int start_time;

    bool isInit = false;

    AVFormatContext *avFormatContext = nullptr;
    AVCodecContext *avCodecContext = nullptr;
    AVStream *avStream = nullptr;

    AVFrame *avFrame = nullptr;
    AVPacket avPacket;

    void encoderMethod();
};


#endif //PLAYWINDOW_H264_ENCODER_H
