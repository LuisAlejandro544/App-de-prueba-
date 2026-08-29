package com.example.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.audio.codec.MediaCodecEncoder
import com.example.audio.codec.PcmDecoder
import com.example.audio.codec.WavEncoder
import com.example.model.AudioFormat
import com.example.model.SilenceCutMode
import com.example.model.SilenceProgress
import com.example.model.SilenceRemoverOptions
import com.example.model.SilenceState
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Segmento temporal clasificado de audio (rango de frames).
 */
data class AudioSegmentRange(
  val startFrame: Long,
  val endFrame: Long,
  val isVoice: Boolean
)

/**
 * Procesador DSP inteligente para detectar y eliminar silencios o acelerar pausas.
 */
object SilenceRemoverProcessor {

  private const val TAG = "SilenceRemoverProcessor"
  private const val WINDOW_MS = 25L // Ventana de 25ms para cálculo de energía RMS

  suspend fun processSilenceRemoval(
    context: Context,
    sourceUri: Uri,
    options: SilenceRemoverOptions,
    outputFile: File,
    onProgress: (SilenceProgress) -> Unit
  ): SilenceProgress = withContext(Dispatchers.IO) {
    var rawPcmFile: File? = null
    var processedPcmFile: File? = null

    try {
      onProgress(
        SilenceProgress(
          state = SilenceState.DECODING,
          progress = 0.05f,
          statusMessage = "Decodificando audio a PCM lineal..."
        )
      )

      rawPcmFile = File.createTempFile("raw_silence_in_", ".pcm", context.cacheDir)
      val decodeResult = PcmDecoder.decodeMediaToPcm(
        context = context,
        uri = sourceUri,
        outputPcmFile = rawPcmFile
      ) { decProg ->
        onProgress(
          SilenceProgress(
            state = SilenceState.DECODING,
            progress = 0.05f + (decProg * 0.35f),
            statusMessage = "Decodificando: ${(decProg * 100).toInt()}%"
          )
        )
      }

      if (!decodeResult.success || !rawPcmFile.exists() || rawPcmFile.length() <= 0) {
        return@withContext SilenceProgress(
          state = SilenceState.ERROR,
          errorMessage = decodeResult.error ?: "No se pudieron extraer datos de audio del archivo seleccionado."
        )
      }

      val sampleRate = decodeResult.sampleRate
      val channelCount = decodeResult.channels
      val bytesPerFrame = channelCount * 2
      val totalFrames = rawPcmFile.length() / bytesPerFrame
      val originalDurationMs = if (sampleRate > 0) (totalFrames * 1000L) / sampleRate else decodeResult.durationMs

      onProgress(
        SilenceProgress(
          state = SilenceState.ANALYZING_DSP,
          progress = 0.45f,
          statusMessage = "Analizando niveles de energía y silencios...",
          originalDurationMs = originalDurationMs
        )
      )

      // Paso 1: Analizar frames y clasificar segmentos
      val segments = analyzeSegments(
        pcmFile = rawPcmFile,
        sampleRate = sampleRate,
        channelCount = channelCount,
        thresholdDb = options.thresholdLevel.dbThreshold,
        minSilenceMs = options.minSilenceDurationMs,
        paddingMs = options.paddingVoiceMs
      )

      val silenceSegmentsCount = segments.count { !it.isVoice }

      // Paso 2: Generar nuevo flujo PCM aplicando corte o aceleración
      onProgress(
        SilenceProgress(
          state = SilenceState.CUTTING_AUDIO,
          progress = 0.60f,
          statusMessage = "Empalmando audio y removiendo pausas...",
          originalDurationMs = originalDurationMs,
          segmentsCut = silenceSegmentsCount
        )
      )

      processedPcmFile = File.createTempFile("cut_silence_out_", ".pcm", context.cacheDir)
      val processedFrames = buildProcessedPcm(
        inputPcm = rawPcmFile,
        outputPcm = processedPcmFile,
        segments = segments,
        channelCount = channelCount,
        mode = options.mode
      )

      val finalDurationMs = if (sampleRate > 0) (processedFrames * 1000L) / sampleRate else originalDurationMs
      val savedDurationMs = max(0L, originalDurationMs - finalDurationMs)
      val savedPercentage = if (originalDurationMs > 0) {
        ((savedDurationMs.toFloat() / originalDurationMs.toFloat()) * 100).toInt()
      } else 0

      // Paso 3: Codificación al formato seleccionado
      onProgress(
        SilenceProgress(
          state = SilenceState.ENCODING,
          progress = 0.80f,
          statusMessage = "Codificando a ${options.targetFormat.displayName}...",
          originalDurationMs = originalDurationMs,
          finalDurationMs = finalDurationMs,
          savedDurationMs = savedDurationMs,
          savedPercentage = savedPercentage,
          segmentsCut = silenceSegmentsCount
        )
      )

      val encodeSuccess = MediaCodecEncoder.encodePcmByFormat(
        pcmFile = processedPcmFile,
        outputFile = outputFile,
        format = options.targetFormat,
        sampleRate = sampleRate,
        channels = channelCount,
        bitrateKbps = options.bitrateKbps
      ) { encProg ->
        onProgress(
          SilenceProgress(
            state = SilenceState.ENCODING,
            progress = 0.80f + (encProg * 0.18f),
            statusMessage = "Codificando: ${(encProg * 100).toInt()}%",
            originalDurationMs = originalDurationMs,
            finalDurationMs = finalDurationMs,
            savedDurationMs = savedDurationMs,
            savedPercentage = savedPercentage,
            segmentsCut = silenceSegmentsCount
          )
        )
      }

      if (!encodeSuccess) {
        // Fallback a WAV
        WavEncoder.encodePcmToWav(
          pcmFile = processedPcmFile,
          outputWavFile = outputFile,
          sampleRate = sampleRate,
          channels = channelCount
        )
      }

      return@withContext SilenceProgress(
        state = SilenceState.COMPLETED,
        progress = 1.0f,
        statusMessage = "¡Audio sin silencios generado con éxito!",
        originalDurationMs = originalDurationMs,
        finalDurationMs = finalDurationMs,
        savedDurationMs = savedDurationMs,
        savedPercentage = savedPercentage,
        segmentsCut = silenceSegmentsCount,
        outputFile = outputFile
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error en SilenceRemoverProcessor: ${e.message}", e)
      return@withContext SilenceProgress(
        state = SilenceState.ERROR,
        errorMessage = "Error durante el procesamiento: ${e.localizedMessage ?: e.message}"
      )
    } finally {
      rawPcmFile?.delete()
      processedPcmFile?.delete()
    }
  }

  private fun analyzeSegments(
    pcmFile: File,
    sampleRate: Int,
    channelCount: Int,
    thresholdDb: Float,
    minSilenceMs: Long,
    paddingMs: Long
  ): List<AudioSegmentRange> {
    val framesPerWindow = ((WINDOW_MS * sampleRate) / 1000L).toInt().coerceAtLeast(1)
    val minSilenceFrames = ((minSilenceMs * sampleRate) / 1000L)
    val paddingFrames = ((paddingMs * sampleRate) / 1000L)

    val windowBytes = framesPerWindow * channelCount * 2
    val buffer = ByteArray(windowBytes)
    val shortBuffer = ShortArray(framesPerWindow * channelCount)

    val frameIsVoice = mutableListOf<Boolean>()
    var totalFrames = 0L

    FileInputStream(pcmFile).use { fis ->
      val bis = BufferedInputStream(fis, 65536)
      var bytesRead: Int
      while (bis.read(buffer).also { bytesRead = it } > 0) {
        val samplesRead = bytesRead / 2
        val framesInBlock = samplesRead / channelCount
        ByteBuffer.wrap(buffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer, 0, samplesRead)

        // Calcular RMS
        var sumSquares = 0.0
        for (i in 0 until samplesRead) {
          val sampleNorm = shortBuffer[i] / 32768.0
          sumSquares += sampleNorm * sampleNorm
        }
        val rms = sqrt(sumSquares / max(1, samplesRead))
        val db = if (rms > 1e-7) (20.0 * log10(rms)).toFloat() else -100f

        val isVoice = db >= thresholdDb
        for (f in 0 until framesInBlock) {
          frameIsVoice.add(isVoice)
        }
        totalFrames += framesInBlock
      }
    }

    if (frameIsVoice.isEmpty()) {
      return listOf(AudioSegmentRange(0, 0, true))
    }

    // Agrupar en tramos continuos
    val rawSegments = mutableListOf<AudioSegmentRange>()
    var currentVoice = frameIsVoice[0]
    var startFrame = 0L

    for (i in 1 until frameIsVoice.size) {
      if (frameIsVoice[i] != currentVoice) {
        rawSegments.add(AudioSegmentRange(startFrame, i.toLong(), currentVoice))
        startFrame = i.toLong()
        currentVoice = frameIsVoice[i]
      }
    }
    rawSegments.add(AudioSegmentRange(startFrame, frameIsVoice.size.toLong(), currentVoice))

    // Filtrar silencios que sean demasiado cortos (mantenerlos como voz)
    val refinedSegments = mutableListOf<AudioSegmentRange>()
    for (seg in rawSegments) {
      val durationFrames = seg.endFrame - seg.startFrame
      if (!seg.isVoice && durationFrames < minSilenceFrames) {
        refinedSegments.add(AudioSegmentRange(seg.startFrame, seg.endFrame, true))
      } else {
        refinedSegments.add(seg)
      }
    }

    // Aplicar padding a las zonas de voz para no morder inicios/finales de palabras
    val finalVoiceMask = BooleanArray(frameIsVoice.size)
    for (seg in refinedSegments) {
      if (seg.isVoice) {
        val padStart = max(0L, seg.startFrame - paddingFrames).toInt()
        val padEnd = (seg.endFrame + paddingFrames).coerceAtMost(frameIsVoice.size.toLong()).toInt()
        for (idx in padStart until padEnd) {
          finalVoiceMask[idx] = true
        }
      }
    }

    // Construir lista final de rangos fusionados
    val finalSegments = mutableListOf<AudioSegmentRange>()
    var curV = finalVoiceMask[0]
    var sF = 0L
    for (i in 1 until finalVoiceMask.size) {
      if (finalVoiceMask[i] != curV) {
        finalSegments.add(AudioSegmentRange(sF, i.toLong(), curV))
        sF = i.toLong()
        curV = finalVoiceMask[i]
      }
    }
    finalSegments.add(AudioSegmentRange(sF, finalVoiceMask.size.toLong(), curV))
    return finalSegments
  }

  private fun buildProcessedPcm(
    inputPcm: File,
    outputPcm: File,
    segments: List<AudioSegmentRange>,
    channelCount: Int,
    mode: SilenceCutMode
  ): Long {
    var writtenFrames = 0L
    val bytesPerFrame = channelCount * 2
    val frameBuf = ByteArray(bytesPerFrame)

    FileInputStream(inputPcm).use { fis ->
      val bis = BufferedInputStream(fis, 65536)
      FileOutputStream(outputPcm).use { fos ->
        val bos = BufferedOutputStream(fos, 65536)

        var currentFileFrame = 0L
        for (seg in segments) {
          val segFrames = seg.endFrame - seg.startFrame

          if (seg.isVoice) {
            // Zona con voz: copiar frame por frame
            for (f in 0 until segFrames) {
              val read = bis.read(frameBuf)
              if (read <= 0) break
              bos.write(frameBuf, 0, read)
              writtenFrames++
              currentFileFrame++
            }
          } else {
            // Zona de silencio
            if (mode == SilenceCutMode.REMOVE) {
              // Modo eliminar: saltar frames del stream
              val bytesToSkip = segFrames * bytesPerFrame
              var skipped = 0L
              while (skipped < bytesToSkip) {
                val s = bis.skip(bytesToSkip - skipped)
                if (s <= 0) break
                skipped += s
              }
              currentFileFrame += segFrames
            } else {
              // Modo acelerar 4x: escribir 1 de cada 4 frames
              for (f in 0 until segFrames) {
                val read = bis.read(frameBuf)
                if (read <= 0) break
                if (f % 4 == 0L) {
                  bos.write(frameBuf, 0, read)
                  writtenFrames++
                }
                currentFileFrame++
              }
            }
          }
        }
        bos.flush()
      }
    }
    return writtenFrames
  }
}
