//
// Created by 毛大宇 on 2023/4/23.
//

#ifndef PLAYWINDOW_VIDEOENCODER_H
#define PLAYWINDOW_VIDEOENCODER_H

#include "iostream"

extern "C" {
#include "include/libavformat/avformat.h"
#include "include/libavcodec/avcodec.h"
};

class VideoEncoder {

private:

    void encoderMethod();
};


#endif //PLAYWINDOW_VIDEOENCODER_H
