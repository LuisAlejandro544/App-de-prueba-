package com.example.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.audio.codec.MediaCodecEncoder
import com.example.audio.codec.PcmDecoder
import com.example.audio.codec.WavEncoder
import com.example.model.AudioFormat
import com.example.model.AudioSplitOptions
import com.example.model.AudioSplitProgress
import com.example.model.DetectedTrackSegment
import com.example.model.SplitProcessingState
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Motor DSP inteligente para segmentar y dividir un archivo de audio en múltiples pistas
 * a partir de la detección de pausas y silencios prolongados.
 */
object AudioSplitterProcessor {

  private const val TAG = "AudioSplitterProcessor"
  private const val WINDOW_MS = 25L

  /**
   * Analiza un audio y devuelve la lista de pistas / segmentos detectados.
   */
  suspend fun analyzeAudioForSplit(
    context: Context,
    sourceUri: Uri,
    options: AudioSplitOptions,
    onProgress: (AudioSplitProgress) -> Unit
  ): Pair<List<DetectedTrackSegment>, File?> = withContext(Dispatchers.IO) {
    var rawPcmFile: File? = null
    try {
      onProgress(
        AudioSplitProgress(
          state = SplitProcessingState.ANALYZING,
          progress = 0.05f,
          statusMessage = "Decodificando audio para análisis espectral..."
        )
      )

      rawPcmFile = File.createTempFile("split_analysis_", ".pcm", context.cacheDir)
      val decodeResult = PcmDecoder.decodeMediaToPcm(
        context = context,
        uri = sourceUri,
        outputPcmFile = rawPcmFile
      ) { decProg ->
        onProgress(
          AudioSplitProgress(
            state = SplitProcessingState.ANALYZING,
            progress = 0.05f + (decProg * 0.45f),
            statusMessage = "Decodificando: ${(decProg * 100).toInt()}%"
          )
        )
      }

      if (!decodeResult.success || !rawPcmFile.exists() || rawPcmFile.length() <= 0) {
        onProgress(
          AudioSplitProgress(
            state = SplitProcessingState.ERROR,
            errorMessage = "No se pudo decodificar el archivo de audio para su análisis."
          )
        )
        return@withContext Pair(emptyList(), null)
      }

      onProgress(
        AudioSplitProgress(
          state = SplitProcessingState.ANALYZING,
          progress = 0.55f,
          statusMessage = "Analizando niveles de energía RMS y pausas..."
        )
      )

      val segments = detectSegmentsFromPcm(
        pcmFile = rawPcmFile,
        sampleRate = decodeResult.sampleRate,
        channelCount = decodeResult.channels,
        thresholdDb = options.sensitivity.thresholdDb,
        minSilenceMs = options.minSilenceDuration.durationMs,
        minTrackDurationMs = options.minTrackDurationMs,
        paddingMs = options.paddingMs
      )

      onProgress(
        AudioSplitProgress(
          state = SplitProcessingState.PREVIEW_READY,
          progress = 1.0f,
          statusMessage = "¡Se detectaron ${segments.size} pistas!",
          totalTracksCount = segments.size
        )
      )

      return@withContext Pair(segments, rawPcmFile)
    } catch (e: Exception) {
      Log.e(TAG, "Error analizando audio: ${e.message}", e)
      rawPcmFile?.delete()
      onProgress(
        AudioSplitProgress(
          state = SplitProcessingState.ERROR,
          errorMessage = "Error durante el análisis: ${e.localizedMessage ?: e.message}"
        )
      )
      return@withContext Pair(emptyList(), null)
    }
  }

  /**
   * Exporta los segmentos seleccionados como archivos individuales de audio en la carpeta destino.
   */
  suspend fun exportSplitTracks(
    context: Context,
    cachedPcmFile: File?,
    sourceUri: Uri,
    segments: List<DetectedTrackSegment>,
    options: AudioSplitOptions,
    outputDirectory: File,
    onProgress: (AudioSplitProgress) -> Unit
  ): AudioSplitProgress = withContext(Dispatchers.IO) {
    var rawPcm = cachedPcmFile
    var shouldDeletePcm = false
    val exportedFiles = mutableListOf<File>()

    try {
      val selectedSegments = segments.filter { it.isSelected }
      if (selectedSegments.isEmpty()) {
        return@withContext AudioSplitProgress(
          state = SplitProcessingState.ERROR,
          errorMessage = "No has seleccionado ningún segmento para exportar."
        )
      }

      // Si no tenemos el PCM en caché, lo decodificamos
      if (rawPcm == null || !rawPcm.exists() || rawPcm.length() <= 0) {
        onProgress(
          AudioSplitProgress(
            state = SplitProcessingState.SPLITTING_EXPORT,
            progress = 0.05f,
            statusMessage = "Preparando decodificación para exportar..."
          )
        )
        rawPcm = File.createTempFile("split_export_", ".pcm", context.cacheDir)
        shouldDeletePcm = true
        val decodeResult = PcmDecoder.decodeMediaToPcm(
          context = context,
          uri = sourceUri,
          outputPcmFile = rawPcm
        ) {}
        if (!decodeResult.success) {
          return@withContext AudioSplitProgress(
            state = SplitProcessingState.ERROR,
            errorMessage = "Error decodificando audio para exportación."
          )
        }
      }

      // Obtener metadatos de audio
      val meta = AudioMetadataReader.readMetadata(context, sourceUri)
      val sampleRate = if ((meta?.sampleRate ?: 0) > 0) meta!!.sampleRate else 44100
      val channels = if ((meta?.channelCount ?: 0) > 0) meta!!.channelCount else 2
      val bytesPerFrame = channels * 2

      val totalTracks = selectedSegments.size
      val defaultName = meta?.name?.substringBeforeLast(".") ?: "Pista"
      val baseName = if (options.baseFileName.isNotBlank()) options.baseFileName.trim() else defaultName

      selectedSegments.forEachIndexed { index, segment ->
        val trackNumber = String.format("%02d", index + 1)
        val trackTitle = if (segment.customTitle.isNotBlank()) {
          "${trackNumber}_${segment.customTitle.trim()}"
        } else {
          "${baseName}_Pista_${trackNumber}"
        }

        val trackFileName = "${trackTitle}.${options.targetFormat.extension}"
        val trackOutFile = File(outputDirectory, trackFileName)

        onProgress(
          AudioSplitProgress(
            state = SplitProcessingState.SPLITTING_EXPORT,
            progress = (index.toFloat() / totalTracks),
            statusMessage = "Exportando pista ${index + 1} de $totalTracks: $trackFileName",
            totalTracksCount = totalTracks,
            exportedTracks = exportedFiles.toList()
          )
        )

        // Extraer PCM del segmento
        val segmentPcmFile = File.createTempFile("seg_pcm_${index}_", ".pcm", context.cacheDir)
        try {
          extractPcmRange(
            sourcePcm = rawPcm,
            outputPcm = segmentPcmFile,
            startFrame = segment.startFrame,
            endFrame = segment.endFrame,
            bytesPerFrame = bytesPerFrame
          )

          // Codificar segmento al formato seleccionado
          val encodeSuccess = MediaCodecEncoder.encodePcmByFormat(
            pcmFile = segmentPcmFile,
            outputFile = trackOutFile,
            format = options.targetFormat,
            sampleRate = sampleRate,
            channels = channels,
            bitrateKbps = options.bitrateKbps
          )

          if (encodeSuccess && trackOutFile.exists() && trackOutFile.length() > 0) {
            AppStorageManager.exportToPublicMusicFolder(
              context = context,
              sourceFile = trackOutFile,
              mimeType = options.targetFormat.mimeType,
              folderType = AppAudioFolder.DIVIDIR,
              customDisplayName = trackFileName
            )
            exportedFiles.add(trackOutFile)
          } else {
            // Fallback directo a WAV si el encoder no soporta algún parámetro
            WavEncoder.encodePcmToWav(
              pcmFile = segmentPcmFile,
              outputWavFile = trackOutFile,
              sampleRate = sampleRate,
              channels = channels
            )
            AppStorageManager.exportToPublicMusicFolder(
              context = context,
              sourceFile = trackOutFile,
              mimeType = "audio/wav",
              folderType = AppAudioFolder.DIVIDIR,
              customDisplayName = trackFileName
            )
            exportedFiles.add(trackOutFile)
          }
        } finally {
          segmentPcmFile.delete()
        }
      }

      // Opcional: Generar ZIP con todas las pistas para facilitar el compartir
      var zipFile: File? = null
      if (options.zipAllTracks && exportedFiles.isNotEmpty()) {
        onProgress(
          AudioSplitProgress(
            state = SplitProcessingState.SPLITTING_EXPORT,
            progress = 0.95f,
            statusMessage = "Empaquetando pistas en archivo ZIP...",
            totalTracksCount = totalTracks,
            exportedTracks = exportedFiles.toList()
          )
        )
        zipFile = File(outputDirectory, "${baseName}_Todas_Las_Pistas.zip")
        createZipArchive(exportedFiles, zipFile)
      }

      return@withContext AudioSplitProgress(
        state = SplitProcessingState.COMPLETED,
        progress = 1.0f,
        statusMessage = "¡Se exportaron exitosamente ${exportedFiles.size} pistas!",
        totalTracksCount = exportedFiles.size,
        exportedTracks = exportedFiles,
        zipFile = zipFile
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error exportando pistas: ${e.message}", e)
      return@withContext AudioSplitProgress(
        state = SplitProcessingState.ERROR,
        errorMessage = "Error exportando pistas: ${e.localizedMessage ?: e.message}",
        exportedTracks = exportedFiles
      )
    } finally {
      if (shouldDeletePcm) {
        rawPcm?.delete()
      }
    }
  }

  private fun detectSegmentsFromPcm(
    pcmFile: File,
    sampleRate: Int,
    channelCount: Int,
    thresholdDb: Float,
    minSilenceMs: Long,
    minTrackDurationMs: Long,
    paddingMs: Long
  ): List<DetectedTrackSegment> {
    val framesPerWindow = ((WINDOW_MS * sampleRate) / 1000L).toInt().coerceAtLeast(1)
    val minSilenceFrames = ((minSilenceMs * sampleRate) / 1000L)
    val minTrackFrames = ((minTrackDurationMs * sampleRate) / 1000L)
    val paddingFrames = ((paddingMs * sampleRate) / 1000L)

    val windowBytes = framesPerWindow * channelCount * 2
    val buffer = ByteArray(windowBytes)
    val shortBuffer = ShortArray(framesPerWindow * channelCount)

    val frameIsVoice = mutableListOf<Boolean>()

    FileInputStream(pcmFile).use { fis ->
      val bis = BufferedInputStream(fis, 65536)
      var bytesRead: Int
      while (bis.read(buffer).also { bytesRead = it } > 0) {
        val samplesRead = bytesRead / 2
        val framesInBlock = samplesRead / channelCount
        ByteBuffer.wrap(buffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer, 0, samplesRead)

        // Calcular RMS del bloque
        var sumSquares = 0.0
        for (i in 0 until samplesRead) {
          val sNorm = shortBuffer[i] / 32768.0
          sumSquares += sNorm * sNorm
        }
        val rms = sqrt(sumSquares / max(1, samplesRead))
        val db = if (rms > 1e-7) (20.0 * log10(rms)).toFloat() else -100f

        val isVoice = db >= thresholdDb
        for (f in 0 until framesInBlock) {
          frameIsVoice.add(isVoice)
        }
      }
    }

    if (frameIsVoice.isEmpty()) {
      return emptyList()
    }

    // Agrupar en tramos continuos
    val rawSegments = mutableListOf<Pair<Long, Long>>() // startFrame, endFrame (sólo zonas de voz)
    var inVoice = false
    var voiceStart = 0L

    for (i in frameIsVoice.indices) {
      val isV = frameIsVoice[i]
      if (isV && !inVoice) {
        inVoice = true
        voiceStart = i.toLong()
      } else if (!isV && inVoice) {
        inVoice = false
        rawSegments.add(Pair(voiceStart, i.toLong()))
      }
    }
    if (inVoice) {
      rawSegments.add(Pair(voiceStart, frameIsVoice.size.toLong()))
    }

    if (rawSegments.isEmpty()) {
      return emptyList()
    }

    // Fusionar segmentos que estén separados por silencios menores a minSilenceFrames
    val mergedVoice = mutableListOf<Pair<Long, Long>>()
    var currentTrackStart = rawSegments[0].first
    var currentTrackEnd = rawSegments[0].second

    for (i in 1 until rawSegments.size) {
      val nextStart = rawSegments[i].first
      val nextEnd = rawSegments[i].second
      val silenceGap = nextStart - currentTrackEnd

      if (silenceGap < minSilenceFrames) {
        // El silencio intermedio es demasiado corto, se considera parte de la misma pista
        currentTrackEnd = nextEnd
      } else {
        // Pausa larga confirmada: cerramos la pista actual e iniciamos una nueva
        mergedVoice.add(Pair(currentTrackStart, currentTrackEnd))
        currentTrackStart = nextStart
        currentTrackEnd = nextEnd
      }
    }
    mergedVoice.add(Pair(currentTrackStart, currentTrackEnd))

    // Filtrar pistas que duren menos que la duración mínima (ruidos o chasquidos) y aplicar padding
    val totalFrames = frameIsVoice.size.toLong()
    val finalTracks = mutableListOf<DetectedTrackSegment>()
    var trackIdx = 1

    for (seg in mergedVoice) {
      val durationFrames = seg.second - seg.first
      if (durationFrames >= minTrackFrames) {
        val paddedStart = max(0L, seg.first - paddingFrames)
        val paddedEnd = (seg.second + paddingFrames).coerceAtMost(totalFrames)

        val startMs = (paddedStart * 1000L) / sampleRate
        val endMs = (paddedEnd * 1000L) / sampleRate
        val durMs = endMs - startMs

        finalTracks.add(
          DetectedTrackSegment(
            trackIndex = trackIdx++,
            startMs = startMs,
            endMs = endMs,
            durationMs = durMs,
            startFrame = paddedStart,
            endFrame = paddedEnd,
            isSelected = true
          )
        )
      }
    }

    return finalTracks
  }

  private fun extractPcmRange(
    sourcePcm: File,
    outputPcm: File,
    startFrame: Long,
    endFrame: Long,
    bytesPerFrame: Int
  ) {
    val startByte = startFrame * bytesPerFrame
    val totalBytesToRead = (endFrame - startFrame) * bytesPerFrame

    FileInputStream(sourcePcm).use { fis ->
      val bis = BufferedInputStream(fis, 65536)
      // Saltar hasta el inicio del frame
      var skipped = 0L
      while (skipped < startByte) {
        val s = bis.skip(startByte - skipped)
        if (s <= 0) break
        skipped += s
      }

      FileOutputStream(outputPcm).use { fos ->
        val bos = BufferedOutputStream(fos, 65536)
        val buffer = ByteArray(8192)
        var bytesLeft = totalBytesToRead

        while (bytesLeft > 0) {
          val toRead = bytesLeft.coerceAtMost(buffer.size.toLong()).toInt()
          val read = bis.read(buffer, 0, toRead)
          if (read <= 0) break
          bos.write(buffer, 0, read)
          bytesLeft -= read
        }
        bos.flush()
      }
    }
  }

  private fun createZipArchive(files: List<File>, zipOutFile: File) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(zipOutFile))).use { zos ->
      val buffer = ByteArray(8192)
      for (file in files) {
        if (!file.exists()) continue
        FileInputStream(file).use { fis ->
          val bis = BufferedInputStream(fis, 8192)
          val entry = ZipEntry(file.name)
          zos.putNextEntry(entry)
          var count: Int
          while (bis.read(buffer).also { count = it } != -1) {
            zos.write(buffer, 0, count)
          }
          zos.closeEntry()
        }
      }
      zos.finish()
    }
  }
}
