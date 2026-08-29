package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AppAudioFolder
import com.example.audio.AppStorageManager
import com.example.audio.AudioMetadataReader
import com.example.audio.AudioMerger
import com.example.audio.AudioPlayerManager
import com.example.audio.PlaybackState
import com.example.model.AudioFormat
import com.example.model.ConvertedAudioFile
import com.example.model.MergeOptions
import com.example.model.MergeProgress
import com.example.model.MergeState
import com.example.model.MergeTrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AudioMergerViewModel(application: Application) : AndroidViewModel(application) {

  companion object {
    const val MAX_TRACKS = 6
  }

  private val audioPlayerManager = AudioPlayerManager(application, viewModelScope)
  val playbackState: StateFlow<PlaybackState> = audioPlayerManager.playbackState

  private val _selectedTracks = MutableStateFlow<List<MergeTrackItem>>(emptyList())
  val selectedTracks: StateFlow<List<MergeTrackItem>> = _selectedTracks.asStateFlow()

  private val _options = MutableStateFlow(MergeOptions())
  val options: StateFlow<MergeOptions> = _options.asStateFlow()

  private val _progress = MutableStateFlow(MergeProgress())
  val progress: StateFlow<MergeProgress> = _progress.asStateFlow()

  private val _mergedHistory = MutableStateFlow<List<ConvertedAudioFile>>(emptyList())
  val mergedHistory: StateFlow<List<ConvertedAudioFile>> = _mergedHistory.asStateFlow()

  private val _isAddingTracks = MutableStateFlow(false)
  val isAddingTracks: StateFlow<Boolean> = _isAddingTracks.asStateFlow()

  private var mergeJob: Job? = null

  init {
    loadExistingMergedFiles()
  }

  private fun loadExistingMergedFiles() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val folder = AppStorageManager.getFolder(getApplication(), AppAudioFolder.FUSIONAR)
        val files = folder.listFiles()?.filter { it.isFile && it.length() > 0 }?.sortedByDescending { it.lastModified() }
        if (!files.isNullOrEmpty()) {
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
          _mergedHistory.value = list
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun addTracks(context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      _isAddingTracks.value = true
      val currentList = _selectedTracks.value.toMutableList()
      val availableSlots = MAX_TRACKS - currentList.size

      if (availableSlots <= 0) {
        Toast.makeText(context, "Se alcanzó el límite máximo de $MAX_TRACKS pistas.", Toast.LENGTH_SHORT).show()
        _isAddingTracks.value = false
        return@launch
      }

      val toProcess = uris.take(availableSlots)
      if (uris.size > availableSlots) {
        Toast.makeText(context, "Solo se añadieron $availableSlots pistas para respetar el límite de $MAX_TRACKS.", Toast.LENGTH_LONG).show()
      }

      for (uri in toProcess) {
        val info = AudioMetadataReader.readMetadata(context, uri)
        if (info != null) {
          currentList.add(
            MergeTrackItem(
              uri = info.uri,
              name = info.name,
              formatExtension = info.formatExtension,
              sizeBytes = info.sizeBytes,
              durationMs = info.durationMs,
              sampleRate = info.sampleRate,
              channelCount = info.channelCount,
              bitrateKbps = info.bitrateKbps
            )
          )
        }
      }

      _selectedTracks.value = currentList
      _isAddingTracks.value = false
    }
  }

  fun moveTrackUp(index: Int) {
    val list = _selectedTracks.value.toMutableList()
    if (index > 0 && index < list.size) {
      val item = list.removeAt(index)
      list.add(index - 1, item)
      _selectedTracks.value = list
    }
  }

  fun moveTrackDown(index: Int) {
    val list = _selectedTracks.value.toMutableList()
    if (index >= 0 && index < list.size - 1) {
      val item = list.removeAt(index)
      list.add(index + 1, item)
      _selectedTracks.value = list
    }
  }

  fun removeTrack(index: Int) {
    val list = _selectedTracks.value.toMutableList()
    if (index in list.indices) {
      list.removeAt(index)
      _selectedTracks.value = list
    }
  }

  fun clearAllTracks() {
    _selectedTracks.value = emptyList()
  }

  fun setTargetFormat(format: AudioFormat) {
    _options.value = _options.value.copy(
      targetFormat = format,
      bitrateKbps = format.recommendedBitrateKbps
    )
  }

  fun setBitrate(bitrateKbps: Int) {
    _options.value = _options.value.copy(bitrateKbps = bitrateKbps)
  }

  fun setSampleRate(sampleRateHz: Int) {
    _options.value = _options.value.copy(sampleRateHz = sampleRateHz)
  }

  fun setCustomFileName(name: String) {
    _options.value = _options.value.copy(customFileName = name)
  }

  fun setEnableCrossfade(enabled: Boolean) {
    _options.value = _options.value.copy(enableCrossfade = enabled)
  }

  fun startMerge(context: Context) {
    val tracks = _selectedTracks.value
    if (tracks.size < 2) {
      Toast.makeText(context, "Selecciona al menos 2 pistas para unir.", Toast.LENGTH_SHORT).show()
      return
    }

    audioPlayerManager.stop()

    mergeJob = viewModelScope.launch {
      val result = AudioMerger.mergeAudioTracks(
        context = context,
        tracks = tracks,
        options = _options.value,
        onProgress = { prog ->
          _progress.value = prog
        }
      )

      if (result != null) {
        val updatedHistory = listOf(result) + _mergedHistory.value.filter { it.file.absolutePath != result.file.absolutePath }
        _mergedHistory.value = updatedHistory
      }
    }
  }

  fun cancelMerge() {
    mergeJob?.cancel()
    _progress.value = MergeProgress(
      state = MergeState.IDLE,
      statusMessage = "Unión cancelada."
    )
  }

  fun dismissProgressDialog() {
    _progress.value = MergeProgress(state = MergeState.IDLE)
  }

  fun togglePlayFile(context: Context, file: ConvertedAudioFile) {
    audioPlayerManager.playOrPause(file.file, file.name)
  }

  fun togglePlayTrack(context: Context, track: MergeTrackItem) {
    audioPlayerManager.playOrPause(track.uri, track.name)
  }

  fun seekTo(positionMs: Long) {
    audioPlayerManager.seekTo(positionMs)
  }

  fun shareMergedFile(context: Context, file: ConvertedAudioFile) {
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
      context.startActivity(Intent.createChooser(intent, "Compartir audio unido"))
    } catch (e: Exception) {
      Toast.makeText(context, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun deleteMergedFile(file: ConvertedAudioFile) {
    try {
      if (file.file.exists()) {
        file.file.delete()
      }
      _mergedHistory.value = _mergedHistory.value.filter { it.id != file.id }
      if (playbackState.value.currentFile?.absolutePath == file.file.absolutePath) {
        audioPlayerManager.stop()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun openOutputFolder(context: Context) {
    AppStorageManager.openFolderInFileManager(context, AppAudioFolder.FUSIONAR)
  }

  override fun onCleared() {
    super.onCleared()
    audioPlayerManager.release()
  }
}

