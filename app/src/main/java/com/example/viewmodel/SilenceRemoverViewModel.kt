package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AppAudioFolder
import com.example.audio.AppStorageManager
import com.example.audio.AudioMetadataReader
import com.example.audio.AudioPlayerManager
import com.example.audio.PlaybackState
import com.example.audio.SilenceRemoverProcessor
import com.example.model.AudioFileInfo
import com.example.model.AudioFormat
import com.example.model.ConvertedAudioFile
import com.example.model.SilenceCutMode
import com.example.model.SilenceProgress
import com.example.model.SilenceRemoverOptions
import com.example.model.SilenceState
import com.example.model.SilenceThresholdLevel
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SilenceRemoverViewModel(application: Application) : AndroidViewModel(application) {

  private val TAG = "SilenceRemoverViewModel"
  val playerManager = AudioPlayerManager(application, viewModelScope)
  val playbackState: StateFlow<PlaybackState> = playerManager.playbackState

  private val _selectedAudioUri = MutableStateFlow<Uri?>(null)
  val selectedAudioUri: StateFlow<Uri?> = _selectedAudioUri.asStateFlow()

  private val _audioMetadata = MutableStateFlow<AudioFileInfo?>(null)
  val audioMetadata: StateFlow<AudioFileInfo?> = _audioMetadata.asStateFlow()

  private val _options = MutableStateFlow(SilenceRemoverOptions())
  val options: StateFlow<SilenceRemoverOptions> = _options.asStateFlow()

  private val _progress = MutableStateFlow(SilenceProgress())
  val progress: StateFlow<SilenceProgress> = _progress.asStateFlow()

  private val _history = MutableStateFlow<List<ConvertedAudioFile>>(emptyList())
  val history: StateFlow<List<ConvertedAudioFile>> = _history.asStateFlow()

  init {
    refreshHistory()
  }

  fun selectAudio(uri: Uri) {
    _selectedAudioUri.value = uri
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val info = AudioMetadataReader.readMetadata(getApplication(), uri)
        _audioMetadata.value = info
      } catch (e: Exception) {
        Log.e(TAG, "Error leyendo metadatos de audio: ${e.message}")
      }
    }
  }

  fun updateThresholdLevel(level: SilenceThresholdLevel) {
    _options.value = _options.value.copy(thresholdLevel = level)
  }

  fun updateMinSilenceDuration(durationMs: Long) {
    _options.value = _options.value.copy(minSilenceDurationMs = durationMs)
  }

  fun updatePaddingVoice(paddingMs: Long) {
    _options.value = _options.value.copy(paddingVoiceMs = paddingMs)
  }

  fun updateCutMode(mode: SilenceCutMode) {
    _options.value = _options.value.copy(mode = mode)
  }

  fun updateTargetFormat(format: AudioFormat) {
    _options.value = _options.value.copy(targetFormat = format)
  }

  fun updateBitrate(bitrate: Int) {
    _options.value = _options.value.copy(bitrateKbps = bitrate)
  }

  fun updateCustomFileName(name: String) {
    _options.value = _options.value.copy(customFileName = name)
  }

  fun startProcessing() {
    val uri = _selectedAudioUri.value ?: return
    viewModelScope.launch(Dispatchers.IO) {
      val baseName = if (_options.value.customFileName.isNotBlank()) {
        _options.value.customFileName.trim()
      } else {
        val origName = _audioMetadata.value?.name?.substringBeforeLast(".") ?: "audio"
        "${origName}_SinSilencio"
      }

      val ext = _options.value.targetFormat.extension
      val targetFolder = AppStorageManager.getFolder(getApplication(), AppAudioFolder.SIN_SILENCIO)
      val outputFile = File(targetFolder, "$baseName.$ext")

      val result = SilenceRemoverProcessor.processSilenceRemoval(
        context = getApplication(),
        sourceUri = uri,
        options = _options.value,
        outputFile = outputFile
      ) { prog ->
        _progress.value = prog
      }

      _progress.value = result

      if (result.state == SilenceState.COMPLETED && result.outputFile != null) {
        AppStorageManager.exportToPublicMusicFolder(
          context = getApplication(),
          sourceFile = result.outputFile,
          mimeType = _options.value.targetFormat.mimeType,
          folderType = AppAudioFolder.SIN_SILENCIO,
          customDisplayName = result.outputFile.name
        )
        refreshHistory()
      }
    }
  }

  fun cancelProcessing() {
    _progress.value = SilenceProgress(state = SilenceState.IDLE)
  }

  fun dismissProgressDialog() {
    _progress.value = SilenceProgress(state = SilenceState.IDLE)
  }

  fun refreshHistory() {
    viewModelScope.launch(Dispatchers.IO) {
      val files = AppStorageManager.listFilesForFolder(getApplication(), AppAudioFolder.SIN_SILENCIO)
      val convertedList = files.map { file ->
        val ext = file.extension.lowercase()
        val format = AudioFormat.values().find { it.extension == ext } ?: AudioFormat.MP3
        val durationMs = extractDuration(file)
        ConvertedAudioFile(
          id = file.absolutePath.hashCode().toString(),
          file = file,
          name = file.name,
          format = format,
          sizeBytes = file.length(),
          durationMs = durationMs,
          timestamp = file.lastModified(),
          sampleRate = 44100,
          channels = 2,
          bitrateKbps = _options.value.bitrateKbps
        )
      }.sortedByDescending { it.timestamp }

      _history.value = convertedList
    }
  }

  fun togglePlayFile(file: ConvertedAudioFile) {
    playerManager.playOrPause(file.file, file.name)
  }

  fun togglePlayOriginal() {
    val uri = _selectedAudioUri.value ?: return
    val name = _audioMetadata.value?.name ?: "Audio Original"
    playerManager.playOrPause(uri, name)
  }

  fun seekTo(positionMs: Long) {
    playerManager.seekTo(positionMs)
  }

  fun shareFile(context: Context, file: ConvertedAudioFile) {
    try {
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file.file
      )
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(intent, "Compartir audio"))
    } catch (e: Exception) {
      Log.e(TAG, "Error compartiendo archivo: ${e.message}")
    }
  }

  fun deleteFile(file: ConvertedAudioFile) {
    viewModelScope.launch(Dispatchers.IO) {
      if (playerManager.playbackState.value.currentFile?.absolutePath == file.file.absolutePath) {
        playerManager.stop()
      }
      file.file.delete()
      refreshHistory()
    }
  }

  private fun extractDuration(file: File): Long {
    return try {
      val mmr = MediaMetadataRetriever()
      mmr.setDataSource(file.absolutePath)
      val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
      mmr.release()
      durStr?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
      0L
    }
  }

  override fun onCleared() {
    super.onCleared()
    playerManager.release()
  }
}
