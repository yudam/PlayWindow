//
// Created by 毛大宇 on 2023/4/27.
//

#ifndef PLAYWINDOW_LOCALRTMPPUSH_H
#define PLAYWINDOW_LOCALRTMPPUSH_H

#include <iostream>
#include <string>
#include <android/log.h>

extern "C" {
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
#include "include/libavutil/time.h"
};

class LocalRtmpPush {

private:
    AVFormatContext *in_avf_context;
    AVFormatContext *out_avf_context;

    int video_index;
    int audio_index;

public:
    int open(const char *infilename, const char *outfilename);

    void push();

    void close();
};


#endif //PLAYWINDOW_LOCALRTMPPUSH_H
