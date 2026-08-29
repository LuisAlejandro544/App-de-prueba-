package com.example.model

import java.io.File

/**
 * Perfil acústico para orientar el algoritmo de compresión.
 */
enum class AudioCompressionProfile(val displayName: String, val description: String) {
  SPEECH_PODCAST(
    displayName = "Voz / Podcasts / Clases",
    description = "Optimizado para grabaciones de voz. Convierte a mono y filtra frecuencias no vocales logrando hasta un 80% de ahorro sin perder inteligibilidad."
  ),
  MUSIC_BALANCED(
    displayName = "Música / Canciones",
    description = "Preserva la imagen estéreo y la respuesta en frecuencia dinámica con codificación psicoacústica avanzada."
  ),
  AGGRESSIVE_SPACE(
    displayName = "Máximo Ahorro de Espacio",
    description = "Prioriza el menor tamaño posible reduciendo tasa de muestreo y bitrate a niveles ultra compactos."
  )
}

/**
 * Ajustes predefinidos (Presets) de compresión rápida.
 */
enum class CompressionPreset(
  val displayName: String,
  val subtitle: String,
  val targetBitrateKbps: Int,
  val targetSampleRateHz: Int,
  val channelMode: AudioChannelMode,
  val maxTargetSizeMb: Float?
) {
  WHATSAPP_DISCORD(
    displayName = "WhatsApp / Discord",
    subtitle = "Garantiza un peso < 16 MB para envío instantáneo",
    targetBitrateKbps = 64,
    targetSampleRateHz = 32000,
    channelMode = AudioChannelMode.MONO,
    maxTargetSizeMb = 15.5f
  ),
  EMAIL_LIMIT(
    displayName = "Email / Gmail",
    subtitle = "Ajusta para límite de adjunto < 25 MB",
    targetBitrateKbps = 96,
    targetSampleRateHz = 44100,
    channelMode = AudioChannelMode.STEREO,
    maxTargetSizeMb = 24.0f
  ),
  MAX_SAVING(
    displayName = "Ahorro Extremo (75-85%)",
    subtitle = "Bitrate de 48 kbps para audios muy largos",
    targetBitrateKbps = 48,
    targetSampleRateHz = 24000,
    channelMode = AudioChannelMode.MONO,
    maxTargetSizeMb = null
  ),
  BALANCED(
    displayName = "Equilibrado (50-60%)",
    subtitle = "Bitrate de 96 kbps con buena fidelidad",
    targetBitrateKbps = 96,
    targetSampleRateHz = 44100,
    channelMode = AudioChannelMode.STEREO,
    maxTargetSizeMb = null
  ),
  LIGHT(
    displayName = "Ligero (30-40%)",
    subtitle = "Bitrate de 128 kbps con calidad casi intacta",
    targetBitrateKbps = 128,
    targetSampleRateHz = 44100,
    channelMode = AudioChannelMode.STEREO,
    maxTargetSizeMb = null
  ),
  TARGET_SIZE_CUSTOM(
    displayName = "Tamaño Específico (MB)",
    subtitle = "Calcula el bitrate exacto para no superar los MB deseados",
    targetBitrateKbps = 64,
    targetSampleRateHz = 32000,
    channelMode = AudioChannelMode.STEREO,
    maxTargetSizeMb = 10.0f
  ),
  CUSTOM(
    displayName = "Personalizado",
    subtitle = "Control total sobre bitrate, frecuencias y canales",
    targetBitrateKbps = 96,
    targetSampleRateHz = 44100,
    channelMode = AudioChannelMode.KEEP_ORIGINAL,
    maxTargetSizeMb = null
  )
}

/**
 * Opciones de configuración para la tarea de compresión de audio.
 */
data class CompressionOptions(
  val targetFormat: AudioFormat = AudioFormat.M4A_AAC,
  val preset: CompressionPreset = CompressionPreset.BALANCED,
  val profile: AudioCompressionProfile = AudioCompressionProfile.SPEECH_PODCAST,
  val targetBitrateKbps: Int = 96,
  val targetSampleRateHz: Int = 44100,
  val channelMode: AudioChannelMode = AudioChannelMode.MONO,
  val customTargetSizeMb: Float = 10.0f,
  val customFileName: String? = null
)

/**
 * Estados del flujo de compresión.
 */
enum class CompressionState {
  IDLE,
  ANALYZING,
  DECODING,
  ENCODING,
  COMPLETED,
  ERROR
}

/**
 * Estado y progreso en tiempo real del compresor.
 */
data class CompressionProgress(
  val state: CompressionState = CompressionState.IDLE,
  val progressPercent: Float = 0f,
  val statusMessage: String = "",
  val originalSizeBytes: Long = 0L,
  val estimatedSizeBytes: Long = 0L,
  val finalSizeBytes: Long = 0L,
  val savedPercentage: Int = 0,
  val outputFile: File? = null,
  val errorMessage: String? = null
)
