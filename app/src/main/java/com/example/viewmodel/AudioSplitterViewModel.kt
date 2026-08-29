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
import com.example.audio.AudioSplitterProcessor
import com.example.audio.PlaybackState
import com.example.model.AudioFileInfo
import com.example.model.AudioSplitOptions
import com.example.model.AudioSplitProgress
import com.example.model.ConvertedAudioFile
import com.example.model.DetectedTrackSegment
import com.example.model.SplitProcessingState
import com.example.model.SplitSilenceDuration
import com.example.model.SplitSilenceSensitivity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioSplitterViewModel(application: Application) : AndroidViewModel(application) {

  companion object {
    private const val TAG = "AudioSplitterViewModel"
  }

  private val playerManager = AudioPlayerManager(application, viewModelScope)
  val playbackState: StateFlow<PlaybackState> = playerManager.playbackState

  private val _selectedAudioUri = MutableStateFlow<Uri?>(null)
  val selectedAudioUri: StateFlow<Uri?> = _selectedAudioUri.asStateFlow()

  private val _audioMetadata = MutableStateFlow<AudioFileInfo?>(null)
  val audioMetadata: StateFlow<AudioFileInfo?> = _audioMetadata.asStateFlow()

  private val _options = MutableStateFlow(AudioSplitOptions())
  val options: StateFlow<AudioSplitOptions> = _options.asStateFlow()

  private val _detectedSegments = MutableStateFlow<List<DetectedTrackSegment>>(emptyList())
  val detectedSegments: StateFlow<List<DetectedTrackSegment>> = _detectedSegments.asStateFlow()

  private val _progress = MutableStateFlow(AudioSplitProgress())
  val progress: StateFlow<AudioSplitProgress> = _progress.asStateFlow()

  private val _history = MutableStateFlow<List<ConvertedAudioFile>>(emptyList())
  val history: StateFlow<List<ConvertedAudioFile>> = _history.asStateFlow()

  private var cachedPcmFile: File? = null
  private var processingJob: Job? = null

  init {
    refreshHistory()
  }

  fun selectAudio(uri: Uri) {
    _selectedAudioUri.value = uri
    _detectedSegments.value = emptyList()
    _progress.value = AudioSplitProgress(state = SplitProcessingState.IDLE)
    cachedPcmFile?.delete()
    cachedPcmFile = null

    viewModelScope.launch(Dispatchers.IO) {
      val meta = AudioMetadataReader.readMetadata(getApplication(), uri)
      _audioMetadata.value = meta
      if (_options.value.baseFileName.isBlank() && meta != null) {
        _options.value = _options.value.copy(baseFileName = meta.name.substringBeforeLast("."))
      }
    }
  }

  fun updateSensitivity(sensitivity: SplitSilenceSensitivity) {
    _options.value = _options.value.copy(sensitivity = sensitivity)
  }

  fun updateMinSilenceDuration(duration: SplitSilenceDuration) {
    _options.value = _options.value.copy(minSilenceDuration = duration)
  }

  fun updateTargetFormat(format: com.example.model.AudioFormat) {
    _options.value = _options.value.copy(targetFormat = format)
  }

  fun updateBitrate(bitrateKbps: Int) {
    _options.value = _options.value.copy(bitrateKbps = bitrateKbps)
  }

  fun updateBaseFileName(name: String) {
    _options.value = _options.value.copy(baseFileName = name)
  }

  fun updateZipAllTracks(zip: Boolean) {
    _options.value = _options.value.copy(zipAllTracks = zip)
  }

  fun toggleTrackSelection(trackIndex: Int) {
    _detectedSegments.value = _detectedSegments.value.map { seg ->
      if (seg.trackIndex == trackIndex) seg.copy(isSelected = !seg.isSelected) else seg
    }
  }

  fun updateTrackCustomTitle(trackIndex: Int, title: String) {
    _detectedSegments.value = _detectedSegments.value.map { seg ->
      if (seg.trackIndex == trackIndex) seg.copy(customTitle = title) else seg
    }
  }

  fun selectAllTracks(select: Boolean) {
    _detectedSegments.value = _detectedSegments.value.map { it.copy(isSelected = select) }
  }

  /**
   * Ejecuta el análisis de silencios para detectar los cortes de pista.
   */
  fun analyzeAudio() {
    val uri = _selectedAudioUri.value ?: return
    processingJob?.cancel()
    processingJob = viewModelScope.launch(Dispatchers.IO) {
      _progress.value = AudioSplitProgress(
        state = SplitProcessingState.ANALYZING,
        progress = 0.05f,
        statusMessage = "Iniciando análisis espectral..."
      )

      val (segments, pcmFile) = AudioSplitterProcessor.analyzeAudioForSplit(
        context = getApplication(),
        sourceUri = uri,
        options = _options.value
      ) { prog ->
        _progress.value = prog
      }

      cachedPcmFile = pcmFile
      _detectedSegments.value = segments
    }
  }

  /**
   * Exporta las pistas seleccionadas a la carpeta pública Música/AudioConverter/Dividir.
   */
  fun exportTracks() {
    val uri = _selectedAudioUri.value ?: return
    val segments = _detectedSegments.value
    if (segments.none { it.isSelected }) return

    processingJob?.cancel()
    processingJob = viewModelScope.launch(Dispatchers.IO) {
      val splitFolder = AppStorageManager.getFolder(getApplication(), AppAudioFolder.DIVIDIR)
      val result = AudioSplitterProcessor.exportSplitTracks(
        context = getApplication(),
        cachedPcmFile = cachedPcmFile,
        sourceUri = uri,
        segments = segments,
        options = _options.value,
        outputDirectory = splitFolder
      ) { prog ->
        _progress.value = prog
      }

      _progress.value = result
      refreshHistory()
    }
  }

  fun cancelProcessing() {
    processingJob?.cancel()
    _progress.value = AudioSplitProgress(state = SplitProcessingState.IDLE)
  }

  fun playTrackPreview(startMs: Long, durationMs: Long) {
    val uri = _selectedAudioUri.value ?: return
    playerManager.playOrPause(uri, "Preescucha")
    playerManager.seekTo(startMs)
  }

  fun togglePlayOriginal() {
    val uri = _selectedAudioUri.value ?: return
    val title = _audioMetadata.value?.name ?: "Audio Original"
    playerManager.playOrPause(uri, title)
  }

  fun togglePlayFile(file: File) {
    playerManager.playOrPause(file, file.name)
  }

  fun refreshHistory() {
    viewModelScope.launch(Dispatchers.IO) {
      val files = AppStorageManager.listFilesForFolder(getApplication(), AppAudioFolder.DIVIDIR)
      val items = files.map { file ->
        val ext = file.extension.lowercase()
        val format = com.example.model.AudioFormat.values().find { it.extension == ext } ?: com.example.model.AudioFormat.MP3
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
          bitrateKbps = meta?.bitrateKbps ?: 192
        )
      }.sortedByDescending { it.timestamp }
      _history.value = items
    }
  }

  fun shareFile(context: Context, file: File, mimeType: String = "audio/*") {
    try {
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(intent, "Compartir audio"))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun deleteFile(file: File) {
    viewModelScope.launch(Dispatchers.IO) {
      if (playbackState.value.currentFile?.absolutePath == file.absolutePath) {
        playerManager.stop()
      }
      file.delete()
      refreshHistory()
    }
  }

  override fun onCleared() {
    super.onCleared()
    playerManager.release()
    cachedPcmFile?.delete()
  }
}
