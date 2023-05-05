//
// Created by 毛大宇 on 2023/4/23.
//

#include "VideoEncoder.h"

void VideoEncoder::encoderMethod() {

    /**
     * 分配一个 AVFormatContext对象，该方法内部会调用 avformat_alloc_context
     */
    AVFormatContext * encoder_fmt_ctx = nullptr;
    avformat_alloc_output_context2(&encoder_fmt_ctx, nullptr,"flv",NULL);

    //avio_open2()


    /**
     * 创建一路流并填充对应的信息，分配AVStream对象
     */
    AVStream *newStream = avformat_new_stream(nullptr, nullptr);
    int ret = avformat_write_header(nullptr, nullptr);

    avcodec_send_frame(nullptr, nullptr);

    avcodec_receive_packet(nullptr, nullptr);

    av_write_frame(nullptr, nullptr);

    /**
     * 将没有输出的AVPacket全部丢给协议层去做输出，
     */
    av_write_trailer(nullptr);
}