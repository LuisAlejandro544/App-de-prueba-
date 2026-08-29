#include <jni.h>
#include <string>
#include "ffmpeg_bridge.h"
#include <android/log.h>

#define LOG_TAG "NativeAudioEngineJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static audio::FFmpegBridge g_ffmpeg_bridge;

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_audio_NativeAudioBridge_getNativeEngineVersion(
        JNIEnv* env,
        jobject /* this */) {
    std::string version = g_ffmpeg_bridge.getEngineVersion();
    return env->NewStringUTF(version.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_NativeAudioBridge_initNativeFFmpeg(
        JNIEnv* env,
        jobject /* this */) {
    return static_cast<jboolean>(g_ffmpeg_bridge.initialize());
}

JNIEXPORT jobjectArray JNICALL
Java_com_example_audio_NativeAudioBridge_getNativeSupportedCodecs(
        JNIEnv* env,
        jobject /* this */) {
    std::vector<std::string> codecs = g_ffmpeg_bridge.getSupportedCodecs();
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(codecs.size(), stringClass, nullptr);

    for (size_t i = 0; i < codecs.size(); ++i) {
        jstring str = env->NewStringUTF(codecs[i].c_str());
        env->SetObjectArrayElement(result, i, str);
        env->DeleteLocalRef(str);
    }
    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_NativeAudioBridge_processPcmGainNative(
        JNIEnv* env,
        jobject /* this */,
        jstring input_pcm_path,
        jstring output_pcm_path,
        jint src_channels,
        jint dst_channels,
        jfloat volume_gain) {
    const char* in_p = env->GetStringUTFChars(input_pcm_path, nullptr);
    const char* out_p = env->GetStringUTFChars(output_pcm_path, nullptr);

    bool success = g_ffmpeg_bridge.processPcmGainNative(
        in_p ? in_p : "",
        out_p ? out_p : "",
        src_channels,
        dst_channels,
        volume_gain
    );

    if (in_p) env->ReleaseStringUTFChars(input_pcm_path, in_p);
    if (out_p) env->ReleaseStringUTFChars(output_pcm_path, out_p);

    return static_cast<jboolean>(success);
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_NativeAudioBridge_writeWavNative(
        JNIEnv* env,
        jobject /* this */,
        jstring input_pcm_path,
        jstring output_wav_path,
        jint sample_rate,
        jint channels,
        jint bits_per_sample) {
    const char* in_p = env->GetStringUTFChars(input_pcm_path, nullptr);
    const char* out_p = env->GetStringUTFChars(output_wav_path, nullptr);

    bool success = g_ffmpeg_bridge.writeWavContainerNative(
        in_p ? in_p : "",
        out_p ? out_p : "",
        sample_rate,
        channels,
        bits_per_sample
    );

    if (in_p) env->ReleaseStringUTFChars(input_pcm_path, in_p);
    if (out_p) env->ReleaseStringUTFChars(output_wav_path, out_p);

    return static_cast<jboolean>(success);
}

JNIEXPORT jfloat JNICALL
Java_com_example_audio_NativeAudioBridge_calculatePcmRmsNative(
        JNIEnv* env,
        jobject /* this */,
        jstring pcm_path) {
    const char* p_path = env->GetStringUTFChars(pcm_path, nullptr);
    float rms = g_ffmpeg_bridge.calculatePcmRmsNative(p_path ? p_path : "");
    if (p_path) env->ReleaseStringUTFChars(pcm_path, p_path);
    return rms;
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_NativeAudioBridge_convertNative(
        JNIEnv* env,
        jobject /* this */,
        jstring input_path,
        jstring output_path,
        jstring codec,
        jint sample_rate,
        jint channels,
        jint bitrate_kbps,
        jfloat volume_gain) {
    const char* in_p = env->GetStringUTFChars(input_path, nullptr);
    const char* out_p = env->GetStringUTFChars(output_path, nullptr);
    const char* c_name = env->GetStringUTFChars(codec, nullptr);

    audio::AudioFormatConfig cfg;
    cfg.codec_name = c_name ? c_name : "mp3";
    cfg.sample_rate = sample_rate;
    cfg.channels = channels;
    cfg.bitrate_kbps = bitrate_kbps;
    cfg.volume_gain = volume_gain;

    bool success = g_ffmpeg_bridge.convertAudio(in_p ? in_p : "", out_p ? out_p : "", cfg);

    if (in_p) env->ReleaseStringUTFChars(input_path, in_p);
    if (out_p) env->ReleaseStringUTFChars(output_path, out_p);
    if (c_name) env->ReleaseStringUTFChars(codec, c_name);

    return static_cast<jboolean>(success);
}

JNIEXPORT jboolean JNICALL
Java_com_example_audio_NativeAudioBridge_extractAudioFromVideoNative(
        JNIEnv* env,
        jobject /* this */,
        jstring video_path,
        jstring output_audio_path,
        jstring codec,
        jint sample_rate,
        jint channels,
        jint bitrate_kbps,
        jfloat volume_gain) {
    const char* vid_p = env->GetStringUTFChars(video_path, nullptr);
    const char* out_p = env->GetStringUTFChars(output_audio_path, nullptr);
    const char* c_name = env->GetStringUTFChars(codec, nullptr);

    audio::AudioFormatConfig cfg;
    cfg.codec_name = c_name ? c_name : "mp3";
    cfg.sample_rate = sample_rate;
    cfg.channels = channels;
    cfg.bitrate_kbps = bitrate_kbps;
    cfg.volume_gain = volume_gain;

    bool success = g_ffmpeg_bridge.extractAudioFromVideo(vid_p ? vid_p : "", out_p ? out_p : "", cfg);

    if (vid_p) env->ReleaseStringUTFChars(video_path, vid_p);
    if (out_p) env->ReleaseStringUTFChars(output_audio_path, out_p);
    if (c_name) env->ReleaseStringUTFChars(codec, c_name);

    return static_cast<jboolean>(success);
}

}
