//
// Created by 毛大宇 on 2023/4/24.
//

#include <jni.h>
#include <string>
#include <android/log.h>
#include "LocalRtmpPush.h"

#define logi(...) __android_log_print(ANDROID_LOG_INFO,"JNILOG",__VA_ARGS__)

/**
 * 动态注册时，该类用于Java层和native的函数进行映射
 */
#define NATIVE_LINK_CLASS "com/play/window/BaseNative"

static LocalRtmpPush *localRtmpPush = nullptr;

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
    if(ret < 0){
        logi(" open failed");
    }
    localRtmpPush->push();
    localRtmpPush->close();
    env->ReleaseStringUTFChars(infilename,in_path);
    env->ReleaseStringUTFChars(outfilename,out_path);

}

void native_app_Push(JNIEnv *env, jobject thiz) {

}

void native_app_close(JNIEnv *env, jobject thiz) {

}

/**
 * Java层函数和native层函数的映射关系
 * 注意：native函数必须在native_method函数之上，否则无法找到
 */
static const JNINativeMethod native_method[] = {
        {"stringFromJNI", "()Ljava/lang/String;",                    (void *) native_stringFromJNI},
        {"app_Open",      "(Ljava/lang/String;Ljava/lang/String;)V", (void *) native_app_Open},
        {"app_Push",      "()V",                                     (void *) native_app_Push},
        {"app_close",     "()V",                                     (void *) native_app_close}
};

/**
 * jni库方法，在Java层加载动态库时，会调用该方法获取jni版本，可以进行一些初始化操作
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    if ((vm->GetEnv((void **) &env, JNI_VERSION_1_6)) != JNI_OK) {
        return JNI_ERR;
    }
    jclass clazz = env->FindClass(NATIVE_LINK_CLASS);

    if (clazz == NULL) {
        return JNI_ERR;
    }
    env->RegisterNatives(clazz, native_method, sizeof(native_method) / sizeof(native_method[0]));

    return JNI_VERSION_1_6;
}