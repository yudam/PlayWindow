//
// Created by 毛大宇 on 2023/7/13.
//

#ifndef PLAYWINDOW_CAMERA_RTMP_PUSH_H
#define PLAYWINDOW_CAMERA_RTMP_PUSH_H


#include "rtmp.h"
#include <cstring>
#include "android/log.h"
#include "RtmpPacketQueue.h"

class camera_rtmp_push {

private :


public:
    void initRtmp(char *rtmp_url);

    void initStartTime();

    void sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len);

    void sendFrame(uint8_t *data, int data_len, int frame_key, long time);

    void sendADTS(uint8_t * adts,int adts_len,int channel);

    void sendAudio(uint8_t * data,int data_len,int channel,long time);

    void stopRtmp();
};


#endif //PLAYWINDOW_CAMERA_RTMP_PUSH_H
