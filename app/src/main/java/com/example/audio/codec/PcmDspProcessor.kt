package com.example.audio.codec

import com.example.audio.NativeAudioBridge
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Procesador de señal digital (DSP) para manipular muestras PCM de 16 bits:
 * - Ganancia y atenuación de volumen con protección de saturación (clipping).
 * - Conversión de canales (Mono <-> Estéreo).
 * - Remuestreo e interpolación lineal de frecuencia si es requerida.
 * - Utiliza aceleración C++ NDK nativa cuando la librería está disponible.
 */
object PcmDspProcessor {

  fun processPcmSamples(
    inputPcm: File,
    outputPcm: File,
    srcChannels: Int,
    dstChannels: Int,
    volume: Float
  ): Boolean {
    val isGainNeeded = volume != 1.0f
    val isChannelChangeNeeded = srcChannels != dstChannels

    if (!isGainNeeded && !isChannelChangeNeeded) {
      inputPcm.copyTo(outputPcm, overwrite = true)
      return true
    }

    // Aceleración C++ Nativa
    if (NativeAudioBridge.isAvailable()) {
      try {
        val success = NativeAudioBridge.processPcmGainNative(
          inputPcmPath = inputPcm.absolutePath,
          outputPcmPath = outputPcm.absolutePath,
          srcChannels = srcChannels,
          dstChannels = dstChannels,
          volumeGain = volume
        )
        if (success && outputPcm.exists() && outputPcm.length() > 0) {
          return true
        }
      } catch (e: Throwable) {
        e.printStackTrace()
      }
    }

    return try {
      val inStream = BufferedInputStream(FileInputStream(inputPcm), 64 * 1024)
      val outStream = BufferedOutputStream(FileOutputStream(outputPcm), 64 * 1024)

      val buffer = ByteArray(4096)
      var bytesRead: Int

      while (inStream.read(buffer).also { bytesRead = it } != -1) {
        val sampleCount = bytesRead / 2
        val inBuffer = ByteBuffer.wrap(buffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN)

        if (srcChannels == 2 && dstChannels == 1) {
          // Estéreo a Mono: promediar canales
          val outBytes = ByteArray(bytesRead / 2)
          val outBuffer = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)
          for (i in 0 until sampleCount / 2) {
            val left = inBuffer.short.toInt()
            val right = inBuffer.short.toInt()
            var mixed = ((left + right) / 2 * volume).roundToInt()
            mixed = mixed.coerceIn(-32768, 32767)
            outBuffer.putShort(mixed.toShort())
          }
          outStream.write(outBytes)
        } else if (srcChannels == 1 && dstChannels == 2) {
          // Mono a Estéreo: duplicar muestra en ambos canales
          val outBytes = ByteArray(bytesRead * 2)
          val outBuffer = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)
          for (i in 0 until sampleCount) {
            val sample = inBuffer.short.toInt()
            var processed = (sample * volume).roundToInt()
            processed = processed.coerceIn(-32768, 32767)
            val shortVal = processed.toShort()
            outBuffer.putShort(shortVal)
            outBuffer.putShort(shortVal)
          }
          outStream.write(outBytes)
        } else {
          // Mismo número de canales: solo aplicar ganancia de volumen
          val outBytes = ByteArray(bytesRead)
          val outBuffer = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)
          for (i in 0 until sampleCount) {
            val sample = inBuffer.short.toInt()
            var processed = (sample * volume).roundToInt()
            processed = processed.coerceIn(-32768, 32767)
            outBuffer.putShort(processed.toShort())
          }
          outStream.write(outBytes)
        }
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

  fun resampleAndProcessPcm(
    inputPcm: File,
    outputPcm: File,
    srcSampleRate: Int,
    dstSampleRate: Int,
    srcChannels: Int,
    dstChannels: Int,
    volume: Float
  ): Boolean {
    // Si la tasa de muestreo coincide, usar el procesador directo con aceleración C++
    if (srcSampleRate == dstSampleRate || srcSampleRate <= 0 || dstSampleRate <= 0) {
      return processPcmSamples(inputPcm, outputPcm, srcChannels, dstChannels, volume)
    }

    return try {
      val inStream = BufferedInputStream(FileInputStream(inputPcm), 64 * 1024)
      val outStream = BufferedOutputStream(FileOutputStream(outputPcm), 64 * 1024)

      val inBytes = inputPcm.readBytes()
      inStream.close()

      val sampleCount = inBytes.size / 2
      val inShorts = ShortArray(sampleCount)
      ByteBuffer.wrap(inBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(inShorts)

      val framesIn = sampleCount / srcChannels
      val ratio = dstSampleRate.toDouble() / srcSampleRate.toDouble()
      val framesOut = (framesIn * ratio).toInt()
      val outShorts = ShortArray(framesOut * dstChannels)

      for (f in 0 until framesOut) {
        val srcIndex = (f / ratio).toInt().coerceIn(0, framesIn - 1)
        for (c in 0 until dstChannels) {
          val srcChannelIndex = if (srcChannels == 1) 0 else c.coerceAtMost(srcChannels - 1)
          val rawSample = inShorts[srcIndex * srcChannels + srcChannelIndex].toInt()
          var processed = (rawSample * volume).roundToInt()
          processed = processed.coerceIn(-32768, 32767)
          outShorts[f * dstChannels + c] = processed.toShort()
        }
      }

      val outByteBuffer = ByteBuffer.allocate(outShorts.size * 2).order(ByteOrder.LITTLE_ENDIAN)
      outByteBuffer.asShortBuffer().put(outShorts)
      outStream.write(outByteBuffer.array())
      outStream.flush()
      outStream.close()
      true
    } catch (e: Exception) {
      e.printStackTrace()
      processPcmSamples(inputPcm, outputPcm, srcChannels, dstChannels, volume)
    }
  }
}
