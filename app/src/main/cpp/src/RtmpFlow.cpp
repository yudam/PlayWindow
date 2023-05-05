//
// Created by 毛大宇 on 2023/4/24.
//

#include "RtmpFlow.h"

void RtmpFlow::rtmpMethod() {
}

void RtmpFlow::init(char *url, int videoBitRate, int frameRate, int width, int height, int sampleRate, int channel, int audioBitRare) {
    this->publicUrl = url;
    this->videoBitRate = videoBitRate;
    this->frameRate = frameRate;
    this->width = width;
    this->height = height;
    this->sampleRate = sampleRate;
    this->channel = channel;
    this->audioBitRate = audioBitRare;
    av_register_all();
    avformat_network_init();
    avformat_alloc_output_context2(&avfCtx, nullptr, "flv", "");
    newStream(true);
    //newStream(false);
    av_dump_format(avfCtx, 0, publicUrl, 0);
}


/**
 * 打开网络输出流
 * avio_open2 :带回调监听的方法
 */
void RtmpFlow::connect() {
    if (avfCtx->flags & AVFMT_NOFILE) {
        if (avio_open(&avfCtx->pb, publicUrl, AVIO_FLAG_READ_WRITE) < 0) {
            return;
        }
    }
}


/**
 * 创建一个流通道
 */
void RtmpFlow::newStream(bool isVideo) {
    /**
     * 1. 设置AVOutputFormat中的音视频格式
     */
    if (isVideo) {
        avfCtx->oformat->video_codec = AV_CODEC_ID_H264;
    } else {
        avfCtx->oformat->audio_codec = AV_CODEC_ID_AAC;
    }

    /**
     * 2. 根据id找到 AVCodec，内部参数都是默认值
     */
    AVCodec *avcodec = avcodec_find_encoder(isVideo ? AV_CODEC_ID_H264 : AV_CODEC_ID_AAC);
    if (!avcodec) {
        return;
    }

    /**
     * 3. 根据AVCodec创建 AVCodecContext,也就是编码器的上下文，保存了编码的信息如比特率，宽高，gop等
     */
    AVCodecContext *avCodecContext = avcodec_alloc_context3(avcodec);
    if (!avCodecContext) {
        return;
    }
    avCodecContext->codec_id = avcodec->id;
    if (isVideo) {
        avCodecContext->codec_type = AVMEDIA_TYPE_VIDEO;
        // 视频编码设置比特率，帧率，宽、高、时基、gop也叫I帧间隔、像素格式等
        avCodecContext->bit_rate = videoBitRate;
        avCodecContext->framerate = AVRational{frameRate, 1};
        avCodecContext->time_base = AVRational{1, frameRate};
        avCodecContext->width = width;
        avCodecContext->height = height;
        avCodecContext->gop_size = 10;
        avCodecContext->max_b_frames = 0;
        avCodecContext->qmin = 10;
        avCodecContext->qmax = 50;
        avCodecContext->level = 41;
        avCodecContext->refs = 1;
        avCodecContext->qcompress = 0.6;
        avCodecContext->pix_fmt = AV_PIX_FMT_YUV420P;
    } else {
        avCodecContext->codec_type = AVMEDIA_TYPE_AUDIO;
        // 音频编码设置比特率、通道、通道类型、采样率、采样深度等
        avCodecContext->bit_rate = audioBitRate;
        avCodecContext->channel_layout = channel == 1 ? AV_CH_LAYOUT_MONO : AV_CH_LAYOUT_STEREO;
        avCodecContext->channels = channel;
        avCodecContext->sample_rate = sampleRate;
        avCodecContext->sample_fmt = AV_SAMPLE_FMT_S16;
    }

    if (avfCtx->oformat->flags & AVFMT_GLOBALHEADER) {
        avCodecContext->flags |= CODEC_FLAG_GLOBAL_HEADER;
    }

    AVDictionary *opts = nullptr;
    if (avCodecContext->codec_id == AV_CODEC_ID_H264) {
        av_dict_set(&opts, "preset", "superfast", 0);
        av_dict_set(&opts, "tune", "zerolatency", 0);
    }

    /**
     * 4. 打开编码器
     */
    if (avcodec_open2(avCodecContext, avcodec, &opts) < 0) {
        return;
    }


    /**
     * 5. 创建AVStream，也就是流通道，用于记录通道信息
     */
    AVStream *avStream = avformat_new_stream(avfCtx, avcodec);
    if (!avStream) {
        return;
    }
    // 设置时基
    avStream->time_base.num = 1;
    avStream->time_base.den = frameRate;
    avStream->codecpar->codec_tag = 0;
    // 复制context参数到coderpar
    if (avcodec_parameters_from_context(avStream->codecpar, avCodecContext) < 0) {
        return;
    }
}

/**
 * 发送AVPacket
 */
int RtmpFlow::sendPacket(uint8_t *data, long pts, bool isCsd) {



}

void RtmpFlow::release() {

}