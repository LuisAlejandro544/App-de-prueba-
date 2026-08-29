package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
  val isPlaying: Boolean = false,
  val currentUri: Uri? = null,
  val currentFile: File? = null,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val title: String = ""
)

class AudioPlayerManager(
  private val context: Context,
  private val scope: CoroutineScope
) {
  private var mediaPlayer: MediaPlayer? = null
  private var progressJob: Job? = null

  private val _playbackState = MutableStateFlow(PlaybackState())
  val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

  fun playOrPause(uri: Uri, title: String = "") {
    if (_playbackState.value.currentUri == uri) {
      if (mediaPlayer?.isPlaying == true) {
        pause()
      } else {
        resume()
      }
      return
    }

    startNewPlayback(uri, null, title)
  }

  fun playOrPause(file: File, title: String = "") {
    if (_playbackState.value.currentFile?.absolutePath == file.absolutePath) {
      if (mediaPlayer?.isPlaying == true) {
        pause()
      } else {
        resume()
      }
      return
    }

    startNewPlayback(Uri.fromFile(file), file, title.ifBlank { file.name })
  }

  private fun startNewPlayback(uri: Uri, file: File?, title: String) {
    stop()
    try {
      mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        )
        setDataSource(context, uri)
        prepare()
        start()
      }

      val duration = mediaPlayer?.duration?.toLong() ?: 0L
      _playbackState.value = PlaybackState(
        isPlaying = true,
        currentUri = uri,
        currentFile = file,
        currentPositionMs = 0L,
        durationMs = duration,
        title = title
      )

      mediaPlayer?.setOnCompletionListener {
        _playbackState.value = _playbackState.value.copy(
          isPlaying = false,
          currentPositionMs = 0L
        )
        stopProgressTicker()
      }

      startProgressTicker()
    } catch (e: Exception) {
      e.printStackTrace()
      stop()
    }
  }

  fun pause() {
    mediaPlayer?.let {
      if (it.isPlaying) {
        it.pause()
        _playbackState.value = _playbackState.value.copy(
          isPlaying = false,
          currentPositionMs = it.currentPosition.toLong()
        )
      }
    }
    stopProgressTicker()
  }

  fun resume() {
    mediaPlayer?.let {
      it.start()
      _playbackState.value = _playbackState.value.copy(isPlaying = true)
      startProgressTicker()
    }
  }

  fun seekTo(positionMs: Long) {
    mediaPlayer?.let {
      it.seekTo(positionMs.toInt())
      _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }
  }

  fun stop() {
    stopProgressTicker()
    try {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    } catch (e: Exception) {
      // ignore
    }
    mediaPlayer = null
    _playbackState.value = PlaybackState()
  }

  private fun startProgressTicker() {
    stopProgressTicker()
    progressJob = scope.launch(Dispatchers.Main) {
      while (isActive) {
        mediaPlayer?.let { player ->
          if (player.isPlaying) {
            _playbackState.value = _playbackState.value.copy(
              currentPositionMs = player.currentPosition.toLong(),
              durationMs = player.duration.toLong().coerceAtLeast(_playbackState.value.durationMs)
            )
          }
        }
        delay(100)
      }
    }
  }

  private fun stopProgressTicker() {
    progressJob?.cancel()
    progressJob = null
  }

  fun release() {
    stop()
  }
}
