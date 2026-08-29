package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AppAudioFolder
import com.example.audio.AppStorageManager
import com.example.audio.AudioMetadataReader
import com.example.audio.AudioPlayerManager
import com.example.audio.PlaybackState
import com.example.audio.Spatial8DAudioProcessor
import com.example.model.AudioFileInfo
import com.example.model.AudioFormat
import com.example.model.ConvertedAudioFile
import com.example.model.Spatial8DOptions
import com.example.model.Spatial8DProgress
import com.example.model.Spatial8DState
import com.example.model.SpatialReverbPreset
import com.example.model.SpatialRotationSpeed
import com.example.model.SpatialTrajectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class Spatial8DViewModel(application: Application) : AndroidViewModel(application) {

  private val audioPlayerManager = AudioPlayerManager(application, viewModelScope)
  val playbackState: StateFlow<PlaybackState> = audioPlayerManager.playbackState

  private val _selectedAudioUri = MutableStateFlow<Uri?>(null)
  val selectedAudioUri: StateFlow<Uri?> = _selectedAudioUri.asStateFlow()

  private val _audioMetadata = MutableStateFlow<AudioFileInfo?>(null)
  val audioMetadata: StateFlow<AudioFileInfo?> = _audioMetadata.asStateFlow()

  private val _options = MutableStateFlow(Spatial8DOptions())
  val options: StateFlow<Spatial8DOptions> = _options.asStateFlow()

  private val _progress = MutableStateFlow(Spatial8DProgress())
  val progress: StateFlow<Spatial8DProgress> = _progress.asStateFlow()

  private val _convertedHistory = MutableStateFlow<List<ConvertedAudioFile>>(emptyList())
  val convertedHistory: StateFlow<List<ConvertedAudioFile>> = _convertedHistory.asStateFlow()

  private var processingJob: Job? = null

  init {
    loadHistory()
  }

  fun loadHistory() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val files = AppStorageManager.listFilesForFolder(getApplication(), AppAudioFolder.AUDIO_8D)
        val list = files.map { file ->
          val ext = file.extension.lowercase()
          val format = AudioFormat.values().find { it.extension == ext } ?: AudioFormat.MP3
          val meta = AudioMetadataReader.readMetadata(getApplication(), Uri.fromFile(file))
          ConvertedAudioFile(
            id = file.absolutePath,
            file = file,
            name = file.name,
            format = format,
            sizeBytes = file.length(),
            durationMs = meta?.durationMs ?: 0L,
            timestamp = file.lastModified(),
            sampleRate = meta?.sampleRate ?: 44100,
            channels = meta?.channelCount ?: 2,
            bitrateKbps = meta?.bitrateKbps ?: format.recommendedBitrateKbps
          )
        }
        _convertedHistory.value = list
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun selectAudio(uri: Uri) {
    _selectedAudioUri.value = uri
    audioPlayerManager.stop()
    viewModelScope.launch(Dispatchers.IO) {
      val meta = AudioMetadataReader.readMetadata(getApplication(), uri)
      _audioMetadata.value = meta
    }
  }

  fun updateRotationSpeed(speed: SpatialRotationSpeed) {
    _options.value = _options.value.copy(rotationSpeed = speed)
  }

  fun updateTrajectory(trajectory: SpatialTrajectory) {
    _options.value = _options.value.copy(trajectory = trajectory)
  }

  fun updateReverbPreset(preset: SpatialReverbPreset) {
    _options.value = _options.value.copy(reverbPreset = preset)
  }

  fun updateSpatialDepth(depth: Float) {
    _options.value = _options.value.copy(spatialDepth = depth.coerceIn(0.1f, 1.0f))
  }

  fun updateTargetFormat(format: AudioFormat) {
    _options.value = _options.value.copy(targetFormat = format)
  }

  fun updateBitrate(bitrateKbps: Int) {
    _options.value = _options.value.copy(bitrateKbps = bitrateKbps)
  }

  fun updateCustomFileName(name: String) {
    _options.value = _options.value.copy(customFileName = if (name.isBlank()) null else name)
  }

  fun startProcessing() {
    val uri = _selectedAudioUri.value ?: return
    if (_progress.value.state != Spatial8DState.IDLE && _progress.value.state != Spatial8DState.COMPLETED && _progress.value.state != Spatial8DState.ERROR) {
      return
    }

    audioPlayerManager.stop()

    processingJob = viewModelScope.launch {
      val result = Spatial8DAudioProcessor.processToSpatial8D(
        context = getApplication(),
        inputUri = uri,
        options = _options.value,
        onProgress = { p ->
          _progress.value = p
        }
      )

      if (result != null) {
        loadHistory()
      }
    }
  }

  fun cancelProcessing() {
    processingJob?.cancel()
    processingJob = null
    _progress.value = Spatial8DProgress(state = Spatial8DState.IDLE)
  }

  fun dismissProgressDialog() {
    _progress.value = Spatial8DProgress(state = Spatial8DState.IDLE)
  }

  fun togglePlayOriginal() {
    val uri = _selectedAudioUri.value ?: return
    val title = _audioMetadata.value?.title ?: "Audio Original"
    audioPlayerManager.playOrPause(uri, title)
  }

  fun togglePlayFile(file: ConvertedAudioFile) {
    audioPlayerManager.playOrPause(file.file, file.name)
  }

  fun seekTo(positionMs: Long) {
    audioPlayerManager.seekTo(positionMs)
  }

  fun deleteFile(file: ConvertedAudioFile) {
    try {
      if (file.file.exists()) {
        file.file.delete()
      }
      _convertedHistory.value = _convertedHistory.value.filter { it.id != file.id }
      if (playbackState.value.currentFile?.absolutePath == file.file.absolutePath) {
        audioPlayerManager.stop()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun shareFile(context: Context, file: ConvertedAudioFile) {
    try {
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file.file
      )
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = file.format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(intent, "Compartir Audio 8D"))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onCleared() {
    super.onCleared()
    audioPlayerManager.release()
  }
}
