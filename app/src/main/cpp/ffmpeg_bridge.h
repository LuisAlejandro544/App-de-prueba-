#ifndef FFMPEG_BRIDGE_H
#define FFMPEG_BRIDGE_H

#include <string>
#include <vector>
#include <cstdint>

namespace audio {

struct AudioFormatConfig {
    std::string codec_name;
    int sample_rate;
    int channels;
    int bitrate_kbps;
    float volume_gain;
};

class FFmpegBridge {
public:
    FFmpegBridge();
    ~FFmpegBridge();

    bool initialize();
    bool convertAudio(const std::string& inputPath, const std::string& outputPath, const AudioFormatConfig& config);
    bool extractAudioFromVideo(const std::string& videoPath, const std::string& outputAudioPath, const AudioFormatConfig& config);
    
    // Funciones reales de procesamiento DSP nativo en C++
    bool processPcmGainNative(const std::string& inputPcmPath, const std::string& outputPcmPath, int srcChannels, int dstChannels, float volumeGain);
    bool writeWavContainerNative(const std::string& inputPcmPath, const std::string& outputWavPath, int sampleRate, int channels, int bitsPerSample = 16);
    float calculatePcmRmsNative(const std::string& pcmPath);

    std::string getEngineVersion() const;
    std::vector<std::string> getSupportedCodecs() const;

private:
    bool is_initialized_;
};

} // namespace audio

#endif // FFMPEG_BRIDGE_H
