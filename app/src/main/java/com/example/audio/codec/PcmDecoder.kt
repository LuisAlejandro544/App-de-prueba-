package com.example.audio.codec

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

data class DecodeResult(
  val success: Boolean,
  val sampleRate: Int = 44100,
  val channels: Int = 2,
  val durationMs: Long = 0,
  val error: String? = null
)

/**
 * Decodificador universal basado en MediaExtractor y MediaCodec.
 * Extrae pistas de audio desde cualquier contenedor multimedia (MP3, AAC, FLAC, OPUS, MP4, MKV, WebM)
 * y las convierte a PCM lineal sin comprimir de 16-bit Little Endian.
 */
object PcmDecoder {

  private const val TIMEOUT_US = 10000L

  suspend fun decodeMediaToPcm(
    context: Context,
    uri: Uri,
    outputPcmFile: File,
    onProgress: (Float) -> Unit = {}
  ): DecodeResult = withContext(Dispatchers.IO) {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    var outStream: BufferedOutputStream? = null

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
        return@withContext DecodeResult(false, error = "No se encontró ninguna pista de audio en el archivo.")
      }

      extractor.selectTrack(audioTrackIndex)
      val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
      val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
        audioFormat.getLong(MediaFormat.KEY_DURATION)
      } else 0L

      val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
        audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      } else 44100

      val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
        audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      } else 2

      val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
      val decoderName = codecList.findDecoderForFormat(audioFormat)
        ?: MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(audioFormat)

      decoder = if (decoderName != null) {
        MediaCodec.createByCodecName(decoderName)
      } else {
        MediaCodec.createDecoderByType(mime)
      }

      decoder.configure(audioFormat, null, null, 0)
      decoder.start()

      outStream = BufferedOutputStream(FileOutputStream(outputPcmFile), 64 * 1024)
      val bufferInfo = MediaCodec.BufferInfo()
      var isEos = false
      var sawOutputEos = false

      var detectedSampleRate = sampleRate
      var detectedChannels = channelCount

      while (!sawOutputEos && coroutineContext.isActive) {
        if (!isEos) {
          val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
          if (inIndex >= 0) {
            val inBuffer = decoder.getInputBuffer(inIndex)
            if (inBuffer != null) {
              val sampleSize = extractor.readSampleData(inBuffer, 0)
              if (sampleSize < 0) {
                decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
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
        }

        val outIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
        if (outIndex >= 0) {
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
            sawOutputEos = true
          }
        } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          val newFormat = decoder.outputFormat
          if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            detectedSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
          }
          if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            detectedChannels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          }
        }
      }

      outStream.flush()

      DecodeResult(
        success = true,
        sampleRate = detectedSampleRate,
        channels = detectedChannels,
        durationMs = durationUs / 1000L
      )
    } catch (e: Exception) {
      e.printStackTrace()
      DecodeResult(false, error = e.message ?: "Error desconocido durante la decodificación")
    } finally {
      try {
        decoder?.stop()
        decoder?.release()
      } catch (ignored: Exception) {}
      try {
        extractor.release()
      } catch (ignored: Exception) {}
      try {
        outStream?.close()
      } catch (ignored: Exception) {}
    }
  }
}
