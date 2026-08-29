package com.example.audio

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.AudioFileInfo
import java.io.File

object AudioMetadataReader {

  fun readMetadata(context: Context, uri: Uri): AudioFileInfo? {
    val retriever = MediaMetadataRetriever()
    var name = "audio_input"
    var sizeBytes = 0L

    try {
      if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (cursor.moveToFirst()) {
            if (nameIndex != -1) name = cursor.getString(nameIndex) ?: "audio_input"
            if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
          }
        }
      } else if (uri.scheme == "file") {
        uri.path?.let { path ->
          val f = File(path)
          if (f.exists()) {
            name = f.name
            sizeBytes = f.length()
          }
        }
      }

      retriever.setDataSource(context, uri)

      val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
      val durationMs = durationStr?.toLongOrNull() ?: 0L

      val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
      val bitrateBps = bitrateStr?.toIntOrNull() ?: 0
      var bitrateKbps = if (bitrateBps > 0) (bitrateBps / 1000) else 0

      // Si el archivo no expone bitrate en sus etiquetas, estimarlo a partir del tamaño y duración
      if (bitrateKbps <= 0 && durationMs > 0 && sizeBytes > 0) {
        val calculatedKbps = ((sizeBytes * 8.0) / (durationMs / 1000.0) / 1000.0).toInt()
        bitrateKbps = calculatedKbps.coerceIn(32, 1411)
      } else if (bitrateKbps <= 0) {
        bitrateKbps = 192 // Fallback estándar
      }

      val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
      val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

      var sampleRate = 44100
      var channelCount = 2

      // Extract details via MediaExtractor
      try {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        for (i in 0 until extractor.trackCount) {
          val format = extractor.getTrackFormat(i)
          val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
          if (mime.startsWith("audio/")) {
            if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
              sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            }
            if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
              channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            }
            break
          }
        }
        extractor.release()
      } catch (e: Exception) {
        // Fallback to retriever or defaults
      }

      val ext = if (name.contains(".")) {
        name.substringAfterLast(".").lowercase()
      } else {
        "audio"
      }

      return AudioFileInfo(
        uri = uri,
        name = name,
        formatExtension = ext.uppercase(),
        sizeBytes = sizeBytes,
        durationMs = durationMs,
        sampleRate = sampleRate,
        channelCount = channelCount,
        bitrateKbps = bitrateKbps,
        artist = artist,
        title = title
      )
    } catch (e: Exception) {
      e.printStackTrace()
      return null
    } finally {
      try {
        retriever.release()
      } catch (e: Exception) {
        // ignore
      }
    }
  }

  fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
      String.format("%.2f MB", mb)
    } else {
      String.format("%.1f KB", kb)
    }
  }

  fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
  }
}
