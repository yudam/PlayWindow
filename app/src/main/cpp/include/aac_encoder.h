//
// Created by 毛大宇 on 2023/5/12.
//

#ifndef PLAYWINDOW_AAC_ENCODER_H
#define PLAYWINDOW_AAC_ENCODER_H

#include <iostream>
#include <string>
#include <android/log.h>

extern "C" {
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
#include "include/libswresample/swresample.h"
#include "include/libavutil/opt.h"
#include "include/libavutil/log.h"
#include "include/libavutil/time.h"

};

class AACEncoder {

public:

    void startAudio(const char *url);

    void setFrameData(const uint8_t *data,int len);

    void stopAudio();

private:

    AVFormatContext *avFormatContext = nullptr;
    AVCodecContext *avCodecContext = nullptr;
    AVStream *audiostream = nullptr;
    AVPacket *avPacket = nullptr;
    AVFrame *avFrame = nullptr;
    SwrContext *swr = nullptr;

    void initSwrConfig();
    void encoderAudio(AVCodecContext *codecContext,AVFrame *audioFrame);
};


#endif //PLAYWINDOW_AAC_ENCODER_H
