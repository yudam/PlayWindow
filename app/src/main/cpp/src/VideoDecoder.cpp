//
// Created by 毛大宇 on 2023/4/23.
//

#include "VideoDecoder.h"

void VideoDecoder::initdecoder() {

    /**
     * 创建 封装格式上下文
     */
    avFormatContext = avformat_alloc_context();
    if (!avFormatContext) {
        return;
    }
    /**
     * 打开输入流文件
     */
    int ret = avformat_open_input(&avFormatContext, m_url, nullptr, nullptr);
    if (ret < 0) {
        return;
    }
    /**
     * 获取音视频流信息
     */
    if (avformat_find_stream_info(avFormatContext, nullptr) < 0) {
        return;
    }

    /**
     * 获取音视频流索引
     */
    for (int i = 0; i < avFormatContext->nb_streams; i++) {
        if (avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            stream_index = avFormatContext->streams[i]->index;
            break;
        }
    }

    if (stream_index == -1) {
        return;
    }

    /**
     * 获取解码器参数
     */
    AVCodecParameters *codecpar = avFormatContext->streams[stream_index]->codecpar;
    /**
     * 获取解码器
     */
    AVCodec *avcodec = avcodec_find_decoder(codecpar->codec_id);
    if (!avcodec) {
        return;
    }

    /**
     * 创建解码器上下文
     */
    avCodecContext = avcodec_alloc_context3(avcodec);
    if (avcodec_parameters_to_context(avCodecContext, codecpar) < 0) {
        return;
    }

    /**
     * 打开解码器 , 这里可以选择配置一些参数在AVDictionary中
     */
    if (avcodec_open2(avCodecContext, avcodec, nullptr) < 0) {
        return;
    }

    avPacket = av_packet_alloc();
    avFrame = av_frame_alloc();

    // 表示流的持续时间，单位为微秒
    avFormatContext->duration;
    // 获取解码器中的宽高，这里的宽高就是解码出来的流的宽高，可以返回给渲染模块来调整画布大小
    avCodecContext->width;
    avCodecContext->height;
}


/**
 * 获取一帧数据的大小
 */
uint8_t *VideoDecoder::getFrameSize() {
    /**
     * 通过像素格式，图像宽高来计算所需的内存大小
     * align：此参数是设定内存对齐的对齐数，也就是按多大的字节进行内存对齐。比如设置为1，表示按1字节对齐，
     * 那么得到的结果就是与实际的内存大小一样。再比如设置为4，表示按4字节对齐。也就是内存的起始地址必须是4的整倍数。
     */
    int bufferSize = av_image_get_buffer_size(avCodecContext->pix_fmt, avCodecContext->width, avCodecContext->height, 1);
    uint8_t *frameBuffer = (uint8_t *) av_malloc(bufferSize * sizeof(uint8_t));
    return frameBuffer;
}


void VideoDecoder::initScale() {
    rgbFrame = av_frame_alloc();
    frameBuffer = getFrameSize();
    // 将新分配的AVFrame和frameBuffer缓冲区关联起来，当AVFrame中数据改变了，frameBuffer中也会改变
    av_image_fill_arrays(rgbFrame->data, rgbFrame->linesize, frameBuffer, AV_PIX_FMT_RGBA, avCodecContext->width,
                         avCodecContext->height, 1);
    // 设置将yuv转换为rgb的参数
    swsContext = sws_getContext(avCodecContext->width, avCodecContext->height, avCodecContext->pix_fmt,
                   avCodecContext->width, avCodecContext->height, AV_PIX_FMT_RGBA,
                   SWS_FAST_BILINEAR, NULL, NULL, NULL);
}

void VideoDecoder::loopdecoder() {
    /**
     * 循环读取一帧数据
     */
    while (av_read_frame(avFormatContext, avPacket) >= 0) {

        if (avPacket->stream_index != stream_index) {
            continue;
        }

        if (avcodec_send_packet(avCodecContext, avPacket) < 0) {
            return;
        }

        while (avcodec_receive_frame(avCodecContext, avFrame) == 0) {
            // avFrame中包含解码后的数据，此时可以发送给渲染层，也可以做一些同步操作
            onFrameAvailable(avFrame);
        }

        av_packet_unref(avPacket);
    }
}

void VideoDecoder::scaleFrameData(AVFrame *avFrame) {
    /**
     * 转换数据格式
     */
    sws_scale(swsContext,avFrame->data,avFrame->linesize,0,avCodecContext->height,
              rgbFrame->data,rgbFrame->linesize);
}

void VideoDecoder::onFrameAvailable(AVFrame *avFrame) {
    scaleFrameData(avFrame);
}


void VideoDecoder::release() {

}

void VideoDecoder::decoderMethod() {


}