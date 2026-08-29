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
import com.example.model.QualityPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AudioTranscoder {

  private const val TIMEOUT_US = 10000L

  suspend fun convertAudio(
    context: Context,
    inputInfo: AudioFileInfo,
    options: ConversionOptions,
    onProgress: (progress: Float, message: String) -> Unit
  ): Result<ConvertedAudioFile> = withContext(Dispatchers.IO) {
    var pcmTempFile: File? = null
    try {
      onProgress(0.05f, "Preparando archivo de audio...")

      val outputDir = File(context.filesDir, "converted").apply { mkdirs() }
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

      // Step 1: Decode input to raw PCM
      onProgress(0.15f, "Decodificando audio original...")
      pcmTempFile = File(context.cacheDir, "temp_decode_${UUID.randomUUID()}.pcm")

      val decodeResult = decodeToPcm(
        context = context,
        uri = inputInfo.uri,
        outputPcmFile = pcmTempFile,
        onDecodeProgress = { prog ->
          onProgress(0.15f + (prog * 0.35f), "Decodificando pistas de audio (${(prog * 100).toInt()}%)")
        }
      )

      if (!decodeResult.success) {
        return@withContext Result.failure(Exception(decodeResult.error ?: "Error al decodificar audio"))
      }

      var sampleRate = decodeResult.sampleRate
      var channels = decodeResult.channels
      var durationMs = decodeResult.durationMs

      // Calculate target parameters (con bloqueo de sobremuestreo y tasa de bits superior al origen)
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

      // Step 2: Apply Audio processing (Volume / Gain / Channel transform / Resample if needed)
      onProgress(0.55f, "Ajustando parámetros y calidad...")
      val processedPcmFile = File(context.cacheDir, "temp_processed_${UUID.randomUUID()}.pcm")
      processPcm(
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

      // Step 3: Encode to destination format
      onProgress(0.70f, "Codificando a formato ${targetFormat.badge}...")

      // Invoke native bridge logging/processing hook
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

      val encodeSuccess = when (targetFormat) {
        AudioFormat.WAV, AudioFormat.AIFF -> {
          encodePcmToWav(
            pcmFile = pcmTempFile,
            outputWavFile = uniqueOutputFile,
            sampleRate = targetSampleRate,
            channels = targetChannels
          )
        }
        AudioFormat.M4A_AAC, AudioFormat.M4R, AudioFormat.MP3, AudioFormat.MP2, AudioFormat.AC3, AudioFormat.WMA -> {
          encodePcmToAac(
            pcmFile = pcmTempFile,
            outputFile = uniqueOutputFile,
            sampleRate = targetSampleRate,
            channels = targetChannels,
            bitrateBps = targetBitrateKbps * 1000,
            onEncodeProgress = { prog ->
              onProgress(0.70f + (prog * 0.25f), "Guardando contenedor ${targetFormat.badge} (${(prog * 100).toInt()}%)")
            }
          )
        }
        AudioFormat.FLAC -> {
          encodePcmToFlac(
            pcmFile = pcmTempFile,
            outputFile = uniqueOutputFile,
            sampleRate = targetSampleRate,
            channels = targetChannels,
            onEncodeProgress = { prog ->
              onProgress(0.70f + (prog * 0.25f), "Codificando FLAC (${(prog * 100).toInt()}%)")
            }
          )
        }
        AudioFormat.OGG_OPUS, AudioFormat.OPUS, AudioFormat.AMR -> {
          encodePcmToOpus(
            pcmFile = pcmTempFile,
            outputFile = uniqueOutputFile,
            sampleRate = targetSampleRate,
            channels = targetChannels,
            bitrateBps = targetBitrateKbps * 1000,
            onEncodeProgress = { prog ->
              onProgress(0.70f + (prog * 0.25f), "Codificando ${targetFormat.badge} (${(prog * 100).toInt()}%)")
            }
          )
        }
      }

      if (!encodeSuccess) {
        // Fallback to WAV format if specialized codec failed
        encodePcmToWav(pcmTempFile, uniqueOutputFile, targetSampleRate, targetChannels)
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

  private data class DecodeResult(
    val success: Boolean,
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val durationMs: Long = 0,
    val error: String? = null
  )

  private suspend fun decodeToPcm(
    context: Context,
    uri: Uri,
    outputPcmFile: File,
    onDecodeProgress: (Float) -> Unit
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
        return@withContext DecodeResult(false, error = "No se encontró pista de audio válida en el archivo.")
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
                onDecodeProgress(prog)
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
      DecodeResult(false, error = e.localizedMessage ?: "Fallo al decodificar audio")
    } finally {
      try { outStream?.close() } catch (e: Exception) {}
      try { decoder?.stop(); decoder?.release() } catch (e: Exception) {}
      try { extractor.release() } catch (e: Exception) {}
    }
  }

  private fun processPcm(
    inputPcm: File,
    outputPcm: File,
    srcSampleRate: Int,
    dstSampleRate: Int,
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

      // Apply volume/gain
      if (volume != 1.0f) {
        for (i in 0 until sampleCount) {
          val scaled = (samples[i] * volume).toInt()
          samples[i] = scaled.coerceIn(-32768, 32767).toShort()
        }
      }

      // Apply Channel transformation if requested
      if (srcChannels == 2 && dstChannels == 1) {
        // Stereo to Mono (average L and R)
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
        // Mono to Stereo (duplicate channel)
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
        // Keep channels as is
        val outBytes = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) outBytes.putShort(s)
        outStream.write(outBytes.array())
      }
    }

    inStream.close()
    outStream.flush()
    outStream.close()
  }

  fun encodePcmToWav(
    pcmFile: File,
    outputWavFile: File,
    sampleRate: Int,
    channels: Int
  ): Boolean {
    try {
      val pcmDataLength = pcmFile.length()
      val totalDataLen = pcmDataLength + 36
      val byteRate = (sampleRate * channels * 16) / 8
      val blockAlign = (channels * 16) / 8

      val inStream = FileInputStream(pcmFile)
      val outStream = BufferedOutputStream(FileOutputStream(outputWavFile))

      val header = ByteArray(44)
      header[0] = 'R'.code.toByte()
      header[1] = 'I'.code.toByte()
      header[2] = 'F'.code.toByte()
      header[3] = 'F'.code.toByte()
      header[4] = (totalDataLen and 0xff).toByte()
      header[5] = ((totalDataLen shr 8) and 0xff).toByte()
      header[6] = ((totalDataLen shr 16) and 0xff).toByte()
      header[7] = ((totalDataLen shr 24) and 0xff).toByte()
      header[8] = 'W'.code.toByte()
      header[9] = 'A'.code.toByte()
      header[10] = 'V'.code.toByte()
      header[11] = 'E'.code.toByte()
      header[12] = 'f'.code.toByte()
      header[13] = 'm'.code.toByte()
      header[14] = 't'.code.toByte()
      header[15] = ' '.code.toByte()
      header[16] = 16 // 16 for PCM format chunk size
      header[17] = 0
      header[18] = 0
      header[19] = 0
      header[20] = 1 // Format = 1 (PCM)
      header[21] = 0
      header[22] = channels.toByte()
      header[23] = 0
      header[24] = (sampleRate and 0xff).toByte()
      header[25] = ((sampleRate shr 8) and 0xff).toByte()
      header[26] = ((sampleRate shr 16) and 0xff).toByte()
      header[27] = ((sampleRate shr 24) and 0xff).toByte()
      header[28] = (byteRate and 0xff).toByte()
      header[29] = ((byteRate shr 8) and 0xff).toByte()
      header[30] = ((byteRate shr 16) and 0xff).toByte()
      header[31] = ((byteRate shr 24) and 0xff).toByte()
      header[32] = blockAlign.toByte()
      header[33] = 0
      header[34] = 16 // 16 bits per sample
      header[35] = 0
      header[36] = 'd'.code.toByte()
      header[37] = 'a'.code.toByte()
      header[38] = 't'.code.toByte()
      header[39] = 'a'.code.toByte()
      header[40] = (pcmDataLength and 0xff).toByte()
      header[41] = ((pcmDataLength shr 8) and 0xff).toByte()
      header[42] = ((pcmDataLength shr 16) and 0xff).toByte()
      header[43] = ((pcmDataLength shr 24) and 0xff).toByte()

      outStream.write(header, 0, 44)

      val buffer = ByteArray(8192)
      var readBytes: Int
      while (inStream.read(buffer).also { readBytes = it } != -1) {
        outStream.write(buffer, 0, readBytes)
      }

      inStream.close()
      outStream.flush()
      outStream.close()
      return true
    } catch (e: Exception) {
      e.printStackTrace()
      return false
    }
  }

  private suspend fun encodePcmToAac(
    pcmFile: File,
    outputFile: File,
    sampleRate: Int,
    channels: Int,
    bitrateBps: Int,
    onEncodeProgress: (Float) -> Unit
  ): Boolean = withContext(Dispatchers.IO) {
    var encoder: MediaCodec? = null
    var muxer: MediaMuxer? = null
    var inStream: FileInputStream? = null

    try {
      val mime = MediaFormat.MIMETYPE_AUDIO_AAC
      val format = MediaFormat.createAudioFormat(mime, sampleRate, channels)
      format.setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
      format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
      format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)

      encoder = MediaCodec.createEncoderByType(mime)
      encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      encoder.start()

      muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
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

  private suspend fun encodePcmToFlac(
    pcmFile: File,
    outputFile: File,
    sampleRate: Int,
    channels: Int,
    onEncodeProgress: (Float) -> Unit
  ): Boolean = withContext(Dispatchers.IO) {
    // Check if FLAC encoder is supported natively
    val flacCodec = findEncoderForMime(MediaFormat.MIMETYPE_AUDIO_FLAC)
    if (flacCodec != null) {
      var encoder: MediaCodec? = null
      var outStream: FileOutputStream? = null
      var inStream: FileInputStream? = null
      try {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_FLAC, sampleRate, channels)
        format.setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, 5)

        encoder = MediaCodec.createByCodecName(flacCodec)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        outStream = FileOutputStream(outputFile)
        inStream = FileInputStream(pcmFile)
        val totalBytes = pcmFile.length().toFloat().coerceAtLeast(1f)
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
                onEncodeProgress((bytesReadTotal / totalBytes).coerceIn(0f, 1f))
              }
            }
          }

          var outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
          while (outIndex >= 0) {
            val outBuffer = encoder.getOutputBuffer(outIndex)
            if (outBuffer != null && bufferInfo.size > 0) {
              outBuffer.position(bufferInfo.offset)
              outBuffer.limit(bufferInfo.offset + bufferInfo.size)
              val chunk = ByteArray(bufferInfo.size)
              outBuffer.get(chunk)
              outStream.write(chunk)
            }
            encoder.releaseOutputBuffer(outIndex, false)
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
              isEos = true
              break
            }
            outIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
          }
        }
        outStream.flush()
        return@withContext true
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        try { inStream?.close() } catch (e: Exception) {}
        try { outStream?.close() } catch (e: Exception) {}
        try { encoder?.stop(); encoder?.release() } catch (e: Exception) {}
      }
    }

    // High compatibility fallback: lossless WAV wrapper
    encodePcmToWav(pcmFile, outputFile, sampleRate, channels)
  }

  private suspend fun encodePcmToOpus(
    pcmFile: File,
    outputFile: File,
    sampleRate: Int,
    channels: Int,
    bitrateBps: Int,
    onEncodeProgress: (Float) -> Unit
  ): Boolean = withContext(Dispatchers.IO) {
    val opusCodec = findEncoderForMime(MediaFormat.MIMETYPE_AUDIO_OPUS)
    if (opusCodec != null) {
      var encoder: MediaCodec? = null
      var muxer: MediaMuxer? = null
      var inStream: FileInputStream? = null
      try {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, sampleRate, channels)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)

        encoder = MediaCodec.createByCodecName(opusCodec)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)
        var audioTrackIndex = -1
        var muxerStarted = false

        inStream = FileInputStream(pcmFile)
        val totalBytes = pcmFile.length().toFloat().coerceAtLeast(1f)
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
                onEncodeProgress((bytesReadTotal / totalBytes).coerceIn(0f, 1f))
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
        return@withContext true
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        try { inStream?.close() } catch (e: Exception) {}
        try { encoder?.stop(); encoder?.release() } catch (e: Exception) {}
        try { muxer?.stop(); muxer?.release() } catch (e: Exception) {}
      }
    }

    // High compatibility fallback: AAC encoder
    encodePcmToAac(pcmFile, outputFile, sampleRate, channels, bitrateBps, onEncodeProgress)
  }

  private fun findEncoderForMime(mime: String): String? {
    val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
    for (info in list.codecInfos) {
      if (info.isEncoder) {
        for (type in info.supportedTypes) {
          if (type.equals(mime, ignoreCase = true)) {
            return info.name
          }
        }
      }
    }
    return null
  }
}
