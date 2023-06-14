//
// Created by 毛大宇 on 2023/4/23.
//

#include "VideoDecoder.h"

#define logi(...) __android_log_print(ANDROID_LOG_INFO,"VideoDecoder",__VA_ARGS__)


void VideoDecoder::init(char *media_file, JNIEnv *jniEnv, jobject object) {

    this->filename = media_file;
    this->jni_env = jniEnv;
    this->java_surface = object;
    /**
     * 打开输入流文件
     */
    int ret = avformat_open_input(&avFormatContext, filename, nullptr, nullptr);
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
     * 获取解码器
     */
    AVCodecParameters *codecpar = avFormatContext->streams[stream_index]->codecpar;
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

    m_videoWidth = avCodecContext->width;
    m_videoHeight = avCodecContext->height;

    avPacket = av_packet_alloc();
    // 分配一个AVFrame结构体
    avFrame = av_frame_alloc();
    rgbFrame = av_frame_alloc();
    /**
     * 通过像素格式，图像宽高来计算所需的内存大小
     * align：此参数是设定内存对齐的对齐数，也就是按多大的字节进行内存对齐。比如设置为1，表示按1字节对齐，
     * 那么得到的结果就是与实际的内存大小一样。再比如设置为4，表示按4字节对齐。也就是内存的起始地址必须是4的整倍数。
     */
    int request_buffer_size = av_image_get_buffer_size(AV_PIX_FMT_RGBA, avCodecContext->width, avCodecContext->height, 1);
    frameBuffer = (uint8_t *) av_malloc(request_buffer_size);
    // 将新分配的AVFrame和frameBuffer缓冲区关联起来，当AVFrame中数据改变了，frameBuffer中也会改变
    av_image_fill_arrays(rgbFrame->data, rgbFrame->linesize, frameBuffer, AV_PIX_FMT_RGBA, avCodecContext->width,
                         avCodecContext->height, 1);
    // 设置将yuv转换为rgb的参数
    swsContext = sws_getContext(avCodecContext->width, avCodecContext->height, avCodecContext->pix_fmt,
                                avCodecContext->width, avCodecContext->height, AV_PIX_FMT_RGBA,
                                SWS_FAST_BILINEAR, NULL, NULL, NULL);
}

void VideoDecoder::startDecoder() {
    /**
     * 循环读取一帧数据
     */
    while (av_read_frame(avFormatContext, avPacket) >= 0) {

        if (avPacket->stream_index != stream_index) {
            continue;
        }

        if (avcodec_send_packet(avCodecContext, avPacket) != 0) {
            logi("avcodec_send_packet  failed");
            return;
        }

        while (avcodec_receive_frame(avCodecContext, avFrame) == 0) {
            /**
             * 转换数据格式
             */
            sws_scale(swsContext, avFrame->data, avFrame->linesize, 0, avCodecContext->height,
                      rgbFrame->data, rgbFrame->linesize);
            /**
             * 发送到窗口
             */
            onFrameAvailable(avFrame);
        }
        av_packet_unref(avPacket);
    }
}

/**
 * avFrame->linesize 表示每行的字节数，一般每一行要求64或者128位对齐，不够的在后面添加0x00，
 * 所以linesize一般大于图像每行的字节数
 *
 */
void VideoDecoder::onFrameAvailable(AVFrame *avFrame) {
    aNativeWindow = ANativeWindow_fromSurface(jni_env, java_surface);
    // 设置渲染区域和输入格式
    ANativeWindow_setBuffersGeometry(aNativeWindow,m_videoWidth,m_videoHeight,WINDOW_FORMAT_RGBA_8888);
    // 渲染
    ANativeWindow_Buffer m_ANativeWindow_Buffer;

    // 锁定当前Window，获取屏幕缓冲区Buffer的指针
    ANativeWindow_lock(aNativeWindow, &m_ANativeWindow_Buffer, nullptr);
    uint8_t *dstBuffer = static_cast<uint8_t *>(m_ANativeWindow_Buffer.bits);

    // 输入的图的步长（一行像素有多少字节）
    int srcLinesize = rgbFrame->linesize[0];
    // RGBA缓冲区步长
    int dstLInesize = m_ANativeWindow_Buffer.stride * 4;

    for (int i = 0; i < m_videoHeight; i++) {
        // 一行一行的拷贝图像数据
        memcpy(dstBuffer+i*dstLInesize,frameBuffer + i * srcLinesize,srcLinesize);
    }

    // 解锁当前Window，渲染缓冲区数据
    ANativeWindow_unlockAndPost(aNativeWindow);
}


void VideoDecoder::release() {

    if(avFrame != NULL){
        av_frame_free(&avFrame);
        avFrame = NULL;
    }

    if(rgbFrame != NULL){
        av_frame_free(&rgbFrame);
        rgbFrame = NULL;
    }

    if(avPacket != NULL){
        av_packet_free(&avPacket);
        avPacket = NULL;
    }

    if(frameBuffer != NULL){
        av_free(frameBuffer);
        frameBuffer = NULL;
    }

    if(swsContext != NULL){
        sws_freeContext(swsContext);
        swsContext = NULL;
    }

    if(avCodecContext != NULL){
        avcodec_close(avCodecContext);
        avcodec_free_context(&avCodecContext);
        avCodecContext  = NULL;
    }

    if(avFormatContext != NULL){
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;
    }

    if(aNativeWindow != NULL){
        ANativeWindow_release(aNativeWindow);
        aNativeWindow = NULL;
    }
}