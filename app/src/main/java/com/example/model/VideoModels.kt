package com.example.model

import android.graphics.Bitmap
import android.net.Uri

enum class ExtractionMode(val displayName: String, val description: String) {
  HIGH_FIDELITY_CONVERSION(
    displayName = "Conversión de Alta Fidelidad",
    description = "Codifica al formato y tasa de bits seleccionada con control acústico y volumen."
  ),
  DIRECT_STREAM_COPY(
    displayName = "Extracción Directa (Ultra Rápida)",
    description = "Extrae la pista de audio original en segundos sin recodificar ni perder calidad."
  )
}

data class VideoFileInfo(
  val uri: Uri,
  val name: String,
  val containerExtension: String,
  val sizeBytes: Long,
  val durationMs: Long,
  val width: Int = 0,
  val height: Int = 0,
  val audioMimeType: String = "",
  val audioCodecName: String = "AAC",
  val audioChannels: Int = 2,
  val audioSampleRate: Int = 44100,
  val audioBitrateKbps: Int = 192,
  val hasAudioTrack: Boolean = true,
  val thumbnail: Bitmap? = null
) {
  val resolutionLabel: String
    get() = when {
      width >= 3840 || height >= 2160 -> "4K UHD (${width}x${height})"
      width >= 2560 || height >= 1440 -> "2K QHD (${width}x${height})"
      width >= 1920 || height >= 1080 -> "1080p FHD (${width}x${height})"
      width >= 1280 || height >= 720 -> "720p HD (${width}x${height})"
      width > 0 && height > 0 -> "${width}x${height} SD"
      else -> "Video Estándar"
    }
}

data class VideoExtractionOptions(
  val targetFormat: AudioFormat = AudioFormat.MP3,
  val mode: ExtractionMode = ExtractionMode.HIGH_FIDELITY_CONVERSION,
  val preset: QualityPreset = QualityPreset.STANDARD,
  val bitrateKbps: Int = 192,
  val sampleRateHz: Int = 44100,
  val channelMode: AudioChannelMode = AudioChannelMode.KEEP_ORIGINAL,
  val volumeMultiplier: Float = 1.0f,
  val customFileName: String = ""
)

enum class VideoExtractionState {
  IDLE,
  ANALYZING,
  EXTRACTING,
  COMPLETED,
  ERROR
}

data class VideoExtractionProgress(
  val state: VideoExtractionState = VideoExtractionState.IDLE,
  val progressPercent: Float = 0f,
  val statusMessage: String = "",
  val extractedFile: ConvertedAudioFile? = null,
  val errorMessage: String? = null
)
