//
// Created by 毛大宇 on 2023/4/24.
//

#include <jni.h>
#include <string>
#include <android/log.h>
#include "LocalRtmpPush.h"
#include "RtmpFlow.h"
#include "JNIUtil.h"
#include "aac_encoder.h"
#include "h264_encoder.h"


#define logi(...) __android_log_print(ANDROID_LOG_INFO,"JNILOG",__VA_ARGS__)

/**
 * 动态注册时，该类用于Java层和native的函数进行映射
 */

#define NATIVE_LINK_CLASS "com/play/window/BaseNative"
#define JAVA_MEDIAPACKET_CLASS  "com/play/window/codec/MediaPacket"

static LocalRtmpPush *localRtmpPush = nullptr;
static RtmpFlow *rtmpFlow = nullptr;
static JavaImpl *javaImpl = nullptr;
static AACEncoder *aacEncoder = nullptr;
static H264Encoder *h264Encoder = nullptr;

jstring native_stringFromJNI(JNIEnv *jniEnv, jobject object) {

    logi("--------native_stringFromJNI");
    return jniEnv->NewStringUTF("test_jni_dynamic");
}

void native_app_Open(JNIEnv *env, jobject thiz, jstring infilename, jstring outfilename) {
    if (localRtmpPush == nullptr) {
        localRtmpPush = new LocalRtmpPush();
    }
    logi(" native_app_Open");
    const char *in_path = env->GetStringUTFChars(infilename, NULL);
    const char *out_path = env->GetStringUTFChars(outfilename, NULL);
    int ret = localRtmpPush->open(in_path, out_path);
    if (ret < 0) {
        logi(" open failed");
    }
    localRtmpPush->push();
    localRtmpPush->close();
    env->ReleaseStringUTFChars(infilename, in_path);
    env->ReleaseStringUTFChars(outfilename, out_path);

}

void native_app_Push(JNIEnv *env, jobject thiz) {

}

void native_app_close(JNIEnv *env, jobject thiz) {

}

void native_initPublish(JNIEnv *env, jobject thiz, jstring url, jint video_bit_rate,
                        jint framerate, jint width, jint height) {
    rtmpFlow = new RtmpFlow();
    const char *rtmp_address = env->GetStringUTFChars(url, nullptr);
    rtmpFlow->init(const_cast<char *>(rtmp_address), video_bit_rate, framerate, width, height);
}

void native_connect(JNIEnv *env, jobject object) {
    rtmpFlow->connect();
}

void native_sendPacket(JNIEnv *env, jobject object, jobject packet) {
    MediaPacket *packet1 = new MediaPacket();
    packet1->pts = env->GetLongField(packet, javaImpl->java_pts);
    jobject dataBuffer = env->GetObjectField(packet, javaImpl->java_data);
    packet1->buffer = env->GetDirectBufferAddress(dataBuffer);
    packet1->bufferSize = env->GetIntField(packet, javaImpl->java_bufferSize);

    logi("  buffer : %d", packet1->buffer == nullptr);
    rtmpFlow->sendMediaPacket(packet1);
}

void native_release(JNIEnv *env, jobject object) {
    rtmpFlow->release();
}

void native_startAudioRecord(JNIEnv *env, jobject thiz, jstring path) {
    const char *url = env->GetStringUTFChars(path, nullptr);
    aacEncoder = new AACEncoder();
    aacEncoder->startAudio(url);
}

void native_setFrameData(JNIEnv *env, jobject thiz, jbyteArray data, jint len) {

    jbyte *buffer = env->GetByteArrayElements(data, nullptr);

    aacEncoder->setFrameData((uint8_t *) buffer, len);
}

void native_stopAudioRecord(JNIEnv *env, jobject thiz) {
    aacEncoder->stopAudio();
    delete aacEncoder;
}

void native_start_publish(JNIEnv *env, jobject thiz, jstring url, jint width, jint height) {
    h264Encoder = new H264Encoder();
    const char *rtmp = env->GetStringUTFChars(url, NULL);
    logi(" rtmp: %s,  width: %d,  height: %d",rtmp,width,height);
    h264Encoder->startPublish(rtmp, width, height);
}

void native_setVideoData(JNIEnv *env, jobject thiz, jbyteArray dataBuffer, jint len) {

    if(h264Encoder != nullptr){
        jbyte *buffer = env->GetByteArrayElements(dataBuffer, NULL);
        h264Encoder->encoderBuffer((uint8_t *) buffer, len);
    }
}

void native_stop_publish(JNIEnv *env, jobject thiz) {
    if(h264Encoder != nullptr){
        h264Encoder->stopPublish();
    }
}


/**
 * Java层函数和native层函数的映射关系
 * 注意：native函数必须在native_method函数之上，否则无法找到
 */
static const JNINativeMethod native_method[] = {
        {"stringFromJNI",    "()Ljava/lang/String;",                    (void *) native_stringFromJNI},
        {"app_Open",         "(Ljava/lang/String;Ljava/lang/String;)V", (void *) native_app_Open},
        {"app_Push",         "()V",                                     (void *) native_app_Push},
        {"app_close",        "()V",                                     (void *) native_app_close},
        {"initPublish",      "(Ljava/lang/String;IIII)V",               (void *) native_initPublish},
        {"connect",          "()V",                                     (void *) native_connect},
        {"sendPacket",       "(Lcom/play/window/codec/MediaPacket;)V",  (void *) native_sendPacket},
        {"release",          "()V",                                     (void *) native_release},
        {"startAudioRecord", "(Ljava/lang/String;)V",                   (void *) native_startAudioRecord},
        {"setFrameData",     "([BI)V",                                  (void *) native_setFrameData},
        {"stopAudioRecord",  "()V",                                     (void *) native_stopAudioRecord},
        {"startPublic",      "(Ljava/lang/String;II)V",                 (void *) native_start_publish},
        {"setVideoData",     "([BI)V",
                                                                        (void *) native_setVideoData},
        {"stopPublish",      "()V",                                     (void *) native_stop_publish}
};

void loadMediaPacketField(JNIEnv *pEnv);

/**
 * jni库方法，在Java层加载动态库时，会调用该方法获取jni版本，可以进行一些初始化操作
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    if ((vm->GetEnv((void **) &env, JNI_VERSION_1_6)) != JNI_OK) {
        return JNI_ERR;
    }
    loadMediaPacketField(env);
    jclass clazz = env->FindClass(NATIVE_LINK_CLASS);

    if (clazz == NULL) {
        return JNI_ERR;
    }
    env->RegisterNatives(clazz, native_method, sizeof(native_method) / sizeof(native_method[0]));

    return JNI_VERSION_1_6;
}


void loadMediaPacketField(JNIEnv *env) {
    javaImpl = new JavaImpl();
    jclass javaMediaPacketClass = env->FindClass(JAVA_MEDIAPACKET_CLASS);
    javaImpl->java_data = env->GetFieldID(javaMediaPacketClass, "data", "Ljava/nio/ByteBuffer;");
    javaImpl->java_csd0 = env->GetFieldID(javaMediaPacketClass, "csd0", "Ljava/nio/ByteBuffer;");
    javaImpl->java_csd1 = env->GetFieldID(javaMediaPacketClass, "csd1", "Ljava/nio/ByteBuffer;");
    javaImpl->java_pts = env->GetFieldID(javaMediaPacketClass, "pts", "J");
    javaImpl->java_isVideo = env->GetFieldID(javaMediaPacketClass, "isVideo", "Z");
    javaImpl->java_isAudio = env->GetFieldID(javaMediaPacketClass, "isAudio", "Z");
    javaImpl->java_isCsd = env->GetFieldID(javaMediaPacketClass, "isCsd", "Z");
    javaImpl->java_bufferSize = env->GetFieldID(javaMediaPacketClass, "bufferSize", "I");
    javaImpl->java_csd0Size = env->GetFieldID(javaMediaPacketClass, "csd0Size", "I");
    javaImpl->java_csd1Size = env->GetFieldID(javaMediaPacketClass, "csd1Size", "I");
}


void *startThread(void *args) {

    /**
     * 分配指定字节大小内存，并返回首地址
     */
    uint8_t *c = (uint8_t *) malloc(100);

    /**
     * 复制一部分字节到dest内存中
     */
    char *src = "Jni-test";
    char dest[20];
    memcpy(dest, src, strlen(src));
}


void stringMethod(JNIEnv *jniEnv, jobject object) {

    char *temp_str = "jni_learn";
    jstring newString = jniEnv->NewStringUTF(temp_str);
    jsize stringSize = jniEnv->GetStringLength(newString);

    jniEnv->ReleaseStringUTFChars(newString, temp_str);

    const jchar *copy_str = jniEnv->GetStringChars(newString, nullptr);
}

jobject callObjMethod(JNIEnv *jniEnv, jobject object) {
    // 1. 找到ArrayList类
    jclass list_class = jniEnv->FindClass("java/util/ArrayList");
    // 2. 获取构造函数
    jmethodID list_methodId = jniEnv->GetMethodID(list_class, "<init>", "()V");
    // 3. 创建ArrayList对象
    jobject listObj = jniEnv->NewObject(list_class, list_methodId);
    // 4. 获取List中Add方法
    jmethodID jaddMethodId = jniEnv->GetMethodID(list_class, "add", "(Ljava/lang/Object;)Z");
    // 5. 循环添加元素到集合中
    for (int i = 0; i < 10; i++) {
        jstring temp = jniEnv->NewStringUTF(" index : " + i);
        jniEnv->CallBooleanMethod(listObj, jaddMethodId, temp);
    }
    return listObj;
}