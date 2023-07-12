//
// Created by 毛大宇 on 2023/6/30.
//

#include "aac_decoder.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"aac_decoder",__VA_ARGS__)

void AacDecoder::prepareAudio(char *audio_path) {
    this->in_audio_path = audio_path;
}


void AacDecoder::startDecoder() {
    av_register_all();
    int ret = avformat_open_input(&in_avformat_context, in_audio_path, nullptr, nullptr);
    if (ret < 0) {
        LOGI("avformat_open_input failed ,  path : %s",in_audio_path);
        return;
    }

    avformat_find_stream_info(in_avformat_context, nullptr);

}

void AacDecoder::stopDecoder() {

}