#include <jni.h>
#include <stdlib.h>
#include <android/log.h>
#include "fvad.h"

#define LOG_TAG "VAD"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static Fvad* vad = nullptr;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_VadEngine_init(JNIEnv *env, jobject thiz, jint sampleRate) {

    vad = fvad_new();
    if (!vad) {
        LOGD("Failed to create VAD");
        return;
    }

    fvad_set_mode(vad, 2); // 0-3
    fvad_set_sample_rate(vad, sampleRate);

    LOGD("VAD initialized");
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_myapplication_VadEngine_process(
        JNIEnv *env,
        jobject thiz,
        jshortArray audioFrame) {

    if (!vad) return 0;

    jshort* data = env->GetShortArrayElements(audioFrame, nullptr);
    int length = env->GetArrayLength(audioFrame);

    int result = fvad_process(vad, data, length);

    env->ReleaseShortArrayElements(audioFrame, data, 0);

    return result; // 1 speech, 0 silence
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_VadEngine_release(
        JNIEnv *env,
        jobject thiz) {

    if (vad) {
        fvad_free(vad);
        vad = nullptr;
    }
}
