package com.example.model

import android.net.Uri
import java.io.File

/**
 * Representa una pista individual dentro de la lista de unión.
 */
data class MergeTrackItem(
  val id: String = java.util.UUID.randomUUID().toString(),
  val uri: Uri,
  val name: String,
  val formatExtension: String,
  val sizeBytes: Long,
  val durationMs: Long,
  val sampleRate: Int,
  val channelCount: Int,
  val bitrateKbps: Int
)

/**
 * Opciones de configuración para la unión de pistas.
 */
data class MergeOptions(
  val targetFormat: AudioFormat = AudioFormat.MP3,
  val bitrateKbps: Int = 192,
  val sampleRateHz: Int = 44100,
  val targetChannels: Int = 2, // Estéreo estándar para máxima compatibilidad
  val customFileName: String = "",
  val enableCrossfade: Boolean = true // Micro-fundido de 15ms para evitar chasquidos digitales
)

/**
 * Estados del proceso de unión.
 */
enum class MergeState {
  IDLE,
  PREPARING,
  DECODING_TRACKS,
  NORMALIZING_DSP,
  ENCODING_OUTPUT,
  SAVING,
  COMPLETED,
  ERROR
}

/**
 * Progreso en tiempo real de la unión de pistas.
 */
data class MergeProgress(
  val state: MergeState = MergeState.IDLE,
  val currentTrackIndex: Int = 0,
  val totalTracks: Int = 0,
  val progressPercent: Float = 0f,
  val statusMessage: String = "",
  val mergedFile: ConvertedAudioFile? = null,
  val errorMessage: String? = null
)
