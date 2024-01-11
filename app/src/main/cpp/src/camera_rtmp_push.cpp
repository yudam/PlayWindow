//
// Created by 毛大宇 on 2023/7/13.
//

#include "camera_rtmp_push.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_VERBOSE,"RTMP_PUSH",__VA_ARGS__)

RtmpPacketQueue<RTMPPacket *> rtmpPacketQueue;
std::atomic<bool> isPublishing;
uint32_t startTime;

uint32_t preTime;



void releaseRTMPPacket(RTMPPacket *packet){
    // C++中允许通过 if(packet) 表示非空，   if(!packet）表示空
    if(packet){
        RTMPPacket_Free(packet);
        delete packet;
        packet = nullptr;
    }
}

void *start_rtmp_thread(char *rtmp_url) {
    // 1. 初始化RTMP结构
    RTMP *rtmp = RTMP_Alloc();
    if (!rtmp) {
        LOGI(" RTMP_Alloc  failed");
        return NULL;
    }
    RTMP_Init(rtmp);
    rtmp->Link.timeout = 5;

    int ret = RTMP_SetupURL(rtmp, rtmp_url);
    if (!ret) {
        LOGI("RTMP_SetupURL failed : %s", rtmp_url);
        return NULL;
    }

    // 2. 向RTMP SERVER发送握手连接报文
    RTMP_EnableWrite(rtmp);
    ret = RTMP_Connect(rtmp, NULL);
    if (!ret) {
        LOGI("RTMP_Connect  failed  :",rtmp_url);
        return NULL;
    }

    ret = RTMP_ConnectStream(rtmp, 0);
    if (!ret) {
        LOGI("RTMP_ConnectStream  : %s",rtmp_url);
        return NULL;
    }

    // 3. 循环获取队列中数据包并发送
    startTime = RTMP_GetTime();
    isPublishing = true;
    RTMPPacket *rtmpPacket = nullptr;
    while (isPublishing) {

        ret = rtmpPacketQueue.pop(rtmpPacket);
        if(!isPublishing){
            break;
        }

        if(!ret){
            continue;
        }
        rtmpPacket->m_nInfoField2 = rtmp -> m_stream_id;
        ret = RTMP_SendPacket(rtmp,rtmpPacket,1);
        if(!ret){
            LOGI("RTMP_SendPacket   failed : %d ",rtmpPacket->m_nTimeStamp);
        } else {
            LOGI("RTMP_SendPacket   success   time : %d",rtmpPacket->m_nTimeStamp);
        }
        releaseRTMPPacket(rtmpPacket);
    }
    releaseRTMPPacket(rtmpPacket);

    // 4. 关闭并释放RTMP
    isPublishing = false;
    RTMP_Close(rtmp);
    RTMP_Free(rtmp);
}

/**
 * RTMP
 */
void camera_rtmp_push::initRtmp(char *rtmp_url) {
    std::thread rtmp_thread(start_rtmp_thread, rtmp_url);
    rtmp_thread.detach();
}


/**
 * 关闭并释放RTMP
 */
void camera_rtmp_push::stopRtmp() {
    isPublishing = false;
}

/**
 * 构造AAC的音频同步包
 * 通过RTMP推流时必须首先发送音视频的同步包
 */
void camera_rtmp_push::sendADTS(uint8_t * adts,int adts_len,int channel){
    initStartTime();
    int body_size = adts_len + 2;
    RTMPPacket *rtmpPacket = new RTMPPacket();
    RTMPPacket_Reset(rtmpPacket);
    RTMPPacket_Alloc(rtmpPacket, body_size);
    rtmpPacket->m_body[0] = 0xAF;
    if(channel == 1){
        rtmpPacket->m_body[0] = 0xAE;
    }
    rtmpPacket->m_body[1] = 0x00;

    memcpy(&rtmpPacket->m_body[2],adts,adts_len);

    rtmpPacket->m_hasAbsTimestamp = 0;
    rtmpPacket->m_nBodySize = body_size;
    rtmpPacket->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    rtmpPacket->m_nChannel = 0x11;
    rtmpPacket->m_headerType = RTMP_PACKET_SIZE_LARGE;
    rtmpPacket->m_nTimeStamp = 0;

    rtmpPacketQueue.push(rtmpPacket);

    LOGI("   adts    time :  %d");
}

/**
 * 构造音频数据包
 */
void camera_rtmp_push::sendAudio(uint8_t * data,int data_len,int channel,long time){
    initStartTime();

    int body_size  =  2 + data_len;
    auto * rtmpPacket = new RTMPPacket ();
    RTMPPacket_Alloc(rtmpPacket,body_size);

    rtmpPacket->m_body[0] = 0xAF;
    if(channel == 1){
        rtmpPacket->m_body[0] = 0xAE;
    }
    rtmpPacket->m_body[1] = 0x01;
    memcpy(&rtmpPacket->m_body[2],data,data_len);


    rtmpPacket->m_hasAbsTimestamp = 0;
    // 设置RTMP包长度
    rtmpPacket->m_nBodySize = body_size;
    // 设置RTMP包类型，这里是音频数据类型
    rtmpPacket->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    // 分配RTMP通道，随意分配 10
    rtmpPacket->m_nChannel = 0x11;
    // 设置头类型
    rtmpPacket->m_headerType = RTMP_PACKET_SIZE_LARGE;
    // 设置绝对时间，对于SPS、PPS、ADTS没有时间，赋值为0
    rtmpPacket->m_nTimeStamp = RTMP_GetTime() - startTime;

    rtmpPacketQueue.push(rtmpPacket);
    LOGI("   sendAudio    time :  %d",rtmpPacket->m_nTimeStamp);
}


/**
 * 根据FLV协议中视频同步包(AVC Sequence Header)格式构造数据包
 * 这里是不包含StartCode的sps和pps数据
 */
void camera_rtmp_push::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {

    initStartTime();

    //  去掉帧之间的界定符
    if (sps[2] == 0x00) {
        sps += 4;
        sps_len -= 4;
    } else {
        sps += 3;
        sps -= 3;
    }

    if (pps[2] == 0x00) {
        pps += 4;
        pps_len -= 4;
    } else {
        pps += 3;
        pps_len -= 3;
    }

    int body_size = 13 + sps_len + 3 + pps_len;

    RTMPPacket *rtmpPacket = new RTMPPacket();
    RTMPPacket_Reset(rtmpPacket);
    RTMPPacket_Alloc(rtmpPacket, body_size);

    int i = 0;

    // AVC Header
    rtmpPacket->m_body[i++] = 0x17;
    rtmpPacket->m_body[i++] = 0x00;
    rtmpPacket->m_body[i++] = 0x00;
    rtmpPacket->m_body[i++] = 0x00;
    rtmpPacket->m_body[i++] = 0x00;


    rtmpPacket->m_body[i++] = 0x01;
    rtmpPacket->m_body[i++] = sps[1];
    rtmpPacket->m_body[i++] = sps[2];
    rtmpPacket->m_body[i++] = sps[3];
    rtmpPacket->m_body[i++] = 0xff;

    rtmpPacket->m_body[i++] = 0xe1;
    rtmpPacket->m_body[i++] = (sps_len >> 8) & 0xff;
    rtmpPacket->m_body[i++] = sps_len & 0xff;
    memcpy(&rtmpPacket->m_body[i], sps, sps_len);
    i += sps_len;

    rtmpPacket->m_body[i++] = 0x01;
    rtmpPacket->m_body[i++] = (pps_len >> 8) & 0xff;
    rtmpPacket->m_body[i++] = pps_len & 0xff;
    memcpy(&rtmpPacket->m_body[i], pps, pps_len);

    rtmpPacket->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    rtmpPacket->m_nBodySize = body_size;
    rtmpPacket->m_nChannel = 0x10;

    // sps 和 pps没有时间戳
    rtmpPacket->m_nTimeStamp = 0;
    rtmpPacket->m_hasAbsTimestamp = 0;
    rtmpPacket->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

   // LOGI("spslen:  %d   ppslen:%d",sps_len,pps_len);
    rtmpPacketQueue.push(rtmpPacket);
}

/**
 * 按照FLV协议中对AVC格式的要求构造Video数据包
 */
void camera_rtmp_push::sendFrame(uint8_t *data, int data_len, int frame_key, long time) {

    initStartTime();


    // 去掉StartCode
    if (data[2] == 0x00) {
        data += 4;
        data_len -= 4;
    } else {
        data += 3;
        data_len -= 3;
    }

    int body_size = data_len + 9;
    auto *rtmpPacket = new RTMPPacket();
    RTMPPacket_Reset(rtmpPacket);
    RTMPPacket_Alloc(rtmpPacket, body_size);

    int i = 0;
    if (frame_key == 1) {
        // IDR帧
        rtmpPacket->m_body[i++] = 0x17;
    } else {
        // P或者B帧
        rtmpPacket->m_body[i++] = 0x27;
    }
    rtmpPacket->m_body[i++] = 0x01;
    rtmpPacket->m_body[i++] = 0x00;
    rtmpPacket->m_body[i++] = 0x00;
    rtmpPacket->m_body[i++] = 0x00;

    rtmpPacket->m_body[i++] = (data_len >> 24) & 0xff;
    rtmpPacket->m_body[i++] = (data_len >> 16) & 0xff;
    rtmpPacket->m_body[i++] = (data_len >> 8) & 0xff;
    rtmpPacket->m_body[i++] = (data_len) & 0xff;

    memcpy(&rtmpPacket->m_body[i], data, data_len);

    rtmpPacket->m_hasAbsTimestamp = 0;
    rtmpPacket->m_nBodySize = body_size;
    rtmpPacket->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    rtmpPacket->m_nChannel = 0x10;
    rtmpPacket->m_headerType = RTMP_PACKET_SIZE_LARGE;
    rtmpPacket->m_nTimeStamp = RTMP_GetTime() - startTime;

    //LOGI("m_nTimeStamp : %d ,      startTime : %d ,    currTime :  %d  ",rtmpPacket->m_nTimeStamp,startTime,RTMP_GetTime());
    rtmpPacketQueue.push(rtmpPacket);
}

void camera_rtmp_push::initStartTime() {
    if(startTime == 0){
        startTime = RTMP_GetTime();
    }
}
