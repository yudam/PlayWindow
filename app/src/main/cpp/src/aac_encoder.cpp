//
// Created by 毛大宇 on 2023/5/12.
//

#include "aac_encoder.h"

#define logi(...) __android_log_print(ANDROID_LOG_INFO,"AACEncoder",__VA_ARGS__)

void AACEncoder::startAudio(const char *url) {

    /**注册组件*/
    av_register_all();

    /** 申请一个输出文件上下文，该函数会创建 avFormatContext */
    int ret = avformat_alloc_output_context2(&avFormatContext, nullptr, nullptr, url);
    if (ret < 0) {
        logi("avformat_alloc_output_context2: %s",url);
        return;
    }

    /** 打开输出文件 */
    ret = avio_open(&avFormatContext->pb, url, AVIO_FLAG_READ_WRITE);
    if (ret < 0) {
        logi("avio_open");
        return;
    }

    /**
     * 获取编码器
     */
    AVCodec *avCodec = avcodec_find_encoder(AV_CODEC_ID_AAC);
    if (!avCodec) {
        logi("avcodec_find_encoder");
        return;
    }

    /**
     * 获取编码器上下，并配置编码器属性
     */
    avCodecContext = avcodec_alloc_context3(avCodec);
    if (!avCodecContext) {
        logi("avcodec_alloc_context3");
        return;
    }

    avCodecContext->codec_id = avCodec->id;
    avCodecContext->codec_type = AVMEDIA_TYPE_AUDIO; // 编解码器的类型，视频、音频
    avCodecContext->sample_fmt = AV_SAMPLE_FMT_FLTP;
    avCodecContext->sample_rate = 44100;
    avCodecContext->channel_layout = AV_CH_LAYOUT_STEREO;
    avCodecContext->channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
    avCodecContext->bit_rate = 96000;
    //avCodecContext->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL;

    if (avFormatContext->oformat->flags & AVFMT_GLOBALHEADER) {
        avCodecContext->flags |= CODEC_FLAG_GLOBAL_HEADER;
    }

    /**
     * 打开解码器
     */
    ret = avcodec_open2(avCodecContext, avCodec, nullptr);
    if (ret < 0) {
        logi("avcodec_open2");
        return;
    }

    /**
     * 为容器添加一路流
     */
    audiostream = avformat_new_stream(avFormatContext, avCodec);
    if (!audiostream) {
        logi("avformat_new_stream");
        return;
    }
    // 将编码器的参数复制给流
    ret = avcodec_parameters_from_context(audiostream->codecpar,avCodecContext);
    if(ret < 0){
        logi("avcodec_parameters_from_context");
        return;
    }

    /**
     * 写入头文件
     */
    ret = avformat_write_header(avFormatContext, nullptr);
    if (ret < 0) {
        logi("avformat_write_header %d",ret);
        return;
    }

    // 初始化AVPacket，用户后续接收编码数据并写入文件
    avPacket = av_packet_alloc();

    /**
     * 初始化AVFrame，并且分配内存大小，最后初始化 SwrContext用于后续的pcm数据转换
     */
    avFrame = av_frame_alloc();

    /**
     * 音频对应的一个AVFrame中包含多少个音频帧，这里表示多少个，默认frame_size为1024
     * nb_samples：表示但个通道中一帧音频包含的采样数
     */
    avFrame->nb_samples = avCodecContext->frame_size;
    avFrame->format = avCodecContext->sample_fmt;

    int bufferSize = av_samples_get_buffer_size(nullptr, avCodecContext->channels, avCodecContext->frame_size,
                                                avCodecContext->sample_fmt, 1);
    const uint8_t *audioBuffer = (uint8_t *) av_malloc(bufferSize);
    avcodec_fill_audio_frame(avFrame, avCodecContext->channels, avCodecContext->sample_fmt,
                             audioBuffer, bufferSize, 1);
    initSwrConfig();
}

/**
 * 编码pcm数据并保存
 */
void AACEncoder::encoderAudio(AVCodecContext *codecContext, AVFrame *audioFrame) {
    if (avcodec_send_frame(codecContext, audioFrame) < 0) {
        logi("avcodec_send_frame");
        return;
    }

    while (avcodec_receive_packet(codecContext, avPacket) == 0) {
        // 1. 编码器输出的AVPacket和流绑定，因为这里获取的acPacket是没有和流绑定的
        avPacket->stream_index = audiostream->index;

        // 2. 转换AVPacket的pts、dts、duration时间基为输出模式的时间基
        int ret = av_interleaved_write_frame(avFormatContext, avPacket);
        if (ret < 0) {
            logi("av_interleaved_write_frame");
        } else {
            logi("success av_interleaved_write_frame  %d",ret);
        }
        av_packet_unref(avPacket);
    }
    logi("failed");
}

/**
 * 对pcm数据进行重采样
 */
void AACEncoder::setFrameData(const uint8_t *data, int len) {

    /**
     *  定义输出数据保存的数组，len为4096
     *  AV_SAMPLE_FMT_FLTP采样格式采用两个数组，也就是data[0]和data[1]来存储
     *
     *  swr_convert第三个和第五个参数表示单通道样本的数量，不是字节数
     *
     *  采样大小len = (位深)2 * (通道数)2 * (采样数)1024
     *  len / 4 表示传入的音频数据但各个通道的采样数：
     *
     *  注意out_count 一定要大于in_count
     *
     *  ret表示返回的单个通道的采样数，值一般小于预期的采样数，
     *
     */
    uint8_t *out[2];
    out[0] = new uint8_t[len];
    out[1] = new uint8_t[len];
    int ret = swr_convert(swr, (uint8_t **) &out, len * 4, &data, len / 4);
    logi("  swr_convert  per count : %d    len:  %d",ret,len);
    if (ret < 0) {
        logi("swr_convert");
    }

    avFrame->data[0] = out[0];
    avFrame->data[1] = out[1];
    encoderAudio(avCodecContext, avFrame);

    delete out[0];
    delete out[1];
}


/**
 * 转码相关的参数配置，主要是将采样格式从AV_SAMPLE_FMT_S16转换为AV_SAMPLE_FMT_FLTP
 */
void AACEncoder::initSwrConfig() {
    swr = swr_alloc();
    av_opt_set_channel_layout(swr, "in_channel_layout", AV_CH_LAYOUT_STEREO, 0);
    av_opt_set_channel_layout(swr, "out_channel_layout", AV_CH_LAYOUT_STEREO, 0);
    av_opt_set_int(swr, "in_sample_rate", 44100, 0);
    av_opt_set_int(swr, "out_sample_rate", 44100, 0);
    av_opt_set_sample_fmt(swr, "in_sample_fmt", AV_SAMPLE_FMT_S16, 0);
    av_opt_set_sample_fmt(swr, "out_sample_fmt", AV_SAMPLE_FMT_FLTP, 0);
    swr_init(swr);
}

void AACEncoder::stopAudio() {
    encoderAudio(avCodecContext, nullptr);
    av_write_trailer(avFormatContext);
    avcodec_close(avCodecContext);
    av_free(avFrame);
    av_free(avPacket);
    avio_close(avFormatContext->pb);
    avformat_free_context(avFormatContext);
}