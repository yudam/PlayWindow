//
// Created by 毛大宇 on 2023/5/11.
//

#include <iostream>
#include <jni.h>

class JavaImpl {

public:
    jfieldID java_data = nullptr;
    jfieldID java_csd0 = nullptr;
    jfieldID java_csd1 = nullptr;
    jfieldID java_isCsd = nullptr;
    jfieldID java_pts = nullptr;
    jfieldID java_isVideo = nullptr;
    jfieldID java_isAudio = nullptr;

    jfieldID java_bufferSize = nullptr;
    jfieldID java_csd0Size = nullptr;
    jfieldID java_csd1Size = nullptr;

    jfieldID java_key_frame = nullptr;
};