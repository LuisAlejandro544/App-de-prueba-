package com.example.audio

import android.util.Log

object NativeAudioBridge {
  private const val TAG = "NativeAudioBridge"
  private var isLibraryLoaded = false

  init {
    try {
      System.loadLibrary("native_audio_engine")
      isLibraryLoaded = true
      initNativeFFmpeg()
      Log.i(TAG, "Librería nativa C++ NDK cargada con éxito: ${getNativeEngineVersion()}")
    } catch (e: UnsatisfiedLinkError) {
      Log.w(TAG, "C++ Native library no disponible en este entorno: ${e.message}")
    } catch (e: Throwable) {
      Log.w(TAG, "Error cargando motor C++ nativo: ${e.message}")
    }
  }

  fun isAvailable(): Boolean = isLibraryLoaded

  external fun getNativeEngineVersion(): String
  external fun initNativeFFmpeg(): Boolean
  external fun getNativeSupportedCodecs(): Array<String>

  external fun processPcmGainNative(
    inputPcmPath: String,
    outputPcmPath: String,
    srcChannels: Int,
    dstChannels: Int,
    volumeGain: Float
  ): Boolean

  external fun writeWavNative(
    inputPcmPath: String,
    outputWavPath: String,
    sampleRate: Int,
    channels: Int,
    bitsPerSample: Int = 16
  ): Boolean

  external fun calculatePcmRmsNative(pcmPath: String): Float

  external fun convertNative(
    inputPath: String,
    outputPath: String,
    codec: String,
    sampleRate: Int,
    channels: Int,
    bitrateKbps: Int,
    volumeGain: Float
  ): Boolean

  external fun extractAudioFromVideoNative(
    videoPath: String,
    outputAudioPath: String,
    codec: String,
    sampleRate: Int,
    channels: Int,
    bitrateKbps: Int,
    volumeGain: Float
  ): Boolean
}
