package com.example.audio

import android.content.Context
import android.net.Uri
import com.example.audio.codec.MediaCodecEncoder
import com.example.audio.codec.PcmDecoder
import com.example.model.AudioFormat
import com.example.model.ConvertedAudioFile
import com.example.model.Spatial8DOptions
import com.example.model.Spatial8DProgress
import com.example.model.Spatial8DState
import com.example.model.SpatialTrajectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Motor de procesamiento para la generación de Audio 8D Espacial y Holofónico.
 * Aplica paneo orbital continuo 360°, retraso interaural (ITD), efecto de sombra craneal
 * y reverberación estéreo inmersiva.
 */
object Spatial8DAudioProcessor {

  suspend fun processToSpatial8D(
    context: Context,
    inputUri: Uri,
    options: Spatial8DOptions,
    onProgress: (Spatial8DProgress) -> Unit
  ): ConvertedAudioFile? = withContext(Dispatchers.IO) {
    var rawPcmFile: File? = null
    var spatialPcmFile: File? = null

    try {
      onProgress(
        Spatial8DProgress(
          state = Spatial8DState.ANALYZING,
          progressPercent = 0.05f,
          currentPhaseText = "Analizando propiedades acústicas del audio..."
        )
      )

      val metadata = AudioMetadataReader.readMetadata(context, inputUri)
      val durationMs = metadata?.durationMs ?: 0L
      val sampleRate = if ((metadata?.sampleRate ?: 0) > 0) metadata!!.sampleRate else 44100
      val srcChannels = if ((metadata?.channelCount ?: 0) > 0) metadata!!.channelCount else 2

      if (!coroutineContext.isActive) return@withContext null

      // Paso 1: Decodificar a PCM
      onProgress(
        Spatial8DProgress(
          state = Spatial8DState.DECODING_PCM,
          progressPercent = 0.15f,
          currentPhaseText = "Decodificando pistas de audio a PCM lineal...",
          totalDurationMs = durationMs
        )
      )

      val cacheDir = File(context.cacheDir, "spatial_temp").apply { mkdirs() }
      rawPcmFile = File(cacheDir, "raw_${System.currentTimeMillis()}.pcm")
      spatialPcmFile = File(cacheDir, "spatial_${System.currentTimeMillis()}.pcm")

      val decodeResult = PcmDecoder.decodeMediaToPcm(context, inputUri, rawPcmFile) { pct ->
        onProgress(
          Spatial8DProgress(
            state = Spatial8DState.DECODING_PCM,
            progressPercent = 0.15f + (pct * 0.25f),
            currentPhaseText = "Decodificando audio: ${(pct * 100).toInt()}%",
            totalDurationMs = durationMs
          )
        )
      }

      if (!decodeResult.success || !rawPcmFile.exists() || rawPcmFile.length() == 0L) {
        throw IllegalStateException("No se pudo decodificar el archivo de audio: ${decodeResult.error ?: "Error desconocido"}")
      }

      if (!coroutineContext.isActive) return@withContext null

      // Paso 2: Aplicar Algoritmos DSP 8D Espaciales
      onProgress(
        Spatial8DProgress(
          state = Spatial8DState.APPLYING_8D_DSP,
          progressPercent = 0.45f,
          currentPhaseText = "Calculando trayectoria orbital 8D y acústica binaural...",
          totalDurationMs = durationMs
        )
      )

      var dspSuccess = false

      // Intentar aceleración nativa C++
      if (NativeAudioBridge.isAvailable()) {
        try {
          val trajectoryInt = when (options.trajectory) {
            SpatialTrajectory.CIRCULAR_360 -> 0
            SpatialTrajectory.PENDULO_8 -> 1
            SpatialTrajectory.EXPANSION_3D -> 2
          }

          dspSuccess = NativeAudioBridge.process8DSpatialNative(
            inputPcmPath = rawPcmFile.absolutePath,
            outputPcmPath = spatialPcmFile.absolutePath,
            sampleRate = sampleRate,
            srcChannels = srcChannels,
            rotationSpeedHz = options.rotationSpeed.frequencyHz,
            trajectoryType = trajectoryInt,
            spatialDepth = options.spatialDepth,
            reverbRoomSize = options.reverbPreset.roomSize,
            reverbDamping = options.reverbPreset.damping,
            reverbWet = options.reverbPreset.wetLevel
          )
        } catch (e: Throwable) {
          e.printStackTrace()
          dspSuccess = false
        }
      }

      // Fallback a motor DSP Kotlin si no se usó C++ o falló
      if (!dspSuccess || !spatialPcmFile.exists() || spatialPcmFile.length() == 0L) {
        dspSuccess = applySpatialDspKotlin(
          inputPcm = rawPcmFile,
          outputPcm = spatialPcmFile,
          sampleRate = sampleRate,
          srcChannels = srcChannels,
          options = options
        ) { pct ->
          onProgress(
            Spatial8DProgress(
              state = Spatial8DState.APPLYING_8D_DSP,
              progressPercent = 0.45f + (pct * 0.30f),
              currentPhaseText = "Procesando campo espacial 8D: ${(pct * 100).toInt()}%",
              totalDurationMs = durationMs
            )
          )
        }
      }

      if (!dspSuccess || !spatialPcmFile.exists() || spatialPcmFile.length() == 0L) {
        throw IllegalStateException("Error al aplicar el procesamiento 8D al audio.")
      }

      if (!coroutineContext.isActive) return@withContext null

      // Paso 3: Codificación al formato final
      onProgress(
        Spatial8DProgress(
          state = Spatial8DState.ENCODING_OUTPUT,
          progressPercent = 0.78f,
          currentPhaseText = "Codificando a ${options.targetFormat.displayName} (${options.bitrateKbps} kbps)...",
          totalDurationMs = durationMs
        )
      )

      val targetFolder = AppStorageManager.getFolder(context, AppAudioFolder.AUDIO_8D)
      val defaultBaseName = metadata?.title?.takeIf { it.isNotBlank() }
        ?: inputUri.lastPathSegment?.substringBeforeLast(".")
        ?: "audio_8d"
      val sanitizedBase = defaultBaseName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
      val outputFileName = if (!options.customFileName.isNullOrBlank()) {
        "${options.customFileName.trim()}.${options.targetFormat.extension}"
      } else {
        "${sanitizedBase}_8D.${options.targetFormat.extension}"
      }

      val destinationFile = File(targetFolder, outputFileName)

      val encodeSuccess = MediaCodecEncoder.encodePcmByFormat(
        pcmFile = spatialPcmFile,
        outputFile = destinationFile,
        format = options.targetFormat,
        sampleRate = sampleRate,
        channels = 2, // Siempre estéreo para 8D
        bitrateKbps = options.bitrateKbps
      ) { pct ->
        onProgress(
          Spatial8DProgress(
            state = Spatial8DState.ENCODING_OUTPUT,
            progressPercent = 0.78f + (pct * 0.20f),
            currentPhaseText = "Codificando archivo final: ${(pct * 100).toInt()}%",
            totalDurationMs = durationMs
          )
        )
      }

      if (!encodeSuccess || !destinationFile.exists() || destinationFile.length() == 0L) {
        throw IllegalStateException("Error al codificar el archivo de audio 8D final.")
      }

      // Exportar e indexar en la biblioteca pública MediaStore
      AppStorageManager.exportToPublicMusicFolder(
        context = context,
        sourceFile = destinationFile,
        mimeType = options.targetFormat.mimeType,
        folderType = AppAudioFolder.AUDIO_8D,
        customDisplayName = destinationFile.name
      )

      val convertedResult = ConvertedAudioFile(
        id = destinationFile.absolutePath,
        file = destinationFile,
        name = destinationFile.name,
        format = options.targetFormat,
        sizeBytes = destinationFile.length(),
        durationMs = durationMs,
        timestamp = destinationFile.lastModified(),
        sampleRate = sampleRate,
        channels = 2,
        bitrateKbps = options.bitrateKbps
      )

      onProgress(
        Spatial8DProgress(
          state = Spatial8DState.COMPLETED,
          progressPercent = 1.0f,
          currentPhaseText = "¡Audio 8D generado con éxito!",
          totalDurationMs = durationMs,
          outputFile = convertedResult
        )
      )

      return@withContext convertedResult
    } catch (e: Exception) {
      e.printStackTrace()
      onProgress(
        Spatial8DProgress(
          state = Spatial8DState.ERROR,
          progressPercent = 0.0f,
          currentPhaseText = "Error: ${e.localizedMessage ?: "Ocurrió un error inesperado"}",
          errorMessage = e.localizedMessage ?: "Error desconocido"
        )
      )
      return@withContext null
    } finally {
      rawPcmFile?.delete()
      spatialPcmFile?.delete()
    }
  }

  /**
   * Implementación DSP en Kotlin para procesamiento binaural 8D con LFO rotativo,
   * cálculo de ITD (retardo temporal interaural) y filtro de sombra de cabeza.
   */
  private fun applySpatialDspKotlin(
    inputPcm: File,
    outputPcm: File,
    sampleRate: Int,
    srcChannels: Int,
    options: Spatial8DOptions,
    onProgressUpdate: (Float) -> Unit
  ): Boolean {
    return try {
      val inStream = BufferedInputStream(FileInputStream(inputPcm), 64 * 1024)
      val outStream = BufferedOutputStream(FileOutputStream(outputPcm), 64 * 1024)

      val totalBytes = inputPcm.length()
      var totalProcessedBytes = 0L

      val buffer = ByteArray(4096)
      var bytesRead: Int

      val rotationHz = options.rotationSpeed.frequencyHz
      val phaseIncrement = (2.0 * PI * rotationHz) / sampleRate.toDouble()
      var currentPhase = 0.0
      val depth = options.spatialDepth.coerceIn(0.1f, 1.0f).toDouble()

      // Búfer circular para retardo ITD (Interaural Time Difference)
      val itdBufferSize = 64
      val leftDelay = FloatArray(itdBufferSize)
      val rightDelay = FloatArray(itdBufferSize)
      var delayWriteIdx = 0

      // Filtro de sombra de cabeza
      var lpfLeft = 0.0f
      var lpfRight = 0.0f

      while (inStream.read(buffer).also { bytesRead = it } != -1) {
        val sampleCount = bytesRead / 2
        val inBuffer = ByteBuffer.wrap(buffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN)

        val frames = if (srcChannels == 2) sampleCount / 2 else sampleCount
        val outBytes = ByteArray(frames * 4) // 2 canales estéreo * 2 bytes
        val outBuffer = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)

        for (f in 0 until frames) {
          val inL: Float
          val inR: Float
          if (srcChannels == 2) {
            inL = inBuffer.short / 32768.0f
            inR = inBuffer.short / 32768.0f
          } else {
            val mono = inBuffer.short / 32768.0f
            inL = mono
            inR = mono
          }

          // Ángulo azimutal según la trayectoria
          val angle = currentPhase
          currentPhase += phaseIncrement
          if (currentPhase >= 2.0 * PI) currentPhase -= 2.0 * PI

          val azimuth = when (options.trajectory) {
            SpatialTrajectory.PENDULO_8 -> sin(angle) * (PI * 0.5)
            else -> angle
          }

          val sinAzimuth = sin(azimuth)
          val cosAzimuth = cos(azimuth)
          val panNormalized = ((sinAzimuth * depth + 1.0) * 0.5).coerceIn(0.0, 1.0)

          var gainLeft = cos(panNormalized * (PI * 0.5))
          var gainRight = sin(panNormalized * (PI * 0.5))

          if (cosAzimuth < 0.0) {
            val backFactor = 1.0 + (cosAzimuth * 0.15 * depth)
            gainLeft *= backFactor
            gainRight *= backFactor
          }

          // ITD Delay
          val itdSamples = (sinAzimuth * 18.0 * depth).toFloat()
          leftDelay[delayWriteIdx] = inL
          rightDelay[delayWriteIdx] = inR

          var delayedL = inL
          var delayedR = inR

          if (itdSamples > 0.0f) {
            val delayInt = itdSamples.toInt()
            val readIdx = (delayWriteIdx + itdBufferSize - delayInt) % itdBufferSize
            delayedL = leftDelay[readIdx]
          } else if (itdSamples < 0.0f) {
            val delayInt = (-itdSamples).toInt()
            val readIdx = (delayWriteIdx + itdBufferSize - delayInt) % itdBufferSize
            delayedR = rightDelay[readIdx]
          }
          delayWriteIdx = (delayWriteIdx + 1) % itdBufferSize

          // Filtro de sombra de cabeza
          val alphaL = if (sinAzimuth > 0.0) (0.25f * sinAzimuth.toFloat() * depth.toFloat()) else 0.0f
          val alphaR = if (sinAzimuth < 0.0) (0.25f * (-sinAzimuth).toFloat() * depth.toFloat()) else 0.0f

          lpfLeft = (1.0f - alphaL) * delayedL + alphaL * lpfLeft
          lpfRight = (1.0f - alphaR) * delayedR + alphaR * lpfRight

          var spatL = (lpfLeft * gainLeft).toFloat()
          var spatR = (lpfRight * gainRight).toFloat()

          // Reverberación sutil si está activada
          if (options.reverbPreset.wetLevel > 0.01f) {
            val wet = options.reverbPreset.wetLevel
            val room = options.reverbPreset.roomSize
            val mid = (spatL + spatR) * 0.5f * room * 0.25f
            spatL = spatL * (1.0f - wet * 0.5f) + mid * wet
            spatR = spatR * (1.0f - wet * 0.5f) + mid * wet
          }

          val finalL = (spatL * 32767.0f).roundToInt().coerceIn(-32768, 32767)
          val finalR = (spatR * 32767.0f).roundToInt().coerceIn(-32768, 32767)

          outBuffer.putShort(finalL.toShort())
          outBuffer.putShort(finalR.toShort())
        }

        outStream.write(outBytes)
        totalProcessedBytes += bytesRead
        if (totalBytes > 0) {
          onProgressUpdate(totalProcessedBytes.toFloat() / totalBytes.toFloat())
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
}
