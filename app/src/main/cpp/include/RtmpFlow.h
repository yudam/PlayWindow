//
// Created by 毛大宇 on 2023/4/24.
//

#ifndef PLAYWINDOW_RTMPFLOW_H
#define PLAYWINDOW_RTMPFLOW_H

#include <iostream>

extern "C" {
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
#include "include/libavutil/log.h"
#include "include/libavutil/time.h"
};

class RtmpFlow {

public:
    AVFormatContext *avfCtx = nullptr;
    char *publicUrl = nullptr;
    int videoBitRate;
    int frameRate;
    int width;
    int height;
    int sampleRate;
    int channel;
    int audioBitRate;

    AVStream *video_st = nullptr;
    AVFrame * avFrame = nullptr;

    void init(char *url, int videoBitRate, int frameRate, int width, int height, int sampleRate, int channel, int audioBitTare);

    void connect();

    void newStream(bool isVideo);

    int sendPacket(uint8_t * data,long pts,bool isCsd);

    void release();

private:
    void rtmpMethod();


};


#endif //PLAYWINDOW_RTMPFLOW_H
