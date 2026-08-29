package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.example.audio.codec.MediaCodecEncoder
import com.example.audio.codec.PcmDecoder
import com.example.audio.codec.PcmDspProcessor
import com.example.audio.codec.WavEncoder
import com.example.model.AudioChannelMode
import com.example.model.AudioFormat
import com.example.model.ConvertedAudioFile
import com.example.model.ExtractionMode
import com.example.model.QualityPreset
import com.example.model.VideoExtractionOptions
import com.example.model.VideoFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.coroutines.coroutineContext

/**
 * Motor especializado en la extracción y separación de pistas de audio desde archivos de video.
 * Soporta modo ultra-rápido directo (Passthrough) y transcodificación de alta fidelidad.
 */
object VideoAudioExtractor {

  private const val TIMEOUT_US = 10000L

  suspend fun extractAudio(
    context: Context,
    videoInfo: VideoFileInfo,
    options: VideoExtractionOptions,
    onProgress: (progress: Float, message: String) -> Unit
  ): Result<ConvertedAudioFile> = withContext(Dispatchers.IO) {
    var pcmTempFile: File? = null
    try {
      onProgress(0.05f, "Preparando contenedor de video...")

      val outputDir = AppStorageManager.getFolder(context, AppAudioFolder.VIDEO_A_AUDIO)
      val targetFormat = options.targetFormat

      val sanitizedBaseName = if (options.customFileName.isNotBlank()) {
        options.customFileName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
      } else {
        val originalName = videoInfo.name.substringBeforeLast(".")
        "${originalName}_audio"
      }

      val outputFile = File(outputDir, "$sanitizedBaseName.${targetFormat.extension}")
      val uniqueOutputFile = if (outputFile.exists()) {
        File(outputDir, "${sanitizedBaseName}_${System.currentTimeMillis().toString().takeLast(4)}.${targetFormat.extension}")
      } else {
        outputFile
      }

      // Comprobar si es posible extracción directa por stream sin pérdida (Passthrough)
      val canDirectExtract = options.mode == ExtractionMode.DIRECT_STREAM_COPY &&
        isFormatDirectlyExtractable(videoInfo.audioMimeType, targetFormat)

      if (canDirectExtract) {
        onProgress(0.20f, "Extrayendo pista de audio directa (sin pérdida)...")
        val directSuccess = extractAudioDirectStream(
          context = context,
          uri = videoInfo.uri,
          outputFile = uniqueOutputFile,
          onProgress = { prog ->
            onProgress(0.20f + (prog * 0.75f), "Extrayendo audio a velocidad ultra-rápida (${(prog * 100).toInt()}%)")
          }
        )

        if (directSuccess && uniqueOutputFile.exists() && uniqueOutputFile.length() > 0) {
          if (NativeAudioBridge.isAvailable()) {
            NativeAudioBridge.extractAudioFromVideoNative(
              videoPath = videoInfo.name,
              outputAudioPath = uniqueOutputFile.absolutePath,
              codec = targetFormat.id,
              sampleRate = videoInfo.audioSampleRate,
              channels = videoInfo.audioChannels,
              bitrateKbps = videoInfo.audioBitrateKbps,
              volumeGain = 1.0f
            )
          }

          onProgress(1.0f, "¡Audio extraído directamente con éxito!")
          val result = ConvertedAudioFile(
            id = UUID.randomUUID().toString(),
            file = uniqueOutputFile,
            name = uniqueOutputFile.name,
            format = targetFormat,
            sizeBytes = uniqueOutputFile.length(),
            durationMs = videoInfo.durationMs,
            timestamp = System.currentTimeMillis(),
            sampleRate = videoInfo.audioSampleRate,
            channels = videoInfo.audioChannels,
            bitrateKbps = videoInfo.audioBitrateKbps
          )
          return@withContext Result.success(result)
        }
      }

      // Modo transcodificación o conversión acústica personalizada
      onProgress(0.15f, "Demultiplexando pista de audio del video...")
      pcmTempFile = File(context.cacheDir, "video_extract_${UUID.randomUUID()}.pcm")

      val decodeResult = PcmDecoder.decodeMediaToPcm(
        context = context,
        uri = videoInfo.uri,
        outputPcmFile = pcmTempFile,
        onProgress = { prog ->
          onProgress(0.15f + (prog * 0.40f), "Decodificando flujo de audio (${(prog * 100).toInt()}%)")
        }
      )

      if (!decodeResult.success) {
        return@withContext Result.failure(Exception(decodeResult.error ?: "No se pudo extraer la pista de audio del video"))
      }

      val sampleRate = decodeResult.sampleRate
      val channels = decodeResult.channels
      val durationMs = if (decodeResult.durationMs > 0) decodeResult.durationMs else videoInfo.durationMs

      val targetSampleRate = when {
        options.preset == QualityPreset.ORIGINAL -> sampleRate
        options.sampleRateHz > 0 -> options.sampleRateHz.coerceAtMost(sampleRate)
        else -> sampleRate
      }

      val targetChannels = when (options.channelMode) {
        AudioChannelMode.KEEP_ORIGINAL -> channels
        AudioChannelMode.STEREO -> 2
        AudioChannelMode.MONO -> 1
      }

      val targetBitrateKbps = when {
        options.preset == QualityPreset.ORIGINAL -> videoInfo.audioBitrateKbps.coerceIn(64, 320)
        options.preset.bitrateKbps > 0 -> options.preset.bitrateKbps
        else -> options.bitrateKbps
      }

      // Procesamiento de ganancia y canales
      onProgress(0.60f, "Optimizando fidelidad y volumen...")
      val processedPcmFile = File(context.cacheDir, "video_processed_${UUID.randomUUID()}.pcm")
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

      if (NativeAudioBridge.isAvailable()) {
        NativeAudioBridge.extractAudioFromVideoNative(
          videoPath = videoInfo.name,
          outputAudioPath = uniqueOutputFile.absolutePath,
          codec = targetFormat.id,
          sampleRate = targetSampleRate,
          channels = targetChannels,
          bitrateKbps = targetBitrateKbps,
          volumeGain = options.volumeMultiplier
        )
      }

      // Codificación
      onProgress(0.70f, "Codificando a ${targetFormat.badge}...")
      val encodeSuccess = MediaCodecEncoder.encodePcmByFormat(
        pcmFile = pcmTempFile,
        outputFile = uniqueOutputFile,
        format = targetFormat,
        sampleRate = targetSampleRate,
        channels = targetChannels,
        bitrateKbps = targetBitrateKbps,
        onEncodeProgress = { prog ->
          onProgress(0.70f + (prog * 0.25f), "Empaquetando ${targetFormat.badge} (${(prog * 100).toInt()}%)")
        }
      )

      if (!encodeSuccess || !uniqueOutputFile.exists()) {
        WavEncoder.encodePcmToWav(pcmTempFile, uniqueOutputFile, targetSampleRate, targetChannels)
      }

      onProgress(1.0f, "¡Extracción de audio completada!")

      val result = ConvertedAudioFile(
        id = UUID.randomUUID().toString(),
        file = uniqueOutputFile,
        name = uniqueOutputFile.name,
        format = targetFormat,
        sizeBytes = uniqueOutputFile.length(),
        durationMs = durationMs,
        timestamp = System.currentTimeMillis(),
        sampleRate = targetSampleRate,
        channels = targetChannels,
        bitrateKbps = targetBitrateKbps
      )

      Result.success(result)
    } catch (e: Exception) {
      e.printStackTrace()
      Result.failure(e)
    } finally {
      pcmTempFile?.delete()
    }
  }

  private fun isFormatDirectlyExtractable(audioMime: String, targetFormat: AudioFormat): Boolean {
    return (audioMime.contains("mp4a-latm", ignoreCase = true) || audioMime.contains("aac", ignoreCase = true)) &&
      (targetFormat == AudioFormat.M4A_AAC || targetFormat == AudioFormat.M4R)
  }

  private suspend fun extractAudioDirectStream(
    context: Context,
    uri: Uri,
    outputFile: File,
    onProgress: (Float) -> Unit
  ): Boolean = withContext(Dispatchers.IO) {
    val extractor = MediaExtractor()
    var muxer: MediaMuxer? = null
    try {
      extractor.setDataSource(context, uri, null)
      var audioTrackIndex = -1
      var format: MediaFormat? = null

      for (i in 0 until extractor.trackCount) {
        val f = extractor.getTrackFormat(i)
        val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
        if (mime.startsWith("audio/")) {
          audioTrackIndex = i
          format = f
          break
        }
      }

      if (audioTrackIndex == -1 || format == null) {
        return@withContext false
      }

      extractor.selectTrack(audioTrackIndex)
      val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L

      muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
      val muxerTrackIndex = muxer.addTrack(format)
      muxer.start()

      val maxBufferSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
        format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
      } else {
        128 * 1024
      }

      val buffer = ByteBuffer.allocate(maxBufferSize)
      val bufferInfo = MediaCodec.BufferInfo()

      while (coroutineContext.isActive) {
        buffer.clear()
        val sampleSize = extractor.readSampleData(buffer, 0)
        if (sampleSize < 0) break

        bufferInfo.offset = 0
        bufferInfo.size = sampleSize
        bufferInfo.presentationTimeUs = extractor.sampleTime
        bufferInfo.flags = extractor.sampleFlags

        muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

        if (durationUs > 0) {
          val prog = (extractor.sampleTime.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
          onProgress(prog)
        }

        extractor.advance()
      }

      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    } finally {
      try {
        if (muxer != null) {
          muxer.stop()
          muxer.release()
        }
      } catch (ignored: Exception) {}
      try {
        extractor.release()
      } catch (ignored: Exception) {}
    }
  }
}
