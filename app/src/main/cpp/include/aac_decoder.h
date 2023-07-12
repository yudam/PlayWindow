//
// Created by 毛大宇 on 2023/6/30.
//

#include <iostream>
#include <android/log.h>
#include "media/NdkMediaCodec.h"

extern "C" {
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
}

#ifndef PLAYWINDOW_AAC_DECODER_H
#define PLAYWINDOW_AAC_DECODER_H


class AacDecoder {

private:
    AVFormatContext *in_avformat_context = nullptr;
    AVCodecContext *avCodecContext = nullptr;
    AVFrame *out_avFrame = nullptr;

    char *in_audio_path;

public:

    void prepareAudio(char* audio_path);

    void startDecoder();

    void stopDecoder();
};


#endif //PLAYWINDOW_AAC_DECODER_H
