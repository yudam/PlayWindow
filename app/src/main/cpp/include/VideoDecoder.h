//
// Created by 毛大宇 on 2023/4/23.
//

#ifndef PLAYWINDOW_VIDEODECODER_H
#define PLAYWINDOW_VIDEODECODER_H

#include "iostream"

extern "C" {
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
};


class VideoDecoder {


private:

    void decoderMethod();
};


#endif //PLAYWINDOW_VIDEODECODER_H
