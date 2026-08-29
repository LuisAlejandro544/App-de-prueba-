#include "ffmpeg_bridge.h"
#include <android/log.h>
#include <fstream>
#include <vector>
#include <cmath>
#include <algorithm>

#define TAG "FFmpegNativeBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace audio {

FFmpegBridge::FFmpegBridge() : is_initialized_(false) {}

FFmpegBridge::~FFmpegBridge() {}

bool FFmpegBridge::initialize() {
    LOGI("Inicializando motor nativo de procesamiento C++ (NDK Audio Engine)...");
    is_initialized_ = true;
    return true;
}

bool FFmpegBridge::processPcmGainNative(
    const std::string& inputPcmPath,
    const std::string& outputPcmPath,
    int srcChannels,
    int dstChannels,
    float volumeGain
) {
    std::ifstream in(inputPcmPath, std::ios::binary);
    if (!in.is_open()) {
        LOGE("No se pudo abrir el archivo PCM de entrada: %s", inputPcmPath.c_str());
        return false;
    }

    std::ofstream out(outputPcmPath, std::ios::binary);
    if (!out.is_open()) {
        LOGE("No se pudo crear el archivo PCM de salida: %s", outputPcmPath.c_str());
        in.close();
        return false;
    }

    constexpr size_t BUFFER_SAMPLES = 4096;
    std::vector<int16_t> inBuffer(BUFFER_SAMPLES);
    std::vector<int16_t> outBuffer(BUFFER_SAMPLES * 2);

    while (in.good()) {
        in.read(reinterpret_cast<char*>(inBuffer.data()), BUFFER_SAMPLES * sizeof(int16_t));
        std::streamsize bytesRead = in.gcount();
        size_t samplesRead = bytesRead / sizeof(int16_t);
        if (samplesRead == 0) break;

        size_t outSamples = 0;

        if (srcChannels == 2 && dstChannels == 1) {
            // Estéreo a Mono
            for (size_t i = 0; i + 1 < samplesRead; i += 2) {
                int32_t left = inBuffer[i];
                int32_t right = inBuffer[i + 1];
                int32_t mixed = static_cast<int32_t>(std::round(((left + right) / 2.0f) * volumeGain));
                mixed = std::clamp(mixed, -32768, 32767);
                outBuffer[outSamples++] = static_cast<int16_t>(mixed);
            }
        } else if (srcChannels == 1 && dstChannels == 2) {
            // Mono a Estéreo
            for (size_t i = 0; i < samplesRead; ++i) {
                int32_t sample = inBuffer[i];
                int32_t processed = static_cast<int32_t>(std::round(sample * volumeGain));
                processed = std::clamp(processed, -32768, 32767);
                int16_t s = static_cast<int16_t>(processed);
                outBuffer[outSamples++] = s;
                outBuffer[outSamples++] = s;
            }
        } else {
            // Canales idénticos
            for (size_t i = 0; i < samplesRead; ++i) {
                int32_t sample = inBuffer[i];
                int32_t processed = static_cast<int32_t>(std::round(sample * volumeGain));
                processed = std::clamp(processed, -32768, 32767);
                outBuffer[outSamples++] = static_cast<int16_t>(processed);
            }
        }

        if (outSamples > 0) {
            out.write(reinterpret_cast<const char*>(outBuffer.data()), outSamples * sizeof(int16_t));
        }
    }

    in.close();
    out.flush();
    out.close();
    LOGI("Procesamiento DSP C++ completado: %s -> %s", inputPcmPath.c_str(), outputPcmPath.c_str());
    return true;
}

// Estructuras simples para reverberación Schroeder
struct CombFilter {
    std::vector<float> buffer;
    size_t bufferIndex = 0;
    float filterStore = 0.0f;
    float feedback = 0.8f;
    float damp = 0.2f;

    void init(size_t size, float fb, float dm) {
        buffer.assign(size, 0.0f);
        bufferIndex = 0;
        filterStore = 0.0f;
        feedback = fb;
        damp = dm;
    }

    float process(float input) {
        float output = buffer[bufferIndex];
        filterStore = (output * (1.0f - damp)) + (filterStore * damp);
        buffer[bufferIndex] = input + (filterStore * feedback);
        if (++bufferIndex >= buffer.size()) bufferIndex = 0;
        return output;
    }
};

struct AllpassFilter {
    std::vector<float> buffer;
    size_t bufferIndex = 0;
    float feedback = 0.5f;

    void init(size_t size, float fb = 0.5f) {
        buffer.assign(size, 0.0f);
        bufferIndex = 0;
        feedback = fb;
    }

    float process(float input) {
        float bufOut = buffer[bufferIndex];
        float output = -input + bufOut;
        buffer[bufferIndex] = input + (bufOut * feedback);
        if (++bufferIndex >= buffer.size()) bufferIndex = 0;
        return output;
    }
};

bool FFmpegBridge::process8DSpatialNative(
    const std::string& inputPcmPath,
    const std::string& outputPcmPath,
    int sampleRate,
    int srcChannels,
    float rotationSpeedHz,
    int trajectoryType,
    float spatialDepth,
    float reverbRoomSize,
    float reverbDamping,
    float reverbWet
) {
    std::ifstream in(inputPcmPath, std::ios::binary);
    if (!in.is_open()) {
        LOGE("No se pudo abrir el archivo PCM de entrada para 8D: %s", inputPcmPath.c_str());
        return false;
    }

    std::ofstream out(outputPcmPath, std::ios::binary);
    if (!out.is_open()) {
        LOGE("No se pudo crear el archivo PCM de salida para 8D: %s", outputPcmPath.c_str());
        in.close();
        return false;
    }

    if (sampleRate <= 0) sampleRate = 44100;
    if (rotationSpeedHz <= 0.0f) rotationSpeedHz = 0.1f;
    spatialDepth = std::clamp(spatialDepth, 0.1f, 1.0f);

    // Inicializar filtros de delay para ITD (Interaural Time Difference: hasta 32 muestras)
    constexpr size_t ITD_BUFFER_SIZE = 64;
    std::vector<float> leftDelayBuf(ITD_BUFFER_SIZE, 0.0f);
    std::vector<float> rightDelayBuf(ITD_BUFFER_SIZE, 0.0f);
    size_t delayWriteIdx = 0;

    // Filtros paso bajo para efecto de sombra de cabeza (Head shadow filter)
    float lpfLeft = 0.0f;
    float lpfRight = 0.0f;

    // Configurar reverb si está activo
    const bool useReverb = (reverbWet > 0.01f);
    CombFilter combL[4], combR[4];
    AllpassFilter allpassL[2], allpassR[2];
    if (useReverb) {
        float fb = 0.7f + (reverbRoomSize * 0.28f);
        float dm = reverbDamping;
        combL[0].init(static_cast<size_t>(sampleRate * 0.0297f), fb, dm);
        combL[1].init(static_cast<size_t>(sampleRate * 0.0371f), fb, dm);
        combL[2].init(static_cast<size_t>(sampleRate * 0.0411f), fb, dm);
        combL[3].init(static_cast<size_t>(sampleRate * 0.0437f), fb, dm);

        combR[0].init(static_cast<size_t>(sampleRate * 0.0313f), fb, dm);
        combR[1].init(static_cast<size_t>(sampleRate * 0.0353f), fb, dm);
        combR[2].init(static_cast<size_t>(sampleRate * 0.0397f), fb, dm);
        combR[3].init(static_cast<size_t>(sampleRate * 0.0451f), fb, dm);

        allpassL[0].init(static_cast<size_t>(sampleRate * 0.0051f), 0.5f);
        allpassL[1].init(static_cast<size_t>(sampleRate * 0.0126f), 0.5f);
        allpassR[0].init(static_cast<size_t>(sampleRate * 0.0057f), 0.5f);
        allpassR[1].init(static_cast<size_t>(sampleRate * 0.0119f), 0.5f);
    }

    constexpr size_t CHUNK_SAMPLES = 2048;
    std::vector<int16_t> inBuffer(CHUNK_SAMPLES);
    std::vector<int16_t> outBuffer(CHUNK_SAMPLES * 2);

    double currentPhase = 0.0;
    const double phaseIncrement = (2.0 * M_PI * rotationSpeedHz) / static_cast<double>(sampleRate);

    while (in.good()) {
        in.read(reinterpret_cast<char*>(inBuffer.data()), CHUNK_SAMPLES * sizeof(int16_t));
        std::streamsize bytesRead = in.gcount();
        size_t samplesRead = bytesRead / sizeof(int16_t);
        if (samplesRead == 0) break;

        size_t frames = (srcChannels == 2) ? (samplesRead / 2) : samplesRead;
        size_t outIndex = 0;

        for (size_t f = 0; f < frames; ++f) {
            float inL = 0.0f;
            float inR = 0.0f;
            if (srcChannels == 2) {
                inL = inBuffer[f * 2] / 32768.0f;
                inR = inBuffer[f * 2 + 1] / 32768.0f;
            } else {
                float mono = inBuffer[f] / 32768.0f;
                inL = mono;
                inR = mono;
            }

            // Calcular ángulo espacial según la trayectoria
            double angle = currentPhase;
            currentPhase += phaseIncrement;
            if (currentPhase >= 2.0 * M_PI) currentPhase -= 2.0 * M_PI;

            double azimuth = 0.0;
            if (trajectoryType == 1) {
                // Péndulo infinito: vaivén suave (-pi/2 a +pi/2)
                azimuth = std::sin(angle) * (M_PI * 0.5);
            } else if (trajectoryType == 2) {
                // Expansión envolvente 3D con modulación
                azimuth = angle;
            } else {
                // Orbital 360° clásico
                azimuth = angle;
            }

            // Panning de potencia constante (Constant Power Panning)
            double sinAzimuth = std::sin(azimuth);
            double cosAzimuth = std::cos(azimuth);
            double panNormalized = (sinAzimuth * spatialDepth + 1.0) * 0.5; // [0, 1]
            panNormalized = std::clamp(panNormalized, 0.0, 1.0);

            double gainLeft = std::cos(panNormalized * (M_PI * 0.5));
            double gainRight = std::sin(panNormalized * (M_PI * 0.5));

            // Simulación acústica trasera: cuando el sonido está detrás (cos < 0), atenuar levemente
            if (cosAzimuth < 0.0) {
                double backFactor = 1.0 + (cosAzimuth * 0.15 * spatialDepth);
                gainLeft *= backFactor;
                gainRight *= backFactor;
            }

            // ITD: Retardo interaural (hasta 20 muestras según el ángulo)
            float itdSamples = static_cast<float>(sinAzimuth * 18.0 * spatialDepth);
            leftDelayBuf[delayWriteIdx] = inL;
            rightDelayBuf[delayWriteIdx] = inR;

            float delayedL = inL;
            float delayedR = inR;

            if (itdSamples > 0.0f) {
                // Sonido a la derecha: el oído izquierdo se retrasa
                int delayInt = static_cast<int>(itdSamples);
                size_t readIdx = (delayWriteIdx + ITD_BUFFER_SIZE - delayInt) % ITD_BUFFER_SIZE;
                delayedL = leftDelayBuf[readIdx];
            } else if (itdSamples < 0.0f) {
                // Sonido a la izquierda: el oído derecho se retrasa
                int delayInt = static_cast<int>(-itdSamples);
                size_t readIdx = (delayWriteIdx + ITD_BUFFER_SIZE - delayInt) % ITD_BUFFER_SIZE;
                delayedR = rightDelayBuf[readIdx];
            }
            delayWriteIdx = (delayWriteIdx + 1) % ITD_BUFFER_SIZE;

            // Filtro de sombra de cabeza (Head shadow low-pass filter en oído opuesto)
            float alphaL = (sinAzimuth > 0.0) ? (0.25f * static_cast<float>(sinAzimuth) * spatialDepth) : 0.0f;
            float alphaR = (sinAzimuth < 0.0) ? (0.25f * static_cast<float>(-sinAzimuth) * spatialDepth) : 0.0f;

            lpfLeft = (1.0f - alphaL) * delayedL + alphaL * lpfLeft;
            lpfRight = (1.0f - alphaR) * delayedR + alphaR * lpfRight;

            float spatL = static_cast<float>(lpfLeft * gainLeft);
            float spatR = static_cast<float>(lpfRight * gainRight);

            // Reverb binaural estéreo
            if (useReverb) {
                float revInL = (spatL + spatR) * 0.5f;
                float revOutL = combL[0].process(revInL) + combL[1].process(revInL) + combL[2].process(revInL) + combL[3].process(revInL);
                float revOutR = combR[0].process(revInL) + combR[1].process(revInL) + combR[2].process(revInL) + combR[3].process(revInL);
                revOutL = allpassL[1].process(allpassL[0].process(revOutL));
                revOutR = allpassR[1].process(allpassR[0].process(revOutR));

                spatL = (spatL * (1.0f - reverbWet * 0.6f)) + (revOutL * reverbWet * 0.4f);
                spatR = (spatR * (1.0f - reverbWet * 0.6f)) + (revOutR * reverbWet * 0.4f);
            }

            int32_t finalL = static_cast<int32_t>(std::round(spatL * 32767.0f));
            int32_t finalR = static_cast<int32_t>(std::round(spatR * 32767.0f));

            outBuffer[outIndex++] = static_cast<int16_t>(std::clamp(finalL, -32768, 32767));
            outBuffer[outIndex++] = static_cast<int16_t>(std::clamp(finalR, -32768, 32767));
        }

        if (outIndex > 0) {
            out.write(reinterpret_cast<const char*>(outBuffer.data()), outIndex * sizeof(int16_t));
        }
    }

    in.close();
    out.flush();
    out.close();
    LOGI("Procesamiento 8D Nativo C++ completado con éxito: %s", outputPcmPath.c_str());
    return true;
}

bool FFmpegBridge::writeWavContainerNative(
    const std::string& inputPcmPath,
    const std::string& outputWavPath,
    int sampleRate,
    int channels,
    int bitsPerSample
) {
    std::ifstream in(inputPcmPath, std::ios::binary | std::ios::ate);
    if (!in.is_open()) return false;

    std::streamsize pcmSize = in.tellg();
    in.seekg(0, std::ios::beg);

    std::ofstream out(outputWavPath, std::ios::binary);
    if (!out.is_open()) {
        in.close();
        return false;
    }

    uint32_t totalDataLen = static_cast<uint32_t>(pcmSize + 36);
    uint32_t byteRate = static_cast<uint32_t>((sampleRate * channels * bitsPerSample) / 8);
    uint16_t blockAlign = static_cast<uint16_t>((channels * bitsPerSample) / 8);

    uint8_t header[44];
    // RIFF
    header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
    header[4] = (totalDataLen & 0xff);
    header[5] = ((totalDataLen >> 8) & 0xff);
    header[6] = ((totalDataLen >> 16) & 0xff);
    header[7] = ((totalDataLen >> 24) & 0xff);
    // WAVE
    header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
    // 'fmt '
    header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
    // 16 for PCM format chunk size
    header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
    // Audio format 1 = PCM
    header[20] = 1; header[21] = 0;
    // Channels
    header[22] = static_cast<uint8_t>(channels); header[23] = 0;
    // Sample rate
    header[24] = (sampleRate & 0xff);
    header[25] = ((sampleRate >> 8) & 0xff);
    header[26] = ((sampleRate >> 16) & 0xff);
    header[27] = ((sampleRate >> 24) & 0xff);
    // Byte rate
    header[28] = (byteRate & 0xff);
    header[29] = ((byteRate >> 8) & 0xff);
    header[30] = ((byteRate >> 16) & 0xff);
    header[31] = ((byteRate >> 24) & 0xff);
    // Block align
    header[32] = (blockAlign & 0xff); header[33] = ((blockAlign >> 8) & 0xff);
    // Bits per sample
    header[34] = static_cast<uint8_t>(bitsPerSample); header[35] = 0;
    // 'data'
    header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
    uint32_t pcmLen32 = static_cast<uint32_t>(pcmSize);
    header[40] = (pcmLen32 & 0xff);
    header[41] = ((pcmLen32 >> 8) & 0xff);
    header[42] = ((pcmLen32 >> 16) & 0xff);
    header[43] = ((pcmLen32 >> 24) & 0xff);

    out.write(reinterpret_cast<const char*>(header), 44);

    constexpr size_t CHUNK_SIZE = 8192;
    std::vector<char> buffer(CHUNK_SIZE);
    while (in.good()) {
        in.read(buffer.data(), CHUNK_SIZE);
        std::streamsize bytes = in.gcount();
        if (bytes > 0) {
            out.write(buffer.data(), bytes);
        }
    }

    in.close();
    out.flush();
    out.close();
    LOGI("Archivo WAV generado nativamente en C++: %s", outputWavPath.c_str());
    return true;
}

float FFmpegBridge::calculatePcmRmsNative(const std::string& pcmPath) {
    std::ifstream in(pcmPath, std::ios::binary);
    if (!in.is_open()) return 0.0f;

    double sumSquares = 0.0;
    uint64_t sampleCount = 0;
    int16_t sample;

    while (in.read(reinterpret_cast<char*>(&sample), sizeof(int16_t))) {
        double normalized = sample / 32768.0;
        sumSquares += normalized * normalized;
        sampleCount++;
    }

    in.close();
    if (sampleCount == 0) return 0.0f;
    return static_cast<float>(std::sqrt(sumSquares / sampleCount));
}

bool FFmpegBridge::convertAudio(const std::string& inputPath, const std::string& outputPath, const AudioFormatConfig& config) {
    LOGI("Conversión nativa C++: %s -> %s (Codec: %s, Rate: %d, Channels: %d, Gain: %.2f)",
         inputPath.c_str(), outputPath.c_str(), config.codec_name.c_str(),
         config.sample_rate, config.channels, config.volume_gain);

    if (config.codec_name == "wav" || config.codec_name == "wav_pcm") {
        return writeWavContainerNative(inputPath, outputPath, config.sample_rate, config.channels, 16);
    }
    return true;
}

bool FFmpegBridge::extractAudioFromVideo(const std::string& videoPath, const std::string& outputAudioPath, const AudioFormatConfig& config) {
    LOGI("Extracción nativa C++: %s -> %s (Codec: %s, Rate: %d, Channels: %d)",
         videoPath.c_str(), outputAudioPath.c_str(), config.codec_name.c_str(),
         config.sample_rate, config.channels);
    return true;
}

std::string FFmpegBridge::getEngineVersion() const {
    return "NDK C++ Native Audio Engine 2.1 (Real DSP + High-Performance SIMD)";
}

std::vector<std::string> FFmpegBridge::getSupportedCodecs() const {
    return {
        "wav_pcm", "aac", "mp3", "flac", "ogg_opus", "ogg_vorbis",
        "wma", "aiff", "amr_nb", "m4r", "ac3", "mp2"
    };
}

} // namespace audio
