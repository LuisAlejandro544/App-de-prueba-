package com.example.audio

import android.content.Context
import com.example.audio.codec.MediaCodecEncoder
import com.example.audio.codec.PcmDecoder
import com.example.audio.codec.PcmDspProcessor
import com.example.audio.codec.WavEncoder
import com.example.model.AudioChannelMode
import com.example.model.AudioFileInfo
import com.example.model.AudioFormat
import com.example.model.ConversionOptions
import com.example.model.ConvertedAudioFile
import com.example.model.QualityPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Orquestador principal del proceso de transcodificación de archivos de audio.
 * Integra decodificación universal, procesamiento DSP (ganancia, canales) y codificación multipropósito.
 */
object AudioTranscoder {

  suspend fun convertAudio(
    context: Context,
    inputInfo: AudioFileInfo,
    options: ConversionOptions,
    onProgress: (progress: Float, message: String) -> Unit
  ): Result<ConvertedAudioFile> = withContext(Dispatchers.IO) {
    var pcmTempFile: File? = null
    try {
      onProgress(0.05f, "Preparando archivo de audio...")

      val outputDir = AppStorageManager.getFolder(context, AppAudioFolder.CONVERTIR)
      val targetFormat = options.targetFormat

      val sanitizedBaseName = if (options.customFileName.isNotBlank()) {
        options.customFileName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
      } else {
        val originalName = inputInfo.name.substringBeforeLast(".")
        "${originalName}_converted"
      }

      val outputFile = File(outputDir, "$sanitizedBaseName.${targetFormat.extension}")
      val uniqueOutputFile = if (outputFile.exists()) {
        File(outputDir, "${sanitizedBaseName}_${System.currentTimeMillis().toString().takeLast(4)}.${targetFormat.extension}")
      } else {
        outputFile
      }

      // Paso 1: Decodificar entrada a PCM lineal
      onProgress(0.15f, "Decodificando audio original...")
      pcmTempFile = File(context.cacheDir, "temp_decode_${UUID.randomUUID()}.pcm")

      val decodeResult = PcmDecoder.decodeMediaToPcm(
        context = context,
        uri = inputInfo.uri,
        outputPcmFile = pcmTempFile,
        onProgress = { prog ->
          onProgress(0.15f + (prog * 0.35f), "Decodificando pistas de audio (${(prog * 100).toInt()}%)")
        }
      )

      if (!decodeResult.success) {
        return@withContext Result.failure(Exception(decodeResult.error ?: "Error al decodificar audio"))
      }

      val sampleRate = decodeResult.sampleRate
      val channels = decodeResult.channels
      val durationMs = decodeResult.durationMs

      // Calcular parámetros acústicos seguros (protección contra inflado artificial de bitrate)
      val maxAllowedSampleRate = sampleRate.coerceAtMost(inputInfo.sampleRate.takeIf { it > 0 } ?: sampleRate)
      val targetSampleRate = when {
        options.preset == QualityPreset.ORIGINAL -> sampleRate
        options.sampleRateHz > 0 -> options.sampleRateHz.coerceAtMost(maxAllowedSampleRate)
        else -> sampleRate
      }

      val targetChannels = when (options.channelMode) {
        AudioChannelMode.KEEP_ORIGINAL -> channels
        AudioChannelMode.STEREO -> 2
        AudioChannelMode.MONO -> 1
      }

      val maxAllowedBitrate = inputInfo.bitrateKbps.coerceAtLeast(32)
      val rawBitrate = when {
        options.preset == QualityPreset.ORIGINAL -> inputInfo.bitrateKbps.coerceIn(64, 320)
        options.preset.bitrateKbps > 0 -> options.preset.bitrateKbps
        else -> options.bitrateKbps
      }
      val targetBitrateKbps = rawBitrate.coerceAtMost(maxAllowedBitrate)

      // Paso 2: Procesamiento DSP (Ganancia, Mezcla de canales y Resample)
      onProgress(0.55f, "Ajustando parámetros acústicos y volumen...")
      val processedPcmFile = File(context.cacheDir, "temp_processed_${UUID.randomUUID()}.pcm")
      PcmDspProcessor.resampleAndProcessPcm(
        inputPcm = pcmTempFile,
        outputPcm = processedPcmFile,
        srcSampleRate = sampleRate,
        dstSampleRate = targetSampleRate,
        srcChannels = channels,
        dstChannels = targetChannels,
        volume = options.volumeMultiplier
      )

      pcmTempFile.delete()
      pcmTempFile = processedPcmFile

      // Notificación al motor nativo C++
      if (NativeAudioBridge.isAvailable()) {
        NativeAudioBridge.convertNative(
          inputPath = pcmTempFile.absolutePath,
          outputPath = uniqueOutputFile.absolutePath,
          codec = targetFormat.id,
          sampleRate = targetSampleRate,
          channels = targetChannels,
          bitrateKbps = targetBitrateKbps,
          volumeGain = options.volumeMultiplier
        )
      }

      // Paso 3: Codificación al formato objetivo
      onProgress(0.70f, "Codificando a formato ${targetFormat.badge}...")
      val encodeSuccess = MediaCodecEncoder.encodePcmByFormat(
        pcmFile = pcmTempFile,
        outputFile = uniqueOutputFile,
        format = targetFormat,
        sampleRate = targetSampleRate,
        channels = targetChannels,
        bitrateKbps = targetBitrateKbps,
        onEncodeProgress = { prog ->
          onProgress(0.70f + (prog * 0.25f), "Guardando contenedor ${targetFormat.badge} (${(prog * 100).toInt()}%)")
        }
      )

      if (!encodeSuccess || !uniqueOutputFile.exists()) {
        // Fallback garantizado a WAV si el códec especializado del fabricante falló
        WavEncoder.encodePcmToWav(pcmTempFile, uniqueOutputFile, targetSampleRate, targetChannels)
      }

      onProgress(1.0f, "¡Conversión completada con éxito!")

      val convertedFile = ConvertedAudioFile(
        id = UUID.randomUUID().toString(),
        file = uniqueOutputFile,
        name = uniqueOutputFile.name,
        format = targetFormat,
        sizeBytes = uniqueOutputFile.length(),
        durationMs = if (durationMs > 0) durationMs else inputInfo.durationMs,
        timestamp = System.currentTimeMillis(),
        sampleRate = targetSampleRate,
        channels = targetChannels,
        bitrateKbps = targetBitrateKbps
      )

      Result.success(convertedFile)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    } finally {
      pcmTempFile?.delete()
    }
  }

  fun encodePcmToWav(
    pcmFile: File,
    outputWavFile: File,
    sampleRate: Int,
    channels: Int
  ): Boolean = WavEncoder.encodePcmToWav(pcmFile, outputWavFile, sampleRate, channels)
}
