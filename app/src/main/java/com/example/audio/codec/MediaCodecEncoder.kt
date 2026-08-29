package com.example.audio.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.model.AudioFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

/**
 * Codificador y multiplexor por hardware/software que transforma PCM crudo
 * en contenedores M4A (AAC), FLAC y Ogg/Opus utilizando MediaCodec y MediaMuxer.
 */
object MediaCodecEncoder {

  private const val TIMEOUT_US = 10000L

  suspend fun encodePcmToAac(
    pcmFile: File,
    outputFile: File,
    sampleRate: Int,
    channels: Int,
    bitrateBps: Int,
    onEncodeProgress: (Float) -> Unit = {}
  ): Boolean = encodePcmToMediaCodec(
    pcmFile = pcmFile,
    outputFile = outputFile,
    mimeType = MediaFormat.MIMETYPE_AUDIO_AAC,
    muxerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
    sampleRate = sampleRate,
    channels = channels,
    bitrateBps = bitrateBps,
    aacProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC,
    onEncodeProgress = onEncodeProgress
  )

  suspend fun encodePcmToFlac(
    pcmFile: File,
    outputFile: File,
    sampleRate: Int,
    channels: Int,
    onEncodeProgress: (Float) -> Unit = {}
  ): Boolean = encodePcmToMediaCodec(
    pcmFile = pcmFile,
    outputFile = outputFile,
    mimeType = MediaFormat.MIMETYPE_AUDIO_FLAC,
    muxerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG,
    sampleRate = sampleRate,
    channels = channels,
    bitrateBps = 0,
    onEncodeProgress = onEncodeProgress
  )

  suspend fun encodePcmToOpus(
    pcmFile: File,
    outputFile: File,
    sampleRate: Int,
    channels: Int,
    bitrateBps: Int,
    onEncodeProgress: (Float) -> Unit = {}
  ): Boolean = encodePcmToMediaCodec(
    pcmFile = pcmFile,
    outputFile = outputFile,
    mimeType = MediaFormat.MIMETYPE_AUDIO_OPUS,
    muxerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG,
    sampleRate = sampleRate,
    channels = channels,
    bitrateBps = bitrateBps,
    onEncodeProgress = onEncodeProgress
  )

  suspend fun encodePcmByFormat(
    pcmFile: File,
    outputFile: File,
    format: AudioFormat,
    sampleRate: Int,
    channels: Int,
    bitrateKbps: Int,
    onEncodeProgress: (Float) -> Unit = {}
  ): Boolean {
    return when (format) {
      AudioFormat.WAV, AudioFormat.AIFF -> {
        WavEncoder.encodePcmToWav(pcmFile, outputFile, sampleRate, channels)
      }
      AudioFormat.FLAC -> {
        encodePcmToFlac(pcmFile, outputFile, sampleRate, channels, onEncodeProgress)
      }
      AudioFormat.OGG_OPUS, AudioFormat.OPUS, AudioFormat.AMR -> {
        encodePcmToOpus(pcmFile, outputFile, sampleRate, channels, bitrateKbps * 1000, onEncodeProgress)
      }
      else -> {
        // M4A, AAC, MP3, MP2, AC3, WMA empaquetados en contenedor de alta compatibilidad M4A / AAC
        encodePcmToAac(pcmFile, outputFile, sampleRate, channels, bitrateKbps * 1000, onEncodeProgress)
      }
    }
  }

  suspend fun encodePcmToMediaCodec(
    pcmFile: File,
    outputFile: File,
    mimeType: String,
    muxerFormat: Int,
    sampleRate: Int,
    channels: Int,
    bitrateBps: Int = 0,
    aacProfile: Int = -1,
    onEncodeProgress: (Float) -> Unit = {}
  ): Boolean = withContext(Dispatchers.IO) {
    var encoder: MediaCodec? = null
    var muxer: MediaMuxer? = null
    var inStream: FileInputStream? = null

    try {
      val format = MediaFormat.createAudioFormat(mimeType, sampleRate, channels)
      if (bitrateBps > 0) {
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
      }
      if (aacProfile > 0) {
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, aacProfile)
      }

      val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
      val encoderName = codecList.findEncoderForFormat(format)
        ?: MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(format)

      encoder = if (encoderName != null) {
        MediaCodec.createByCodecName(encoderName)
      } else {
        MediaCodec.createEncoderByType(mimeType)
      }

      encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      encoder.start()

      muxer = MediaMuxer(outputFile.absolutePath, muxerFormat)
      var audioTrackIndex = -1
      var muxerStarted = false

      inStream = FileInputStream(pcmFile)
      val totalBytes = pcmFile.length()
      var bytesProcessed = 0L

      val bufferInfo = MediaCodec.BufferInfo()
      val inputBuffer = ByteArray(4096)
      var isInputEos = false
      var isOutputEos = false
      var presentationTimeUs = 0L
      val bytesPerSample = 2 * channels

      while (!isOutputEos && coroutineContext.isActive) {
        if (!isInputEos) {
          val inIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
          if (inIndex >= 0) {
            val codecBuffer = encoder.getInputBuffer(inIndex)
            if (codecBuffer != null) {
              codecBuffer.clear()
              val read = inStream.read(inputBuffer)
              if (read <= 0) {
                encoder.queueInputBuffer(inIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isInputEos = true
              } else {
                codecBuffer.put(inputBuffer, 0, read)
                encoder.queueInputBuffer(inIndex, 0, read, presentationTimeUs, 0)
                bytesProcessed += read
                val sampleFrames = read / bytesPerSample
                presentationTimeUs += (sampleFrames * 1_000_000L) / sampleRate

                if (totalBytes > 0) {
                  onEncodeProgress((bytesProcessed.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                }
              }
            }
          }
        }

        val outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
        if (outIndex >= 0) {
          val outBuffer = encoder.getOutputBuffer(outIndex)
          if (outBuffer != null && bufferInfo.size > 0 && muxerStarted) {
            outBuffer.position(bufferInfo.offset)
            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
            muxer.writeSampleData(audioTrackIndex, outBuffer, bufferInfo)
          }
          encoder.releaseOutputBuffer(outIndex, false)

          if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            isOutputEos = true
          }
        } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          if (!muxerStarted) {
            val newFormat = encoder.outputFormat
            audioTrackIndex = muxer.addTrack(newFormat)
            muxer.start()
            muxerStarted = true
          }
        }
      }

      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    } finally {
      try {
        encoder?.stop()
        encoder?.release()
      } catch (ignored: Exception) {}
      try {
        if (muxer != null) {
          muxer.stop()
          muxer.release()
        }
      } catch (ignored: Exception) {}
      try {
        inStream?.close()
      } catch (ignored: Exception) {}
    }
  }
}
