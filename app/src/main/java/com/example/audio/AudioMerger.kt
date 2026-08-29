package com.example.audio

import android.content.Context
import com.example.audio.codec.MediaCodecEncoder
import com.example.audio.codec.PcmDecoder
import com.example.audio.codec.PcmDspProcessor
import com.example.model.AudioFormat
import com.example.model.ConvertedAudioFile
import com.example.model.MergeOptions
import com.example.model.MergeProgress
import com.example.model.MergeState
import com.example.model.MergeTrackItem
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.coroutineContext

/**
 * Motor de unión y concatenación acústica para múltiples pistas de audio.
 * Resuelve heterogeneidades de frecuencia de muestreo (Sample Rate), canales (Mono/Estéreo)
 * y códecs dispares decodificando a PCM lineal y aplicando remuestreo y normalización unificada.
 */
object AudioMerger {

  suspend fun mergeAudioTracks(
    context: Context,
    tracks: List<MergeTrackItem>,
    options: MergeOptions,
    onProgress: (MergeProgress) -> Unit
  ): ConvertedAudioFile? = withContext(Dispatchers.IO) {
    if (tracks.isEmpty()) {
      onProgress(
        MergeProgress(
          state = MergeState.ERROR,
          errorMessage = "No se seleccionaron pistas para unir."
        )
      )
      return@withContext null
    }

    val tempDir = File(context.cacheDir, "audio_merger_temp").apply { mkdirs() }
    val masterPcmFile = File(tempDir, "master_merged_${System.currentTimeMillis()}.pcm")
    val tempPcmFiles = mutableListOf<File>()

    try {
      onProgress(
        MergeProgress(
          state = MergeState.PREPARING,
          totalTracks = tracks.size,
          progressPercent = 0.05f,
          statusMessage = "Preparando motor de unión acústica..."
        )
      )

      // 1. Determinar frecuencia de muestreo y canales unificados
      val targetSampleRate = if (options.sampleRateHz > 0) {
        options.sampleRateHz
      } else {
        tracks.maxOfOrNull { it.sampleRate }?.takeIf { it > 0 } ?: 44100
      }
      val targetChannels = options.targetChannels.coerceAtLeast(1)

      var masterOutStream: BufferedOutputStream? = null
      var totalMergedDurationMs = 0L

      try {
        masterOutStream = BufferedOutputStream(FileOutputStream(masterPcmFile), 64 * 1024)

        // 2. Procesar y normalizar cada pista de audio secuencialmente
        for (i in tracks.indices) {
          if (!coroutineContext.isActive) return@withContext null

          val track = tracks[i]
          val trackProgressBase = (i.toFloat() / tracks.size.toFloat()) * 0.65f + 0.05f

          onProgress(
            MergeProgress(
              state = MergeState.DECODING_TRACKS,
              currentTrackIndex = i + 1,
              totalTracks = tracks.size,
              progressPercent = trackProgressBase,
              statusMessage = "Decodificando pista ${i + 1} de ${tracks.size}: ${track.name}"
            )
          )

          // Decodificar pista individual a PCM crudo
          val rawTrackPcm = File(tempDir, "track_${i}_raw.pcm")
          tempPcmFiles.add(rawTrackPcm)

          val decodeResult = PcmDecoder.decodeMediaToPcm(
            context = context,
            uri = track.uri,
            outputPcmFile = rawTrackPcm,
            onProgress = { subProg ->
              val currentProg = trackProgressBase + (subProg / tracks.size.toFloat()) * 0.4f
              onProgress(
                MergeProgress(
                  state = MergeState.DECODING_TRACKS,
                  currentTrackIndex = i + 1,
                  totalTracks = tracks.size,
                  progressPercent = currentProg.coerceIn(0.05f, 0.70f),
                  statusMessage = "Decodificando pista ${i + 1} de ${tracks.size} (${(subProg * 100).toInt()}%)"
                )
              )
            }
          )

          if (!decodeResult.success || !rawTrackPcm.exists() || rawTrackPcm.length() == 0L) {
            throw IllegalStateException("Fallo al decodificar la pista: ${track.name}. ${decodeResult.error ?: ""}")
          }

          // Remuestreo y unificación de canales si la pista difiere del objetivo
          val normalizedTrackPcm = File(tempDir, "track_${i}_norm.pcm")
          tempPcmFiles.add(normalizedTrackPcm)

          val srcRate = if (decodeResult.sampleRate > 0) decodeResult.sampleRate else track.sampleRate
          val srcChan = if (decodeResult.channels > 0) decodeResult.channels else track.channelCount

          val normSuccess = PcmDspProcessor.resampleAndProcessPcm(
            inputPcm = rawTrackPcm,
            outputPcm = normalizedTrackPcm,
            srcSampleRate = srcRate,
            dstSampleRate = targetSampleRate,
            srcChannels = srcChan,
            dstChannels = targetChannels,
            volume = 1.0f
          )

          val pcmToAppend = if (normSuccess && normalizedTrackPcm.exists() && normalizedTrackPcm.length() > 0) {
            normalizedTrackPcm
          } else {
            rawTrackPcm
          }

          // Concatenar al flujo PCM maestro con micro-suavizado en los empalmes
          appendPcmWithSmoothing(
            pcmFile = pcmToAppend,
            masterOut = masterOutStream,
            targetChannels = targetChannels,
            isFirstTrack = (i == 0),
            applySmoothing = options.enableCrossfade
          )

          totalMergedDurationMs += track.durationMs
        }

        masterOutStream.flush()
      } finally {
        try {
          masterOutStream?.close()
        } catch (ignored: Exception) {}
      }

      if (!masterPcmFile.exists() || masterPcmFile.length() == 0L) {
        throw IllegalStateException("El archivo maestro unificado está vacío.")
      }

      // 3. Codificar el PCM maestro al formato final seleccionado
      onProgress(
        MergeProgress(
          state = MergeState.ENCODING_OUTPUT,
          totalTracks = tracks.size,
          progressPercent = 0.75f,
          statusMessage = "Codificando audio unificado en ${options.targetFormat.badge}..."
        )
      )

      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val baseName = if (options.customFileName.isNotBlank()) {
        options.customFileName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
      } else {
        "Audio_Unido_${tracks.size}_pistas_$timeStamp"
      }
      val finalFileName = "$baseName.${options.targetFormat.extension}"

      val outputFolder = AppStorageManager.getFolder(context, AppAudioFolder.FUSIONAR)
      val finalOutputFile = File(outputFolder, finalFileName)

      val encodeSuccess = MediaCodecEncoder.encodePcmByFormat(
        pcmFile = masterPcmFile,
        outputFile = finalOutputFile,
        format = options.targetFormat,
        sampleRate = targetSampleRate,
        channels = targetChannels,
        bitrateKbps = options.bitrateKbps,
        onEncodeProgress = { encodeProg ->
          val prog = 0.75f + (encodeProg * 0.20f)
          onProgress(
            MergeProgress(
              state = MergeState.ENCODING_OUTPUT,
              totalTracks = tracks.size,
              progressPercent = prog.coerceIn(0.75f, 0.95f),
              statusMessage = "Codificando ${options.targetFormat.badge} (${(encodeProg * 100).toInt()}%)"
            )
          )
        }
      )

      if (!encodeSuccess || !finalOutputFile.exists() || finalOutputFile.length() == 0L) {
        throw IllegalStateException("No se pudo codificar el archivo de salida final en ${options.targetFormat.badge}.")
      }

      // 4. Indexar en el almacenamiento público MediaStore
      onProgress(
        MergeProgress(
          state = MergeState.SAVING,
          totalTracks = tracks.size,
          progressPercent = 0.98f,
          statusMessage = "Guardando en Música/AudioConverter/Fusionar..."
        )
      )

      AppStorageManager.exportToPublicMusicFolder(
        context = context,
        sourceFile = finalOutputFile,
        mimeType = options.targetFormat.mimeType,
        folderType = AppAudioFolder.FUSIONAR,
        customDisplayName = finalFileName
      )

      val result = ConvertedAudioFile(
        id = UUID.randomUUID().toString(),
        file = finalOutputFile,
        name = finalFileName,
        format = options.targetFormat,
        sizeBytes = finalOutputFile.length(),
        durationMs = totalMergedDurationMs,
        timestamp = System.currentTimeMillis(),
        sampleRate = targetSampleRate,
        channels = targetChannels,
        bitrateKbps = options.bitrateKbps
      )

      onProgress(
        MergeProgress(
          state = MergeState.COMPLETED,
          totalTracks = tracks.size,
          progressPercent = 1.0f,
          statusMessage = "¡${tracks.size} pistas unidas exitosamente!",
          mergedFile = result
        )
      )

      result
    } catch (e: Exception) {
      e.printStackTrace()
      onProgress(
        MergeProgress(
          state = MergeState.ERROR,
          errorMessage = e.localizedMessage ?: "Ocurrió un error al unir las pistas de audio."
        )
      )
      null
    } finally {
      // Limpieza de archivos temporales
      try {
        if (masterPcmFile.exists()) masterPcmFile.delete()
        for (f in tempPcmFiles) {
          if (f.exists()) f.delete()
        }
      } catch (ignored: Exception) {}
    }
  }

  /**
   * Concatena el archivo PCM al flujo de salida aplicando un micro-fundido suave
   * de 20ms en el punto de empalme para eliminar transitorios y chasquidos (anti-click).
   */
  private fun appendPcmWithSmoothing(
    pcmFile: File,
    masterOut: BufferedOutputStream,
    targetChannels: Int,
    isFirstTrack: Boolean,
    applySmoothing: Boolean
  ) {
    val inStream = BufferedInputStream(FileInputStream(pcmFile), 64 * 1024)
    val buffer = ByteArray(8192)
    var bytesRead: Int
    var isFirstBuffer = true

    while (inStream.read(buffer).also { bytesRead = it } != -1) {
      if (isFirstBuffer && !isFirstTrack && applySmoothing) {
        // Aplicar micro fade-in de 500 muestras en el inicio de la pista para suavizar el empalme
        val sampleCount = bytesRead / 2
        val byteBuf = ByteBuffer.wrap(buffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN)
        val fadeSamples = (sampleCount).coerceAtMost(500)

        for (s in 0 until fadeSamples) {
          val factor = s.toFloat() / fadeSamples.toFloat()
          val sampleVal = byteBuf.getShort(s * 2).toInt()
          val smoothed = (sampleVal * factor).toInt().coerceIn(-32768, 32767).toShort()
          byteBuf.putShort(s * 2, smoothed)
        }
        isFirstBuffer = false
      }

      masterOut.write(buffer, 0, bytesRead)
    }

    inStream.close()
  }
}
