#include <aaudio/AAudio.h>
#include <android/log.h>

#define LOG_TAG "AAudioCapture"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static AAudioStream* stream = nullptr;

static aaudio_data_callback_result_t
dataCallback(AAudioStream* stream,
             void* userData,
             void* audioData,
             int32_t numFrames) {

    int16_t* input = (int16_t*) audioData;

    // 🔹 اینجا بعداً میره به VAD / FFT
    // فعلاً فقط تست

    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

extern "C"
void startAAudio() {

    AAudioStreamBuilder* builder;
    AAudio_createStreamBuilder(&builder);

    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_INPUT);
    AAudioStreamBuilder_setSampleRate(builder, 16000);
    AAudioStreamBuilder_setChannelCount(builder, 1);
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_I16);
    AAudioStreamBuilder_setPerformanceMode(
            builder,
            AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);

    AAudioStreamBuilder_setDataCallback(builder, dataCallback, nullptr);

    AAudioStreamBuilder_openStream(builder, &stream);
    AAudioStream_requestStart(stream);

    LOGI("AAudio started");
}
