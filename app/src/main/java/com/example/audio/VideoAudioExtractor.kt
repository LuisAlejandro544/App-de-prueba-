package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.example.model.AudioChannelMode
import com.example.model.AudioFileInfo
import com.example.model.AudioFormat
import com.example.model.ConversionOptions
import com.example.model.ConvertedAudioFile
import com.example.model.ExtractionMode
import com.example.model.QualityPreset
import com.example.model.VideoExtractionOptions
import com.example.model.VideoFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.coroutines.coroutineContext

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

      val outputDir = File(context.filesDir, "extracted_audio").apply { mkdirs() }
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
          // Notificar bridges nativos
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

      // Si no es extracción directa o se requiere transcodificación/ajuste acústico:
      onProgress(0.15f, "Demultiplexando pista de audio del video...")
      pcmTempFile = File(context.cacheDir, "video_extract_${UUID.randomUUID()}.pcm")

      val decodeResult = decodeVideoAudioToPcm(
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

      // Procesamiento de ganancia/canales
      onProgress(0.60f, "Optimizando fidelidad y volumen...")
      val processedPcmFile = File(context.cacheDir, "video_processed_${UUID.randomUUID()}.pcm")
      processPcmSamples(
        inputPcm = pcmTempFile,
        outputPcm = processedPcmFile,
        srcChannels = channels,
        dstChannels = targetChannels,
        volume = options.volumeMultiplier
      )
      pcmTempFile.delete()
      pcmTempFile = processedPcmFile

      // Notificar motor nativo C++ y Rust
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

      // Codificación a destino
      onProgress(0.70f, "Codificando a ${targetFormat.badge}...")
      val encodeSuccess = when (targetFormat) {
        AudioFormat.WAV, AudioFormat.AIFF -> {
          AudioTranscoder.encodePcmToWav(
            pcmFile = pcmTempFile,
            outputWavFile = uniqueOutputFile,
            sampleRate = targetSampleRate,
            channels = targetChannels
          )
        }
        else -> {
          encodePcmToFormat(
            pcmFile = pcmTempFile,
            outputFile = uniqueOutputFile,
            targetFormat = targetFormat,
            sampleRate = targetSampleRate,
            channels = targetChannels,
            bitrateKbps = targetBitrateKbps,
            onEncodeProgress = { prog ->
              onProgress(0.70f + (prog * 0.25f), "Empaquetando ${targetFormat.badge} (${(prog * 100).toInt()}%)")
            }
          )
        }
      }

      if (!encodeSuccess || !uniqueOutputFile.exists()) {
        AudioTranscoder.encodePcmToWav(pcmTempFile, uniqueOutputFile, targetSampleRate, targetChannels)
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

      if (audioTrackIndex == -1 || format == null) return@withContext false

      extractor.selectTrack(audioTrackIndex)
      muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
      val muxerTrack = muxer.addTrack(format)
      muxer.start()

      val maxBufferSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
        format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(32768)
      } else 32768

      val buffer = ByteBuffer.allocate(maxBufferSize)
      val bufferInfo = MediaCodec.BufferInfo()
      val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L

      while (coroutineContext.isActive) {
        buffer.clear()
        val sampleSize = extractor.readSampleData(buffer, 0)
        if (sampleSize < 0) break

        bufferInfo.offset = 0
        bufferInfo.size = sampleSize
        bufferInfo.presentationTimeUs = extractor.sampleTime
        bufferInfo.flags = extractor.sampleFlags

        muxer.writeSampleData(muxerTrack, buffer, bufferInfo)

        if (durationUs > 0) {
          onProgress((bufferInfo.presentationTimeUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f))
        }

        extractor.advance()
      }

      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    } finally {
      try { muxer?.stop(); muxer?.release() } catch (e: Exception) {}
      try { extractor.release() } catch (e: Exception) {}
    }
  }

  private data class DecodeResult(
    val success: Boolean,
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val durationMs: Long = 0,
    val error: String? = null
  )

  private suspend fun decodeVideoAudioToPcm(
    context: Context,
    uri: Uri,
    outputPcmFile: File,
    onProgress: (Float) -> Unit
  ): DecodeResult = withContext(Dispatchers.IO) {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    var outStream: FileOutputStream? = null

    try {
      extractor.setDataSource(context, uri, null)
      var audioTrackIndex = -1
      var audioFormat: MediaFormat? = null

      for (i in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
        if (mime.startsWith("audio/")) {
          audioTrackIndex = i
          audioFormat = format
          break
        }
      }

      if (audioTrackIndex == -1 || audioFormat == null) {
        return@withContext DecodeResult(false, error = "El video no contiene una pista de audio válida.")
      }

      extractor.selectTrack(audioTrackIndex)
      val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
      val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
        audioFormat.getLong(MediaFormat.KEY_DURATION)
      } else 0L

      decoder = MediaCodec.createDecoderByType(mime)
      decoder.configure(audioFormat, null, null, 0)
      decoder.start()

      outStream = FileOutputStream(outputPcmFile)
      val bufferInfo = MediaCodec.BufferInfo()
      var isEos = false
      var sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
        audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      } else 44100
      var channels = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
        audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      } else 2

      while (coroutineContext.isActive && !isEos) {
        val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
        if (inIndex >= 0) {
          val inBuffer = decoder.getInputBuffer(inIndex)
          if (inBuffer != null) {
            inBuffer.clear()
            val sampleSize = extractor.readSampleData(inBuffer, 0)
            if (sampleSize < 0) {
              decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              isEos = true
            } else {
              val sampleTime = extractor.sampleTime
              decoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
              extractor.advance()

              if (durationUs > 0) {
                val prog = (sampleTime.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
                onProgress(prog)
              }
            }
          }
        }

        var outIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
        while (outIndex >= 0) {
          val outBuffer = decoder.getOutputBuffer(outIndex)
          if (outBuffer != null && bufferInfo.size > 0) {
            outBuffer.position(bufferInfo.offset)
            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
            val chunk = ByteArray(bufferInfo.size)
            outBuffer.get(chunk)
            outStream.write(chunk)
          }

          decoder.releaseOutputBuffer(outIndex, false)
          if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            isEos = true
            break
          }
          outIndex = decoder.dequeueOutputBuffer(bufferInfo, 0)
        }

        if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          val newFormat = decoder.outputFormat
          if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
          }
          if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            channels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          }
        }
      }

      outStream.flush()
      DecodeResult(
        success = true,
        sampleRate = sampleRate,
        channels = channels,
        durationMs = durationUs / 1000
      )
    } catch (e: Exception) {
      e.printStackTrace()
      DecodeResult(false, error = e.localizedMessage ?: "Error decodificando audio de video")
    } finally {
      try { outStream?.close() } catch (e: Exception) {}
      try { decoder?.stop(); decoder?.release() } catch (e: Exception) {}
      try { extractor.release() } catch (e: Exception) {}
    }
  }

  private fun processPcmSamples(
    inputPcm: File,
    outputPcm: File,
    srcChannels: Int,
    dstChannels: Int,
    volume: Float
  ) {
    val inStream = FileInputStream(inputPcm)
    val outStream = BufferedOutputStream(FileOutputStream(outputPcm))
    val buffer = ByteArray(8192)
    val shortBuffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)

    var bytesRead: Int
    while (inStream.read(buffer).also { bytesRead = it } != -1) {
      val sampleCount = bytesRead / 2
      shortBuffer.clear()
      shortBuffer.put(buffer, 0, bytesRead)
      shortBuffer.flip()

      val samples = ShortArray(sampleCount)
      for (i in 0 until sampleCount) {
        samples[i] = shortBuffer.short
      }

      if (volume != 1.0f) {
        for (i in 0 until sampleCount) {
          val scaled = (samples[i] * volume).toInt()
          samples[i] = scaled.coerceIn(-32768, 32767).toShort()
        }
      }

      if (srcChannels == 2 && dstChannels == 1) {
        val monoCount = sampleCount / 2
        val monoSamples = ShortArray(monoCount)
        for (i in 0 until monoCount) {
          val l = samples[i * 2].toInt()
          val r = samples[i * 2 + 1].toInt()
          monoSamples[i] = ((l + r) / 2).toShort()
        }
        val outBytes = ByteBuffer.allocate(monoCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in monoSamples) outBytes.putShort(s)
        outStream.write(outBytes.array())
      } else if (srcChannels == 1 && dstChannels == 2) {
        val stereoCount = sampleCount * 2
        val stereoSamples = ShortArray(stereoCount)
        for (i in 0 until sampleCount) {
          val s = samples[i]
          stereoSamples[i * 2] = s
          stereoSamples[i * 2 + 1] = s
        }
        val outBytes = ByteBuffer.allocate(stereoCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in stereoSamples) outBytes.putShort(s)
        outStream.write(outBytes.array())
      } else {
        val outBytes = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) outBytes.putShort(s)
        outStream.write(outBytes.array())
      }
    }

    inStream.close()
    outStream.flush()
    outStream.close()
  }

  private suspend fun encodePcmToFormat(
    pcmFile: File,
    outputFile: File,
    targetFormat: AudioFormat,
    sampleRate: Int,
    channels: Int,
    bitrateKbps: Int,
    onEncodeProgress: (Float) -> Unit
  ): Boolean = withContext(Dispatchers.IO) {
    var encoder: MediaCodec? = null
    var muxer: MediaMuxer? = null
    var inStream: FileInputStream? = null

    try {
      val mime = when (targetFormat) {
        AudioFormat.OGG_OPUS, AudioFormat.OPUS -> MediaFormat.MIMETYPE_AUDIO_OPUS
        AudioFormat.FLAC -> MediaFormat.MIMETYPE_AUDIO_FLAC
        else -> MediaFormat.MIMETYPE_AUDIO_AAC
      }

      val outputMuxerFormat = when (targetFormat) {
        AudioFormat.OGG_OPUS, AudioFormat.OPUS -> MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG
        else -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
      }

      val format = MediaFormat.createAudioFormat(mime, sampleRate, channels)
      format.setInteger(MediaFormat.KEY_BIT_RATE, bitrateKbps * 1000)
      if (mime == MediaFormat.MIMETYPE_AUDIO_AAC) {
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
      }
      format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)

      encoder = MediaCodec.createEncoderByType(mime)
      encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      encoder.start()

      muxer = MediaMuxer(outputFile.absolutePath, outputMuxerFormat)
      var audioTrackIndex = -1
      var muxerStarted = false

      inStream = FileInputStream(pcmFile)
      val totalPcmBytes = pcmFile.length().toFloat().coerceAtLeast(1f)
      var bytesReadTotal = 0L

      val bufferInfo = MediaCodec.BufferInfo()
      val pcmBuffer = ByteArray(4096)
      var isEos = false
      var presentationTimeUs = 0L
      val bytesPerSample = channels * 2

      while (coroutineContext.isActive && !isEos) {
        val inIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
        if (inIndex >= 0) {
          val inBuffer = encoder.getInputBuffer(inIndex)
          if (inBuffer != null) {
            inBuffer.clear()
            val bytesRead = inStream.read(pcmBuffer)
            if (bytesRead <= 0) {
              encoder.queueInputBuffer(inIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              isEos = true
            } else {
              inBuffer.put(pcmBuffer, 0, bytesRead)
              encoder.queueInputBuffer(inIndex, 0, bytesRead, presentationTimeUs, 0)
              bytesReadTotal += bytesRead
              val samplesRead = bytesRead / bytesPerSample
              presentationTimeUs += (samplesRead * 1000000L) / sampleRate
              onEncodeProgress((bytesReadTotal / totalPcmBytes).coerceIn(0f, 1f))
            }
          }
        }

        var outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
        while (outIndex >= 0) {
          val outBuffer = encoder.getOutputBuffer(outIndex)
          if (outBuffer != null && bufferInfo.size > 0 && muxerStarted) {
            outBuffer.position(bufferInfo.offset)
            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
            muxer.writeSampleData(audioTrackIndex, outBuffer, bufferInfo)
          }

          encoder.releaseOutputBuffer(outIndex, false)
          if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            isEos = true
            break
          }
          outIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
        }

        if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          val newFormat = encoder.outputFormat
          audioTrackIndex = muxer.addTrack(newFormat)
          muxer.start()
          muxerStarted = true
        }
      }

      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    } finally {
      try { inStream?.close() } catch (e: Exception) {}
      try { encoder?.stop(); encoder?.release() } catch (e: Exception) {}
      try { muxer?.stop(); muxer?.release() } catch (e: Exception) {}
    }
  }
}
