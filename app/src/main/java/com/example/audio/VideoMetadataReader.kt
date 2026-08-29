package com.example.audio

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.VideoFileInfo
import java.io.File

object VideoMetadataReader {

  fun readVideoMetadata(context: Context, uri: Uri): VideoFileInfo? {
    val retriever = MediaMetadataRetriever()
    var name = "video_input.mp4"
    var sizeBytes = 0L

    try {
      if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (cursor.moveToFirst()) {
            if (nameIndex != -1) name = cursor.getString(nameIndex) ?: "video_input.mp4"
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

      val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
      val width = widthStr?.toIntOrNull() ?: 0

      val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
      val height = heightStr?.toIntOrNull() ?: 0

      val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
      val totalBitrateBps = bitrateStr?.toIntOrNull() ?: 0

      // Generar miniatura fotográfica del video
      var thumbnail: Bitmap? = null
      try {
        thumbnail = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
          ?: retriever.frameAtTime
      } catch (e: Throwable) {
        // Ignorar si no se puede extraer frame
      }

      // Analizar pista de audio con MediaExtractor
      var audioMime = ""
      var audioCodecName = "AAC"
      var audioSampleRate = 44100
      var audioChannels = 2
      var audioBitrateKbps = 192
      var hasAudio = false

      try {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        for (i in 0 until extractor.trackCount) {
          val format = extractor.getTrackFormat(i)
          val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
          if (mime.startsWith("audio/")) {
            hasAudio = true
            audioMime = mime
            audioCodecName = when {
              mime.contains("mp4a-latm", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC"
              mime.contains("mp3", ignoreCase = true) || mime.contains("mpeg", ignoreCase = true) -> "MP3"
              mime.contains("opus", ignoreCase = true) -> "OPUS"
              mime.contains("vorbis", ignoreCase = true) -> "OGG Vorbis"
              mime.contains("flac", ignoreCase = true) -> "FLAC"
              mime.contains("ac3", ignoreCase = true) || mime.contains("eac3", ignoreCase = true) -> "Dolby AC3"
              mime.contains("pcm", ignoreCase = true) || mime.contains("raw", ignoreCase = true) -> "PCM WAV"
              else -> mime.substringAfter("audio/").uppercase()
            }
            if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
              audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            }
            if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
              audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            }
            if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
              audioBitrateKbps = (format.getInteger(MediaFormat.KEY_BIT_RATE) / 1000).coerceAtLeast(32)
            }
            break
          }
        }
        extractor.release()
      } catch (e: Throwable) {
        // Fallback
      }

      val ext = if (name.contains(".")) {
        name.substringAfterLast(".").uppercase()
      } else {
        "MP4"
      }

      return VideoFileInfo(
        uri = uri,
        name = name,
        containerExtension = ext,
        sizeBytes = sizeBytes,
        durationMs = durationMs,
        width = width,
        height = height,
        audioMimeType = audioMime,
        audioCodecName = audioCodecName,
        audioChannels = audioChannels,
        audioSampleRate = audioSampleRate,
        audioBitrateKbps = audioBitrateKbps,
        hasAudioTrack = hasAudio,
        thumbnail = thumbnail
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
}
