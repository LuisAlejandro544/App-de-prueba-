package com.example.audio

import android.content.Context
import com.example.audio.codec.MediaCodecEncoder
import com.example.audio.codec.PcmDecoder
import com.example.audio.codec.PcmDspProcessor
import com.example.audio.codec.WavEncoder
import com.example.model.AudioChannelMode
import com.example.model.AudioFileInfo
import com.example.model.AudioFormat
import com.example.model.AudioCompressionProfile
import com.example.model.CompressionOptions
import com.example.model.CompressionPreset
import com.example.model.CompressionProgress
import com.example.model.CompressionState
import com.example.model.ConvertedAudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Motor especializado en optimización y compresión acústica de alta eficiencia.
 * Aplica reducción matemática de tasa de bits, downsampling selectivo, mezcla de canales
 * y cálculo predictivo de tamaño para mensajería y almacenamiento.
 */
object AudioCompressorProcessor {

  /**
   * Calcula el tamaño estimado de salida en bytes antes de iniciar el procesamiento.
   */
  fun estimateCompressedSizeBytes(
    durationMs: Long,
    options: CompressionOptions,
    originalSizeBytes: Long
  ): Long {
    if (durationMs <= 0L) {
      // Fallback basado en ratio aproximado según preset
      return when (options.preset) {
        CompressionPreset.MAX_SAVING -> (originalSizeBytes * 0.20f).toLong()
        CompressionPreset.BALANCED -> (originalSizeBytes * 0.45f).toLong()
        CompressionPreset.LIGHT -> (originalSizeBytes * 0.65f).toLong()
        CompressionPreset.WHATSAPP_DISCORD -> (14.5f * 1024 * 1024).toLong().coerceAtMost(originalSizeBytes)
        CompressionPreset.EMAIL_LIMIT -> (22.5f * 1024 * 1024).toLong().coerceAtMost(originalSizeBytes)
        CompressionPreset.TARGET_SIZE_CUSTOM -> (options.customTargetSizeMb * 1024 * 1024).toLong()
        CompressionPreset.CUSTOM -> (originalSizeBytes * 0.50f).toLong()
      }
    }

    val durationSec = durationMs / 1000.0
    val targetBitrateKbps = calculateEffectiveBitrate(durationMs, options)
    
    // (Bitrate en bps * segundos) / 8 + overhead de cabeceras de contenedor (~2-5 KB)
    val estimatedBytes = ((targetBitrateKbps * 1000L / 8.0) * durationSec).toLong() + 4096L
    return estimatedBytes.coerceAtLeast(1024L)
  }

  /**
   * Determina la tasa de bits (kbps) efectiva requerida según las restricciones seleccionadas.
   */
  fun calculateEffectiveBitrate(durationMs: Long, options: CompressionOptions): Int {
    val durationSec = (durationMs / 1000.0).coerceAtLeast(1.0)

    val targetSizeMb: Float? = when (options.preset) {
      CompressionPreset.WHATSAPP_DISCORD -> 15.2f
      CompressionPreset.EMAIL_LIMIT -> 23.5f
      CompressionPreset.TARGET_SIZE_CUSTOM -> options.customTargetSizeMb
      else -> null
    }

    if (targetSizeMb != null) {
      val targetBytes = targetSizeMb * 1024.0 * 1024.0 * 0.92 // 8% margen de seguridad para contenedor
      val calculatedBps = (targetBytes * 8.0) / durationSec
      val calculatedKbps = (calculatedBps / 1000.0).toInt()
      return calculatedKbps.coerceIn(24, 320)
    }

    return when (options.preset) {
      CompressionPreset.MAX_SAVING -> 48
      CompressionPreset.BALANCED -> 96
      CompressionPreset.LIGHT -> 128
      CompressionPreset.CUSTOM -> options.targetBitrateKbps.coerceIn(24, 320)
      else -> options.targetBitrateKbps.coerceIn(24, 320)
    }
  }

  /**
   * Determina la tasa de muestreo adecuada considerando el perfil acústico.
   */
  fun calculateEffectiveSampleRate(originalSampleRate: Int, options: CompressionOptions): Int {
    val maxSampleRate = if (originalSampleRate > 0) originalSampleRate else 44100
    return when (options.profile) {
      AudioCompressionProfile.SPEECH_PODCAST -> {
        when {
          options.preset == CompressionPreset.MAX_SAVING -> 22050
          maxSampleRate >= 32000 -> 32000
          else -> maxSampleRate
        }.coerceAtMost(maxSampleRate)
      }
      AudioCompressionProfile.AGGRESSIVE_SPACE -> {
        22050.coerceAtMost(maxSampleRate)
      }
      AudioCompressionProfile.MUSIC_BALANCED -> {
        if (options.preset == CompressionPreset.CUSTOM) {
          options.targetSampleRateHz.coerceAtMost(maxSampleRate)
        } else {
          44100.coerceAtMost(maxSampleRate)
        }
      }
    }
  }

  /**
   * Determina el modo de canales óptimo según el perfil y preset.
   */
  fun calculateEffectiveChannelMode(options: CompressionOptions): AudioChannelMode {
    if (options.preset == CompressionPreset.CUSTOM) {
      return options.channelMode
    }
    return when (options.profile) {
      AudioCompressionProfile.SPEECH_PODCAST, AudioCompressionProfile.AGGRESSIVE_SPACE -> AudioChannelMode.MONO
      AudioCompressionProfile.MUSIC_BALANCED -> {
        if (options.preset == CompressionPreset.MAX_SAVING) AudioChannelMode.MONO else AudioChannelMode.STEREO
      }
    }
  }

  /**
   * Ejecuta la compresión completa del archivo de audio.
   */
  suspend fun compressAudio(
    context: Context,
    inputInfo: AudioFileInfo,
    options: CompressionOptions,
    onProgress: (CompressionProgress) -> Unit
  ): Result<ConvertedAudioFile> = withContext(Dispatchers.IO) {
    var pcmTempFile: File? = null
    try {
      val originalSize = inputInfo.sizeBytes
      val estimatedSize = estimateCompressedSizeBytes(inputInfo.durationMs, options, originalSize)

      onProgress(
        CompressionProgress(
          state = CompressionState.ANALYZING,
          progressPercent = 0.05f,
          statusMessage = "Analizando audio y calculando tasa de bits...",
          originalSizeBytes = originalSize,
          estimatedSizeBytes = estimatedSize
        )
      )

      val outputDir = AppStorageManager.getFolder(context, AppAudioFolder.COMPRIMIR)
      val targetFormat = options.targetFormat

      val sanitizedBaseName = if (!options.customFileName.isNullOrBlank()) {
        options.customFileName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
      } else {
        val origName = inputInfo.name.substringBeforeLast(".")
        "${origName}_comprimido"
      }

      val outputFile = File(outputDir, "$sanitizedBaseName.${targetFormat.extension}")
      val uniqueOutputFile = if (outputFile.exists()) {
        File(outputDir, "${sanitizedBaseName}_${System.currentTimeMillis().toString().takeLast(4)}.${targetFormat.extension}")
      } else {
        outputFile
      }

      // Paso 1: Decodificar audio original a PCM
      onProgress(
        CompressionProgress(
          state = CompressionState.DECODING,
          progressPercent = 0.15f,
          statusMessage = "Extrayendo muestras de audio originales...",
          originalSizeBytes = originalSize,
          estimatedSizeBytes = estimatedSize
        )
      )

      pcmTempFile = File(context.cacheDir, "temp_compress_decode_${UUID.randomUUID()}.pcm")

      val decodeResult = PcmDecoder.decodeMediaToPcm(
        context = context,
        uri = inputInfo.uri,
        outputPcmFile = pcmTempFile,
        onProgress = { prog ->
          onProgress(
            CompressionProgress(
              state = CompressionState.DECODING,
              progressPercent = 0.15f + (prog * 0.35f),
              statusMessage = "Decodificando señal de audio (${(prog * 100).toInt()}%)",
              originalSizeBytes = originalSize,
              estimatedSizeBytes = estimatedSize
            )
          )
        }
      )

      if (!decodeResult.success) {
        val err = decodeResult.error ?: "Error al decodificar archivo de audio"
        onProgress(
          CompressionProgress(
            state = CompressionState.ERROR,
            errorMessage = err,
            originalSizeBytes = originalSize,
            estimatedSizeBytes = estimatedSize
          )
        )
        return@withContext Result.failure(Exception(err))
      }

      val srcSampleRate = decodeResult.sampleRate
      val srcChannels = decodeResult.channels
      val durationMs = if (decodeResult.durationMs > 0) decodeResult.durationMs else inputInfo.durationMs

      // Calcular parámetros acústicos óptimos para compresión
      val targetBitrateKbps = calculateEffectiveBitrate(durationMs, options)
      val targetSampleRate = calculateEffectiveSampleRate(srcSampleRate, options)
      val effectiveChannelMode = calculateEffectiveChannelMode(options)
      val targetChannels = when (effectiveChannelMode) {
        AudioChannelMode.KEEP_ORIGINAL -> srcChannels
        AudioChannelMode.STEREO -> 2
        AudioChannelMode.MONO -> 1
      }

      // Paso 2: Resampling DSP y reducción de canales PCM
      onProgress(
        CompressionProgress(
          state = CompressionState.ENCODING,
          progressPercent = 0.55f,
          statusMessage = "Optimizando ancho de banda acústico...",
          originalSizeBytes = originalSize,
          estimatedSizeBytes = estimatedSize
        )
      )

      val processedPcmFile = File(context.cacheDir, "temp_compress_processed_${UUID.randomUUID()}.pcm")
      PcmDspProcessor.resampleAndProcessPcm(
        inputPcm = pcmTempFile,
        outputPcm = processedPcmFile,
        srcSampleRate = srcSampleRate,
        dstSampleRate = targetSampleRate,
        srcChannels = srcChannels,
        dstChannels = targetChannels,
        volume = 1.0f
      )

      pcmTempFile.delete()
      pcmTempFile = processedPcmFile

      // Paso 3: Codificación de alta compresión
      onProgress(
        CompressionProgress(
          state = CompressionState.ENCODING,
          progressPercent = 0.70f,
          statusMessage = "Comprimiendo a $targetBitrateKbps kbps (${targetFormat.displayName})...",
          originalSizeBytes = originalSize,
          estimatedSizeBytes = estimatedSize
        )
      )

      val encodeSuccess = MediaCodecEncoder.encodePcmByFormat(
        pcmFile = pcmTempFile,
        outputFile = uniqueOutputFile,
        format = targetFormat,
        sampleRate = targetSampleRate,
        channels = targetChannels,
        bitrateKbps = targetBitrateKbps,
        onEncodeProgress = { prog ->
          onProgress(
            CompressionProgress(
              state = CompressionState.ENCODING,
              progressPercent = 0.70f + (prog * 0.25f),
              statusMessage = "Generando archivo comprimido (${(prog * 100).toInt()}%)",
              originalSizeBytes = originalSize,
              estimatedSizeBytes = estimatedSize
            )
          )
        }
      )

      if (!encodeSuccess || !uniqueOutputFile.exists() || uniqueOutputFile.length() == 0L) {
        // Fallback a codificación WAV si el códec falló
        WavEncoder.encodePcmToWav(pcmTempFile, uniqueOutputFile, targetSampleRate, targetChannels)
      }

      val finalSizeBytes = uniqueOutputFile.length()
      val savedBytes = (originalSize - finalSizeBytes).coerceAtLeast(0L)
      val savedPercentage = if (originalSize > 0) {
        ((savedBytes.toDouble() / originalSize.toDouble()) * 100).toInt().coerceIn(0, 99)
      } else {
        0
      }

      // Exportar e indexar en almacenamiento público
      AppStorageManager.exportToPublicMusicFolder(
        context = context,
        sourceFile = uniqueOutputFile,
        mimeType = targetFormat.mimeType,
        folderType = AppAudioFolder.COMPRIMIR,
        customDisplayName = uniqueOutputFile.name
      )

      val convertedFile = ConvertedAudioFile(
        id = UUID.randomUUID().toString(),
        file = uniqueOutputFile,
        name = uniqueOutputFile.name,
        format = targetFormat,
        sizeBytes = finalSizeBytes,
        durationMs = durationMs,
        timestamp = System.currentTimeMillis(),
        sampleRate = targetSampleRate,
        channels = targetChannels,
        bitrateKbps = targetBitrateKbps
      )

      onProgress(
        CompressionProgress(
          state = CompressionState.COMPLETED,
          progressPercent = 1.0f,
          statusMessage = "¡Compresión finalizada con éxito!",
          originalSizeBytes = originalSize,
          estimatedSizeBytes = estimatedSize,
          finalSizeBytes = finalSizeBytes,
          savedPercentage = savedPercentage,
          outputFile = uniqueOutputFile
        )
      )

      Result.success(convertedFile)
    } catch (e: Exception) {
      e.printStackTrace()
      onProgress(
        CompressionProgress(
          state = CompressionState.ERROR,
          errorMessage = e.localizedMessage ?: "Error inesperado durante la compresión"
        )
      )
      Result.failure(e)
    } finally {
      pcmTempFile?.delete()
    }
  }
}
