package com.example.model

import java.io.File

/**
 * Representa un segmento de pista detectado a partir del análisis de silencio.
 */
data class DetectedTrackSegment(
  val trackIndex: Int,
  val startMs: Long,
  val endMs: Long,
  val durationMs: Long,
  val startFrame: Long,
  val endFrame: Long,
  val isSelected: Boolean = true,
  val customTitle: String = ""
)

/**
 * Sensibilidad de detección de silencio para la división en pistas.
 */
enum class SplitSilenceSensitivity(
  val displayName: String,
  val thresholdDb: Float,
  val description: String
) {
  SENSITIVE("Sensible (-45 dB)", -45.0f, "Corta ante pausas claras sin cortar silencios intermedios breves."),
  BALANCED("Equilibrado (-35 dB)", -35.0f, "Recomendado para separar canciones de un álbum o secciones de clase."),
  RELAXED("Relajado (-25 dB)", -25.0f, "Útil si hay ruido o soplido de fondo constante en la grabación.")
}

/**
 * Duraciones mínimas de silencio para disparar la división.
 */
enum class SplitSilenceDuration(
  val displayName: String,
  val durationMs: Long,
  val description: String
) {
  SHORT("1.0 segundo", 1000L, "Para pistas con pausas breves entre temas o frases."),
  MEDIUM("2.0 segundos", 2000L, "Estándar para álbumes continuos y sesiones de música."),
  LONG("3.0 segundos", 3000L, "Para conferencias largas o entrevistas espaciadas.")
}

/**
 * Opciones de configuración para la división de audio por silencios.
 */
data class AudioSplitOptions(
  val sensitivity: SplitSilenceSensitivity = SplitSilenceSensitivity.BALANCED,
  val minSilenceDuration: SplitSilenceDuration = SplitSilenceDuration.MEDIUM,
  val minTrackDurationMs: Long = 2000L, // Ignora ruidos de menos de 2s
  val paddingMs: Long = 100L,          // Evita cortar la primera y última nota
  val targetFormat: AudioFormat = AudioFormat.MP3,
  val bitrateKbps: Int = 192,
  val baseFileName: String = "",
  val zipAllTracks: Boolean = false
)

/**
 * Estados del proceso de división.
 */
enum class SplitProcessingState {
  IDLE,
  ANALYZING,
  PREVIEW_READY,
  SPLITTING_EXPORT,
  COMPLETED,
  ERROR
}

/**
 * Progreso y estado general de la división de audio.
 */
data class AudioSplitProgress(
  val state: SplitProcessingState = SplitProcessingState.IDLE,
  val progress: Float = 0f,
  val statusMessage: String = "",
  val totalTracksCount: Int = 0,
  val exportedTracks: List<File> = emptyList(),
  val zipFile: File? = null,
  val errorMessage: String? = null
)
