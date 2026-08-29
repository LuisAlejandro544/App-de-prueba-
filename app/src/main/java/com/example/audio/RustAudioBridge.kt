package com.example.audio

import android.util.Log

object RustAudioBridge {
  private const val TAG = "RustAudioBridge"
  private var isLibraryLoaded = false

  init {
    try {
      System.loadLibrary("audio_converter_core")
      isLibraryLoaded = true
      initRustPipeline()
      Log.i(TAG, "Librería nativa Rust cargada: ${getRustEngineVersion()}")
    } catch (e: UnsatisfiedLinkError) {
      Log.i(TAG, "Rust Core nativo preparado para compilación cruzada cargo-ndk: ${e.message}")
    } catch (e: Throwable) {
      Log.w(TAG, "Error inicializando Rust Bridge: ${e.message}")
    }
  }

  fun isAvailable(): Boolean = isLibraryLoaded

  external fun getRustEngineVersion(): String
  external fun initRustPipeline(): Boolean
  external fun processAudioRust(
    inputPath: String,
    outputPath: String,
    sampleRate: Int,
    channels: Int,
    volumeGain: Float
  ): Boolean

  external fun extractAudioFromVideoRust(
    videoPath: String,
    outputPath: String,
    sampleRate: Int,
    channels: Int,
    volumeGain: Float
  ): Boolean
}
