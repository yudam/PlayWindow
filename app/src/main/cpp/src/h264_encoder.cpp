//
// Created by 毛大宇 on 2023/4/23.
//

#include "h264_encoder.h"

#define logi(...) __android_log_print(ANDROID_LOG_INFO,"H264Encoder",__VA_ARGS__)



/**
 * 创建编码器和输出上下文
 */
void H264Encoder::startPublish(const char *url, int width, int height) {
    this->rtmpUrl = url;
    this->width = width;
    this->height = height;
    logi(" url: %s,  width: %d,  height: %d", url, width, height);
    av_register_all();
    avformat_network_init();

    int ret = avformat_alloc_output_context2(&avFormatContext, NULL, "flv", rtmpUrl);
    if (ret < 0) {
        logi("avformat_alloc_output_context2");
        return;
    }

    AVCodec *avCodec = avcodec_find_encoder(AV_CODEC_ID_H264);
    if (!avCodec) {
        logi("avcodec_find_encoder");
        return;
    }

    avCodecContext = avcodec_alloc_context3(avCodec);
    avCodecContext->codec_id = avCodec->id;
    avCodecContext->codec_type = AVMEDIA_TYPE_VIDEO;
    avCodecContext->pix_fmt = AV_PIX_FMT_YUV420P;
    avCodecContext->width = height;
    avCodecContext->height = width;
    avCodecContext->bit_rate = 100 * 1024 * 8;
    avCodecContext->gop_size = 20;
    avCodecContext->max_b_frames = 0;
    avCodecContext->qmin = 10;
    avCodecContext->qmax = 50;
    // fps设置过大会导致卡顿，实测20没问题，30就会隔几秒卡顿一次
    avCodecContext->time_base = AVRational{1, fps};
    avCodecContext->framerate = AVRational{fps, 1};
    avCodecContext->level = 41;
    avCodecContext->refs = 1;
    avCodecContext->qcompress = 0.6;

    if (avFormatContext->oformat->flags & AVFMT_GLOBALHEADER) {
        avCodecContext->flags |= CODEC_FLAG_GLOBAL_HEADER;
    }

    /**
     * H264编码会存在延迟编码，这里设置 AVDictionary就是为了立即编码
     */
    AVDictionary *options = NULL;
    if (avCodecContext->codec_id == AV_CODEC_ID_H264) {
        av_dict_set(&options, "preset", "superfast", 0);
        av_dict_set(&options, "tune", "zerolatency", 0);
    }
    ret = avcodec_open2(avCodecContext, avCodec, &options);
    if (ret < 0) {
        logi("avcodec_open2");
        return;
    }

    /**
     * 这里设置的time_base会在调用avio_open后被重置为编码格式对应的time_base
     * 比如H264对应的时间基是{1,1000}
     */

    avStream = avformat_new_stream(avFormatContext, avCodec);
    if (!avStream) {
        logi("avformat_new_stream");
        return;
    }
    avStream->time_base = AVRational{1, fps};
    avStream->codecpar->codec_tag = 0;
    ret = avcodec_parameters_from_context(avStream->codecpar, avCodecContext);
    if (ret < 0) {
        logi("avcodec_parameters_from_context");
        return;
    }


    /**
     * 打开输出地址
     */
    ret = avio_open(&avFormatContext->pb, rtmpUrl, AVIO_FLAG_READ_WRITE);
    if (ret < 0) {
        logi("avio_open : %d", ret);
        return;
    }

    /**
     * 写头文件，写入头文件后输出通道的AVStream的时间基就会变成{1,1000}
     */
    ret = avformat_write_header(avFormatContext, nullptr);
    if (ret < 0) {
        logi("avformat_write_header");
        return;
    }

    /**
     * 初始化AVFrame，AVPacket
     */
    avFrame = av_frame_alloc();
    avFrame->width = avCodecContext->width;
    avFrame->height = avCodecContext->height;
    avFrame->format = avCodecContext->pix_fmt;
    int bufferSize = av_image_get_buffer_size(avCodecContext->pix_fmt,
                                              avCodecContext->width,
                                              avCodecContext->height, 1);
    out_buffer = (uint8_t *) av_malloc(bufferSize);
    av_image_fill_arrays(avFrame->data, avFrame->linesize, out_buffer,
                         avCodecContext->pix_fmt, avCodecContext->width,
                         avCodecContext->height, 1);

    // 创建已编码帧
    av_new_packet(&avPacket, bufferSize * 3);

    logi("   num   : %d ,   den  :  %d", avStream->time_base.num, avStream->time_base.den);

    isInit = true;
}


void H264Encoder::encoderBuffer(uint8_t *nv21Buffer, int len) {

    if (!isInit) return;
    convertToI420(nv21Buffer);

    /**
     * 以index作为时间戳，index值得是相机返回过来的帧的下标
     */
    avFrame->pts = index;
    int ret = avcodec_send_frame(avCodecContext, avFrame);
    if (ret < 0) {
        logi("avcodec_send_frame");
        return;
    }

    while (!ret) {
        ret = avcodec_receive_packet(avCodecContext, &avPacket);
        if (ret < 0 || ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            return;
        }

        /**
         * 将编码的时间基对应的pts 转换为flv对应的时间基的时间戳，avStream中时间基础在写入头文件后
         * 就会变成flv对应的时间基{1,1000},但是编码其的时间基还是{1,20}，所以需要转换时间戳
         */
        avPacket.pts = av_rescale_q(avFrame->pts,avCodecContext->time_base,avStream->time_base);
        avPacket.dts = avPacket.pts;
        // 计算一帧持续的时间，这里avStream的time_base是{1,1000},所以duration= 1000 / 40 单位ms
        avPacket.duration = avStream->time_base.den / fps;
        avPacket.pos = -1;
        ret = av_interleaved_write_frame(avFormatContext, &avPacket);
        if (ret < 0) {
            logi("av_interleaved_write_frame    failed : %d", ret);
        }
        av_packet_unref(&avPacket);
        index++;
    }
}


void H264Encoder::stopPublish() {

    av_write_trailer(avFormatContext);


    if (avCodecContext != NULL) {
        avcodec_close(avCodecContext);
        avcodec_free_context(&avCodecContext);
        avCodecContext = NULL;
    }


    if (avFrame != NULL) {
        av_free(avFrame);
        avFrame = NULL;
    }

    if (out_buffer != NULL) {
        av_free(out_buffer);
        out_buffer = NULL;
    }

    if (avFormatContext != NULL) {
        avio_close(avFormatContext->pb);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;
    }

}

void H264Encoder::convertToI420(uint8_t *nv21Buffer) {

    uint8_t *i420_y = out_buffer;
    uint8_t *i420_u = out_buffer + width * height;
    uint8_t *i420_v = out_buffer + width * height * 5 / 4;

    libyuv::ConvertToI420(nv21Buffer, width * height, i420_y, height,
                          i420_u, height / 2, i420_v, height / 2, 0, 0,
                          width, height, width, height, libyuv::kRotate270,
                          libyuv::FOURCC_NV21);
    avFrame->data[0] = i420_y;
    avFrame->data[1] = i420_u;
    avFrame->data[2] = i420_v;
}