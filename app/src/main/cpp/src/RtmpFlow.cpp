//
// Created by 毛大宇 on 2023/4/24.
//

#include "RtmpFlow.h"
#include <jni.h>

#define logi(...) __android_log_print(ANDROID_LOG_INFO,"RtmpFlow",__VA_ARGS__)

//#define LOG_BUF_PREFIX_SIZE 512
//#define LOG_BUF_SIZE 1024
//char libffmpeg_log_buf_prefix[LOG_BUF_PREFIX_SIZE];
//char libffmpeg_log_buf[LOG_BUF_SIZE];
//
//
///**
// * 设置ffmpeg的日志回调，用于打印输出
// * 这里设置的日志级别是AV_LOG_ERROR，也就是错误日志的输出
// */
//static void yunxi_ffmpeg_log_callback(void *ptr, int level, const char *fmt, va_list vl) {
//    int cnt;
//    memset(libffmpeg_log_buf_prefix, 0, LOG_BUF_PREFIX_SIZE);
//    memset(libffmpeg_log_buf, 0, LOG_BUF_SIZE);
//
//    cnt = snprintf(libffmpeg_log_buf_prefix, LOG_BUF_PREFIX_SIZE, "%s", fmt);
//    cnt = vsnprintf(libffmpeg_log_buf, LOG_BUF_SIZE, libffmpeg_log_buf_prefix, vl);
//
//    if (level == AV_LOG_ERROR) {
//        logi("%s", libffmpeg_log_buf);
//    }
//    return;
//}


void RtmpFlow::init(char *url, int videoBitRate, int frameRate, int width, int height) {
    this->publicUrl = url;
    this->videoBitRate = videoBitRate;
    this->frameRate = frameRate;
    this->width = width;
    this->height = height;
    av_register_all();
    avformat_network_init();
//    av_log_set_level(AV_LOG_ERROR);
//    av_log_set_flags(AV_LOG_SKIP_REPEATED);
//    av_log_set_callback(yunxi_ffmpeg_log_callback);


    avformat_alloc_output_context2(&avfCtx, nullptr, "flv", "");
    newStream(true);
    avPacket = av_packet_alloc();
}


/**
 * 打开网络输出流
 * avio_open2 :带回调监听的方法
 */
void RtmpFlow::connect() {
    if (avfCtx->flags & AVFMT_NOFILE) {
        if (avio_open(&avfCtx->pb, publicUrl, AVIO_FLAG_READ_WRITE) < 0) {
            logi(" avio_open failed");
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
        logi("avcodec_find_encoder failed");
        return;
    }

    /**
     * 3. 根据AVCodec创建 AVCodecContext,也就是编码器的上下文，保存了编码的信息如比特率，宽高，gop等
     */
    avCodecContext = avcodec_alloc_context3(avcodec);
    if (!avCodecContext) {
        logi("avcodec_alloc_context3 failed");
        return;
    }
    avCodecContext->codec_id = avcodec->id;
    if (isVideo) {
        avCodecContext->codec_type = AVMEDIA_TYPE_VIDEO;
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
//        avCodecContext->codec_type = AVMEDIA_TYPE_AUDIO;
//        // 音频编码设置比特率、通道、通道类型、采样率、采样深度等
//        avCodecContext->bit_rate = audioBitRate;
//        avCodecContext->channel_layout = channel == 1 ? AV_CH_LAYOUT_MONO : AV_CH_LAYOUT_STEREO;
//        avCodecContext->channels = channel;
//        avCodecContext->sample_rate = sampleRate;
//        avCodecContext->sample_fmt = AV_SAMPLE_FMT_S16;
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
        logi("avcodec_open2 failed");
        return;
    }


    /**
     * 5. 创建AVStream，也就是流通道，用于记录通道信息
     */
    video_st = avformat_new_stream(avfCtx, avcodec);
    video_st->time_base = AVRational{1, frameRate};
    video_st->codecpar->codec_tag = 0;
    int ret = avcodec_parameters_from_context(video_st->codecpar, avCodecContext);
    if (ret < 0) {
        logi(" avcodec_parameters_from_context failed");
        return;
    }
}


int RtmpFlow::sendMediaPacket(MediaPacket *packet) {
    logi("sendMediaPacket    0");
    if (startTime == 0) {
        startTime = packet->pts;
    }
    if (packet->isCsd) {
        logi(" sendVideoHeader ");
        sendVideoHeader((uint8_t *) packet->csd_0, (uint8_t *) packet->csd_1, packet->csd0Size, packet->csd1Size);
    } else {
        sendVideoPacket((uint8_t *) packet->buffer, packet->bufferSize, packet->pts);
    }
}

/**
 * H.264的码流采用的是AVCC格式,所以在AVStream的codepar的extradata中构造sps和pps数组
 */
int RtmpFlow::sendVideoHeader(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {

    int spsLen = 0, ppsLen = 0;
    if (sps[0] == 0x00 && sps[1] == 0x00 && sps[2] == 0x00 && sps[3] == 0x01) {
        spsLen = sps_len - 4;
    } else if (sps[0] == 0x00 && sps[1] == 0x00 && sps[2] == 0x01) {
        spsLen = sps_len - 3;
    }
    if (pps[0] == 0x00 && pps[1] == 0x00 && pps[2] == 0x00 && pps[3] == 0x01) {
        ppsLen = pps_len - 4;
    } else if (pps[0] == 0x00 && pps[1] == 0x00 && pps[2] == 0x01) {
        ppsLen = pps_len - 3;
    }

    if (spsLen == 0 || ppsLen == 0) {
        return -1;
    }
    uint8_t spsBuffer[spsLen];
    uint8_t ppsBuffer[ppsLen];

    for (int i = 0; i < spsLen; i++) {
        spsBuffer[i] = sps[i + sps_len - spsLen];
    }

    for (int i = 0; i < ppsLen; i++) {
        ppsBuffer[i] = pps[i + pps_len - ppsLen];
    }
    return putVideoHeader(spsBuffer, spsLen, ppsBuffer, ppsLen);
}


/**
 * 发送AVPacket
 */
int RtmpFlow::sendVideoPacket(uint8_t *data, int len, long pts) {
    logi("sendVideoPacket    0");
    if (data[0] == 0x00 && data[1] == 0x00 && data[2] == 0x00 && data[3] == 0x01) {
        data[0] = 0x00;
        data[1] = 0x00;
        data[2] = 0x00;
        data[3] = 0x00;
    } else if (data[0] == 0x00 && data[1] == 0x00 && data[2] == 0x01) {
        data[0] = 0x00;
        data[1] = 0x00;
        data[2] = 0x00;
    }

    avPacket->data = data;
    avPacket->stream_index = video_index;
    avPacket->pts = (pts - startTime) / 1000;
    avPacket->dts = avPacket->pts;
    avPacket->size = len;
    logi("  data size : %d,   pts: %d", avPacket->size, avPacket->pts);

    if (avfCtx == nullptr) {
        logi("what    ");
    }
    av_interleaved_write_frame(avfCtx, avPacket);
    logi("sendVideoPacket    1");
    av_packet_free(&avPacket);

}

int RtmpFlow::sendAudioPacket(uint8_t *data, int len, long pts) {


}


/**
 * 构造sps+pps
 */
int RtmpFlow::putVideoHeader(uint8_t *spsBuffer, int spsLen, uint8_t *ppsBuffer, int ppsLen) {

    AVCodecParameters *codecpar = video_st->codecpar;
    uint8_t *extradata = codecpar->extradata;

    extradata[0] = 0x01;
    extradata[1] = spsBuffer[1];
    extradata[2] = spsBuffer[2];
    extradata[3] = spsBuffer[3];
    extradata[4] = 0xFC | 3;
    extradata[5] = 0xE0 | 1;
    extradata[6] = (spsLen >> 8) & 0x00ff;
    extradata[7] = spsLen & 0x00ff;
    for (int i = 0; i < spsLen; i++) {
        extradata[8 + i] = spsBuffer[i];
    }
    extradata[8 + spsLen] = 0x01;
    extradata[8 + spsLen + 1] = (ppsLen >> 8) & 0x00ff;
    extradata[8 + spsLen + 2] = ppsLen & 0x00ff;
    for (int i = 0; i < ppsLen; i++) {
        extradata[8 + spsLen + 3 + i] = ppsBuffer[i];
    }

    int ret = avformat_write_header(avfCtx, NULL);
    if (ret < 0) {
        logi("avformat_write_header failed");
    }
    return ret;
}


int RtmpFlow::putAudioHeader(uint8_t *adtsBuffer, int adtsLen) {


}

void RtmpFlow::release() {

}

