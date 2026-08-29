package com.example.audio.codec

import com.example.audio.NativeAudioBridge
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Codificador y empaquetador de audio sin pérdida en contenedor estándar RIFF WAVE (WAV).
 * Usa aceleración nativa C++ mediante NDK cuando está disponible.
 */
object WavEncoder {

  fun encodePcmToWav(
    pcmFile: File,
    outputWavFile: File,
    sampleRate: Int,
    channels: Int,
    bitsPerSample: Int = 16
  ): Boolean {
    // Si la librería C++ nativa está cargada, ejecutar a nivel de máquina C++
    if (NativeAudioBridge.isAvailable()) {
      try {
        val success = NativeAudioBridge.writeWavNative(
          inputPcmPath = pcmFile.absolutePath,
          outputWavPath = outputWavFile.absolutePath,
          sampleRate = sampleRate,
          channels = channels,
          bitsPerSample = bitsPerSample
        )
        if (success && outputWavFile.exists() && outputWavFile.length() > 44) {
          return true
        }
      } catch (e: Throwable) {
        e.printStackTrace()
      }
    }

    return try {
      val pcmSize = pcmFile.length()
      val totalDataLen = pcmSize + 36
      val byteRate = (sampleRate * channels * bitsPerSample) / 8

      val outStream = FileOutputStream(outputWavFile)
      writeWavHeader(outStream, pcmSize, totalDataLen, sampleRate.toLong(), channels, byteRate.toLong(), bitsPerSample)

      val inStream = FileInputStream(pcmFile)
      val buffer = ByteArray(8192)
      var bytesRead: Int
      while (inStream.read(buffer).also { bytesRead = it } != -1) {
        outStream.write(buffer, 0, bytesRead)
      }

      inStream.close()
      outStream.flush()
      outStream.close()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  fun writeWavHeader(
    out: FileOutputStream,
    totalAudioLen: Long,
    totalDataLen: Long,
    sampleRate: Long,
    channels: Int,
    byteRate: Long,
    bitsPerSample: Int
  ) {
    val header = ByteArray(44)
    // RIFF/WAVE header
    header[0] = 'R'.code.toByte()
    header[1] = 'I'.code.toByte()
    header[2] = 'F'.code.toByte()
    header[3] = 'F'.code.toByte()
    header[4] = (totalDataLen and 0xff).toByte()
    header[5] = (totalDataLen shr 8 and 0xff).toByte()
    header[6] = (totalDataLen shr 16 and 0xff).toByte()
    header[7] = (totalDataLen shr 24 and 0xff).toByte()
    // WAVE
    header[8] = 'W'.code.toByte()
    header[9] = 'A'.code.toByte()
    header[10] = 'V'.code.toByte()
    header[11] = 'E'.code.toByte()
    // 'fmt ' chunk
    header[12] = 'f'.code.toByte()
    header[13] = 'm'.code.toByte()
    header[14] = 't'.code.toByte()
    header[15] = ' '.code.toByte()
    // 16 for PCM format size
    header[16] = 16
    header[17] = 0
    header[18] = 0
    header[19] = 0
    // Audio format 1 = PCM
    header[20] = 1
    header[21] = 0
    // Number of channels
    header[22] = channels.toByte()
    header[23] = 0
    // Sample rate
    header[24] = (sampleRate and 0xff).toByte()
    header[25] = (sampleRate shr 8 and 0xff).toByte()
    header[26] = (sampleRate shr 16 and 0xff).toByte()
    header[27] = (sampleRate shr 24 and 0xff).toByte()
    // Byte rate
    header[28] = (byteRate and 0xff).toByte()
    header[29] = (byteRate shr 8 and 0xff).toByte()
    header[30] = (byteRate shr 16 and 0xff).toByte()
    header[31] = (byteRate shr 24 and 0xff).toByte()
    // Block align (channels * bitsPerSample / 8)
    header[32] = ((channels * bitsPerSample) / 8).toByte()
    header[33] = 0
    // Bits per sample
    header[34] = bitsPerSample.toByte()
    header[35] = 0
    // 'data' chunk
    header[36] = 'd'.code.toByte()
    header[37] = 'a'.code.toByte()
    header[38] = 't'.code.toByte()
    header[39] = 'a'.code.toByte()
    header[40] = (totalAudioLen and 0xff).toByte()
    header[41] = (totalAudioLen shr 8 and 0xff).toByte()
    header[42] = (totalAudioLen shr 16 and 0xff).toByte()
    header[43] = (totalAudioLen shr 24 and 0xff).toByte()

    out.write(header, 0, 44)
  }
}
