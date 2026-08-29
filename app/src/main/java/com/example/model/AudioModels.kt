package com.example.model

import android.net.Uri
import java.io.File

enum class AudioFormat(
  val id: String,
  val extension: String,
  val displayName: String,
  val badge: String,
  val mimeType: String,
  val description: String,
  val recommendedBitrateKbps: Int,
  val isLossless: Boolean = false
) {
  MP3(
    id = "mp3",
    extension = "mp3",
    displayName = "MP3 (Audio Universal)",
    badge = "MP3",
    mimeType = "audio/mpeg",
    description = "Máxima compatibilidad en cualquier reproductor o dispositivo.",
    recommendedBitrateKbps = 192,
    isLossless = false
  ),
  M4A_AAC(
    id = "m4a",
    extension = "m4a",
    displayName = "M4A / AAC (Alta Fidelidad)",
    badge = "M4A",
    mimeType = "audio/mp4",
    description = "Excelente fidelidad y compresión eficiente en teléfonos modernos.",
    recommendedBitrateKbps = 256,
    isLossless = false
  ),
  WAV(
    id = "wav",
    extension = "wav",
    displayName = "WAV (Audio PCM Sin Pérdida)",
    badge = "WAV",
    mimeType = "audio/wav",
    description = "Sin compresión (PCM 16-bit). Ideal para producción y timbres.",
    recommendedBitrateKbps = 1411,
    isLossless = true
  ),
  FLAC(
    id = "flac",
    extension = "flac",
    displayName = "FLAC (Audiophile Lossless)",
    badge = "FLAC",
    mimeType = "audio/flac",
    description = "Calidad de estudio perfecta reduciendo el tamaño a la mitad.",
    recommendedBitrateKbps = 700,
    isLossless = true
  ),
  OGG_OPUS(
    id = "ogg",
    extension = "ogg",
    displayName = "OGG / Vorbis (Streaming)",
    badge = "OGG",
    mimeType = "audio/ogg",
    description = "Muy ligero, ideal para audios de voz y streaming.",
    recommendedBitrateKbps = 128,
    isLossless = false
  ),
  OPUS(
    id = "opus",
    extension = "opus",
    displayName = "OPUS (Baja Latencia & Voz)",
    badge = "OPUS",
    mimeType = "audio/opus",
    description = "El códec de voz y mensajería más eficiente del mundo.",
    recommendedBitrateKbps = 96,
    isLossless = false
  ),
  WMA(
    id = "wma",
    extension = "wma",
    displayName = "WMA (Windows Media Audio)",
    badge = "WMA",
    mimeType = "audio/x-ms-wma",
    description = "Formato estándar para ecosistema Windows y equipos antiguos.",
    recommendedBitrateKbps = 160,
    isLossless = false
  ),
  AIFF(
    id = "aiff",
    extension = "aiff",
    displayName = "AIFF (Apple Lossless PCM)",
    badge = "AIFF",
    mimeType = "audio/x-aiff",
    description = "Formato PCM sin comprimir para ecosistema Apple y estaciones DAW.",
    recommendedBitrateKbps = 1411,
    isLossless = true
  ),
  AMR(
    id = "amr",
    extension = "amr",
    displayName = "AMR (Notas de Voz Telefónicas)",
    badge = "AMR",
    mimeType = "audio/amr",
    description = "Compresión ultra extrema especializada en habla y llamadas.",
    recommendedBitrateKbps = 32,
    isLossless = false
  ),
  M4R(
    id = "m4r",
    extension = "m4r",
    displayName = "M4R (Tono de Llamada / Ringtone)",
    badge = "M4R",
    mimeType = "audio/x-m4r",
    description = "Formato AAC empaquetado para tonos de llamada en smartphones.",
    recommendedBitrateKbps = 256,
    isLossless = false
  ),
  AC3(
    id = "ac3",
    extension = "ac3",
    displayName = "AC3 (Dolby Digital Surround)",
    badge = "AC3",
    mimeType = "audio/ac3",
    description = "Estándar de audio envolvente para cine en casa y video.",
    recommendedBitrateKbps = 384,
    isLossless = false
  ),
  MP2(
    id = "mp2",
    extension = "mp2",
    displayName = "MP2 (Broadcast Radio)",
    badge = "MP2",
    mimeType = "audio/mpeg",
    description = "Formato clásico de radiodifusión digital y televisión.",
    recommendedBitrateKbps = 192,
    isLossless = false
  );

  val supportsBitrateCustomization: Boolean
    get() = this != WAV && this != AIFF && this != FLAC
}

enum class QualityPreset(
  val displayName: String,
  val description: String,
  val bitrateKbps: Int,
  val sampleRateHz: Int
) {
  ORIGINAL("Original", "Mantiene la calidad del archivo original", 0, 0),
  ULTRA("320 kbps (Ultra)", "Máxima fidelidad acústica para música", 320, 48000),
  HIGH("256 kbps (Alta)", "Excelente equilibrio entre peso y calidad", 256, 44100),
  STANDARD("192 kbps (Estándar)", "Recomendado para la mayoría de canciones", 192, 44100),
  ECONOMY("128 kbps (Económico)", "Tamaño reducido ideal para ahorrar espacio", 128, 44100),
  VOICE("64 kbps (Voz)", "Optimizado para podcasts, notas y llamadas", 64, 22050),
  CUSTOM("Personalizado", "Configura tasa de bits, frecuencia y canales a tu gusto", 192, 44100)
}

enum class AudioChannelMode(val displayName: String, val channels: Int) {
  KEEP_ORIGINAL("Mismo que original", 0),
  STEREO("Estéreo (2 canales)", 2),
  MONO("Mono (1 canal)", 1)
}

data class AudioFileInfo(
  val uri: Uri,
  val name: String,
  val formatExtension: String,
  val sizeBytes: Long,
  val durationMs: Long,
  val sampleRate: Int,
  val channelCount: Int,
  val bitrateKbps: Int,
  val artist: String? = null,
  val title: String? = null
) {
  val formattedDuration: String
    get() {
      val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
      val minutes = totalSeconds / 60
      val seconds = totalSeconds % 60
      return "%02d:%02d".format(minutes, seconds)
    }

  val formattedSize: String
    get() {
      if (sizeBytes <= 0) return "0 KB"
      val kb = sizeBytes / 1024.0
      val mb = kb / 1024.0
      return if (mb >= 1.0) "%.1f MB".format(mb) else "%.0f KB".format(kb)
    }
}

data class ConversionOptions(
  val targetFormat: AudioFormat = AudioFormat.MP3,
  val preset: QualityPreset = QualityPreset.STANDARD,
  val bitrateKbps: Int = 192,
  val sampleRateHz: Int = 44100,
  val channelMode: AudioChannelMode = AudioChannelMode.KEEP_ORIGINAL,
  val volumeMultiplier: Float = 1.0f,
  val customFileName: String = ""
)

data class ConvertedAudioFile(
  val id: String,
  val file: File,
  val name: String,
  val format: AudioFormat,
  val sizeBytes: Long,
  val durationMs: Long,
  val timestamp: Long,
  val sampleRate: Int,
  val channels: Int,
  val bitrateKbps: Int
) {
  val formattedDuration: String
    get() {
      val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
      val minutes = totalSeconds / 60
      val seconds = totalSeconds % 60
      return "%02d:%02d".format(minutes, seconds)
    }

  val formattedSize: String
    get() {
      if (sizeBytes <= 0) return "0 KB"
      val kb = sizeBytes / 1024.0
      val mb = kb / 1024.0
      return if (mb >= 1.0) "%.1f MB".format(mb) else "%.0f KB".format(kb)
    }
}

enum class ConversionState {
  IDLE,
  PREPARING,
  DECODING,
  ENCODING,
  SAVING,
  COMPLETED,
  ERROR
}

data class ConversionProgress(
  val state: ConversionState = ConversionState.IDLE,
  val progressPercent: Float = 0f,
  val statusMessage: String = "",
  val convertedFile: ConvertedAudioFile? = null,
  val errorMessage: String? = null
)
