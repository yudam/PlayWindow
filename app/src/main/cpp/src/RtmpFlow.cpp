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
    //av_dump_format(avfCtx, 0, publicUrl, 0);
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
        if (avformat_write_header(avfCtx, nullptr) < 0) {
            logi("avformat_write_header failed");
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
    AVStream *avStream = avformat_new_stream(avfCtx, avcodec);
    if (!avStream) {
        logi("avformat_new_stream failed");
        return;
    }
    // 设置时基
    avStream->time_base = AVRational{1, frameRate};
    avStream->codecpar->codec_tag = 0;
}


int RtmpFlow::sendMediaPacket(MediaPacket *packet) {
    logi("sendMediaPacket    0");
    avPacket->stream_index = video_index;
    if (startTime == 0) {
        startTime = packet->pts;
    }
    avPacket->pts = (packet->pts - startTime) / 1000 + 1;
    avPacket->dts = avPacket->pts;
    avPacket->size = packet->bufferSize;
    avPacket->data = (uint8_t *) packet->buffer;
    avPacket->pos = -1;
    logi("  data size : %d,   pts: %d", avPacket->size, avPacket->pts);
    av_interleaved_write_frame(avfCtx, avPacket);
    logi("sendMediaPacket    1");
    av_packet_unref(avPacket);
}

/**
 * 首先发送sps和pps，sps和pps需要按照NALU单元格式来拼接
 */
int RtmpFlow::sendVideoHeader(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {

    int nalSize = 13 + sps_len + 3 + pps_len;
    uint8_t *packet = (uint8_t *) av_malloc(nalSize);
}


/**
 * 发送AVPacket
 */
int RtmpFlow::sendVideoPacket(uint8_t *data, int len, long pts, bool isCsd) {
    logi("sendVideoPacket    0");
    avPacket->data = data;
    avPacket->stream_index = video_index;
    if (isCsd) {
        startTime = pts;
        avPacket->pts = 0;
        avPacket->dts = 0;
    } else {
        avPacket->pts = (pts - startTime) / 1000;
        avPacket->dts = avPacket->pts;
    }
    avPacket->size = len;
    logi("  data size : %d,   pts: %d", avPacket->size, avPacket->pts);

    if (avfCtx == nullptr) {
        logi("what    ");
    }
    av_interleaved_write_frame(avfCtx, avPacket);
    logi("sendVideoPacket    1");
    av_packet_free(&avPacket);

}

int RtmpFlow::sendAudioPacket(uint8_t *data, int len, long pts, bool isCsd) {


}


void RtmpFlow::release() {

}