//
// Created by 毛大宇 on 2023/4/24.
//

#ifndef PLAYWINDOW_RTMPFLOW_H
#define PLAYWINDOW_RTMPFLOW_H

#include <iostream>
#include <android/log.h>


extern "C" {
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
#include "include/libavutil/log.h"
#include "include/libavutil/time.h"
};


struct MediaPacket {
    void *buffer;
    void *csd_0;
    void *csd_1;
    int media_type;
    int64_t pts;
    int bufferSize;
    int csd0Size;
    int csd1Size;
    bool  isCsd;
};

class RtmpFlow {

public:
    AVFormatContext *avfCtx = nullptr;
    AVCodecContext *avCodecContext = nullptr;
    char *publicUrl = nullptr;
    int videoBitRate;
    int frameRate;
    int width;
    int height;

    AVStream *video_st = nullptr;
    AVFrame *avFrame = nullptr;

    void init(char *url, int videoBitRate, int frameRate, int width, int height);

    void connect();

    void newStream(bool isVideo);

    int sendMediaPacket(MediaPacket *packet);

    int sendVideoHeader(uint8_t *sps,uint8_t *pps,int sps_len,int pps_len);

    int sendVideoPacket(uint8_t *data, int len, long pts, bool isCsd);

    int sendAudioPacket(uint8_t *data, int len, long pts, bool isCsd);

    void release();

private:

    int video_index;
    int audio_index;
    int64_t startTime = 0;

    AVPacket *avPacket = nullptr;

};


#endif //PLAYWINDOW_RTMPFLOW_H
