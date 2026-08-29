package com.example.model

import java.io.File

/**
 * Nivel de umbral de decibelios para detección de silencios.
 */
enum class SilenceThresholdLevel(
  val displayName: String,
  val dbThreshold: Float,
  val description: String
) {
  GENTLE("Suave (-45 dB)", -45.0f, "Corta únicamente silencios absolutos sin afectar susurros."),
  BALANCED("Equilibrado (-35 dB)", -35.0f, "Ideal para notas de voz, conferencias y podcasts."),
  AGGRESSIVE("Agresivo (-25 dB)", -25.0f, "Elimina pausas leves y ruidos de fondo constantes.")
}

/**
 * Modo de procesamiento sobre las secciones en silencio.
 */
enum class SilenceCutMode(
  val displayName: String,
  val description: String
) {
  REMOVE("Eliminar Silencios", "Suprime por completo los silencios empalmando el audio con voz."),
  SPEED_UP("Acelerar Silencios (4x)", "Conserva los silencios pero los comprime a velocidad cuádruple.")
}

/**
 * Opciones de configuración para la herramienta de eliminar silencios.
 */
data class SilenceRemoverOptions(
  val thresholdLevel: SilenceThresholdLevel = SilenceThresholdLevel.BALANCED,
  val minSilenceDurationMs: Long = 350L,
  val paddingVoiceMs: Long = 60L,
  val mode: SilenceCutMode = SilenceCutMode.REMOVE,
  val targetFormat: AudioFormat = AudioFormat.MP3,
  val bitrateKbps: Int = 192,
  val customFileName: String = ""
)

/**
 * Estados del flujo de procesamiento.
 */
enum class SilenceState {
  IDLE,
  DECODING,
  ANALYZING_DSP,
  CUTTING_AUDIO,
  ENCODING,
  COMPLETED,
  ERROR
}

/**
 * Estado y progreso del proceso de eliminación de silencios.
 */
data class SilenceProgress(
  val state: SilenceState = SilenceState.IDLE,
  val progress: Float = 0f,
  val statusMessage: String = "",
  val originalDurationMs: Long = 0L,
  val finalDurationMs: Long = 0L,
  val savedDurationMs: Long = 0L,
  val savedPercentage: Int = 0,
  val segmentsCut: Int = 0,
  val outputFile: File? = null,
  val errorMessage: String? = null
)
