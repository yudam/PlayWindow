//
// Created by 毛大宇 on 2023/4/27.
//

#include "LocalRtmpPush.h"

#define logi(...) __android_log_print(ANDROID_LOG_INFO,"LocalRtmpPush",__VA_ARGS__)
#define loge(...) __android_log_print(ANDROID_LOG_ERROR,"LocalRtmpPush",__VA_ARGS__)

static bool timeout = false;
static bool abort_request = false;

/**
 * 回调函数，在返回1 时会产生中断，阻塞网络过程
 */
static int custom_interrupt_callback(void *arg) {
    if (timeout || abort_request) {
        return 1;
    }
    return 0;
}

/**
 * 本地视频推送的流程就是先解码，然后再重新编码指定格式推流，中间涉及到时间基的转换
 */

int LocalRtmpPush::open(const char *infilename, const char *outfilename) {

    int ret;
    av_register_all();
    avformat_network_init();

    /**
     * 1. 打开输入流，读取 header
     */
    ret = avformat_open_input(&in_avf_context, infilename, nullptr, nullptr);
    if (ret < 0) {
        loge("avformat_open_input");
        return ret;
    }
    // 主要负责媒体信息的分析工作
    avformat_find_stream_info(in_avf_context, nullptr);
    // ffmpeg提供了一个函数用于打印解析到的媒体信息
    av_dump_format(in_avf_context, 0, in_avf_context->filename, 0);
    // 获取指向特定流的index
    av_find_best_stream(in_avf_context, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);

    /**
     * 2. 创建输出流上下文
     */
    ret = avformat_alloc_output_context2(&out_avf_context, nullptr, "flv", outfilename);
    if (ret < 0) {
        loge("avformat_alloc_output_context2");
        return ret;
    }

    /**
     *  3. 输出流配置AVStream，也就是流通道
     */
    for (int i = 0; i < in_avf_context->nb_streams; i++) {
        AVStream *in_stream = in_avf_context->streams[i];
        AVCodec *avcodec = avcodec_find_encoder(in_stream->codecpar->codec_id);
        if (!avcodec) {
            loge("avcodec_find_encoder");
            return 0x11;
        }
        AVStream *out_stream = avformat_new_stream(out_avf_context, avcodec);
        avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar);
        /**
         * codec_tag表示音视频数据采用的码流格式，
         * avcodec_parameters_copy函数会将封装和解封装的codec_tag标签一致，会导致写入问题，
         * 所以codec_tag重置为0，让编码器去匹配默认的值
         * codec_tag表示编解码器相关的信息，这里必须重置为0，否则在avformat_write_header会报错误
         */
        out_stream->codecpar->codec_tag = 0;
        if (in_stream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_index = in_stream->index;
        } else if (in_stream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_index = in_stream->index;
        }
    }

    /**
     * 4. 当 flags没有被标记为AVFMT_NOFILE时，才能调用avio_open2函数初始化
     */
    if (!(out_avf_context->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open2(&out_avf_context->pb, outfilename, AVIO_FLAG_WRITE, nullptr, nullptr);
        if (ret < 0) {
            loge("avio_open2: %d, out_file:  %s", ret, outfilename);
            return ret;
        }
    }
    out_avf_context->interrupt_callback.callback = custom_interrupt_callback;


    for (int i = 0; i < in_avf_context->nb_streams; i++) {
        AVStream *avStream = in_avf_context->streams[i];
        if (avStream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            int size = avStream->codecpar->extradata_size;
            logi("in extradata size : %d", size);
            uint8_t *extradata = avStream->codecpar->extradata;
            for (int n = 0; n < size; n++) {
                logi("in extradata: %02x", extradata[n]);
            }
        }
    }

    for (int i = 0; i < out_avf_context->nb_streams; i++) {
        AVStream *avStream = out_avf_context->streams[i];
        if (avStream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            int size = avStream->codecpar->extradata_size;
            logi("out extradata size : %d", size);
            uint8_t *extradata = avStream->codecpar->extradata;
            for (int n = 0; n < size; n++) {
                logi("out extradata: %02x", extradata[n]);
            }
        }
    }


    ret = avformat_write_header(out_avf_context, nullptr);
    if (ret < 0) {
        loge("avformat_write_header");
        return ret;
    }
    return ret;
}


void LocalRtmpPush::push() {
    int64_t startTime = av_gettime();
    /**
     * 创建一个AVPacket，不通过这种方式分配默认值会有问题
     */
    AVPacket *avPacket = av_packet_alloc();

    while (true) {
        /**
         *  1. 读取下一帧数据到AVPacket中
         */
        int ret = av_read_frame(in_avf_context, avPacket);
        if (ret < 0) {
            loge("av_read_frame");
            return;
        }
        if (avPacket->stream_index != video_index && avPacket->stream_index != audio_index) {
            loge("stream_index failed");
            return;
        }

        if (avPacket->flags & AV_PKT_FLAG_KEY) {
            logi("当前帧是关键帧");
        }

        /**
         * 计算当前帧的pts，并比较当前的时间戳，来判断是否需要延迟
         */
        AVRational time_base = in_avf_context->streams[avPacket->stream_index]->time_base;
        int64_t pts_time = av_rescale_q(avPacket->pts, time_base, AV_TIME_BASE_Q);
        int currTime = av_gettime() - startTime;
        if (pts_time > currTime) {
            logi(" sleep time : %d", pts_time - currTime);
            av_usleep((unsigned int) (pts_time - currTime));
        }

        /**
         * 2. 计算输出流中的pts，dts和duration参数 ，然后写入
         */
        AVStream *in_stream = in_avf_context->streams[avPacket->stream_index];
        AVStream *out_stream = out_avf_context->streams[avPacket->stream_index];
//        avPacket->pts = av_rescale_q(avPacket->pts, in_stream->time_base, out_stream->time_base);
//        avPacket->dts = av_rescale_q(avPacket->dts, in_stream->time_base, out_stream->time_base);
//        avPacket->duration = av_rescale_q(avPacket->duration, in_stream->time_base, out_stream->time_base);
//        avPacket->pos = -1;

        av_packet_rescale_ts(avPacket, in_stream->time_base, out_stream->time_base);

        logi("av_interleaved_write_frame");
        if (av_interleaved_write_frame(out_avf_context, avPacket) < 0) {
            loge("av_interleaved_write_frame");
            break;
        }
        /**
         * 重置avPacket
         */
        av_packet_unref(avPacket);
    }
    /**
     * 回收AVPacket
     */
    av_packet_free(&avPacket);
}

void LocalRtmpPush::close() {
    logi("close");
    if (out_avf_context) {
        // 用于输出文件尾
        av_write_trailer(out_avf_context);
        avformat_free_context(out_avf_context);
        out_avf_context = NULL;
    }

    if (in_avf_context) {
        avformat_free_context(in_avf_context);
        in_avf_context = NULL;
    }
}