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
import com.example.audio.AudioCompressorProcessor
import com.example.audio.AudioMetadataReader
import com.example.audio.AudioPlayerManager
import com.example.audio.PlaybackState
import com.example.model.AudioChannelMode
import com.example.model.AudioFileInfo
import com.example.model.AudioFormat
import com.example.model.AudioCompressionProfile
import com.example.model.CompressionOptions
import com.example.model.CompressionPreset
import com.example.model.CompressionProgress
import com.example.model.CompressionState
import com.example.model.ConvertedAudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class AudioCompressorViewModel(application: Application) : AndroidViewModel(application) {

  private val audioPlayerManager = AudioPlayerManager(application, viewModelScope)
  val playbackState: StateFlow<PlaybackState> = audioPlayerManager.playbackState

  private val _selectedAudioUri = MutableStateFlow<Uri?>(null)
  val selectedAudioUri: StateFlow<Uri?> = _selectedAudioUri.asStateFlow()

  private val _audioMetadata = MutableStateFlow<AudioFileInfo?>(null)
  val audioMetadata: StateFlow<AudioFileInfo?> = _audioMetadata.asStateFlow()

  private val _options = MutableStateFlow(CompressionOptions())
  val options: StateFlow<CompressionOptions> = _options.asStateFlow()

  private val _progress = MutableStateFlow(CompressionProgress())
  val progress: StateFlow<CompressionProgress> = _progress.asStateFlow()

  private val _history = MutableStateFlow<List<ConvertedAudioFile>>(emptyList())
  val history: StateFlow<List<ConvertedAudioFile>> = _history.asStateFlow()

  private var compressionJob: Job? = null

  // Estimación de tamaño y ahorro en tiempo real
  val estimatedSizeBytes: StateFlow<Long> = combine(_audioMetadata, _options) { meta, opt ->
    if (meta == null) 0L
    else AudioCompressorProcessor.estimateCompressedSizeBytes(meta.durationMs, opt, meta.sizeBytes)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

  val estimatedSavedPercent: StateFlow<Int> = combine(_audioMetadata, estimatedSizeBytes) { meta, est ->
    if (meta == null || meta.sizeBytes <= 0 || est <= 0) 0
    else {
      val saved = (meta.sizeBytes - est).coerceAtLeast(0L)
      ((saved.toDouble() / meta.sizeBytes.toDouble()) * 100).toInt().coerceIn(0, 99)
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val effectiveBitrateKbps: StateFlow<Int> = combine(_audioMetadata, _options) { meta, opt ->
    val duration = meta?.durationMs ?: 0L
    AudioCompressorProcessor.calculateEffectiveBitrate(duration, opt)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 96)

  init {
    loadHistory()
  }

  fun selectAudio(uri: Uri) {
    _selectedAudioUri.value = uri
    audioPlayerManager.stop()
    viewModelScope.launch(Dispatchers.IO) {
      val info = AudioMetadataReader.readMetadata(getApplication(), uri)
      _audioMetadata.value = info
      if (info != null) {
        val baseName = info.name.substringBeforeLast(".") + "_comprimido"
        _options.value = _options.value.copy(customFileName = baseName)
      }
    }
  }

  fun clearSelectedAudio() {
    audioPlayerManager.stop()
    _selectedAudioUri.value = null
    _audioMetadata.value = null
  }

  fun setPreset(preset: CompressionPreset) {
    _options.value = _options.value.copy(
      preset = preset,
      targetBitrateKbps = preset.targetBitrateKbps,
      targetSampleRateHz = preset.targetSampleRateHz,
      channelMode = preset.channelMode
    )
  }

  fun setProfile(profile: AudioCompressionProfile) {
    _options.value = _options.value.copy(profile = profile)
  }

  fun setTargetFormat(format: AudioFormat) {
    _options.value = _options.value.copy(targetFormat = format)
  }

  fun setCustomTargetSizeMb(sizeMb: Float) {
    _options.value = _options.value.copy(
      customTargetSizeMb = sizeMb.coerceIn(1.0f, 100.0f),
      preset = CompressionPreset.TARGET_SIZE_CUSTOM
    )
  }

  fun setBitrate(bitrateKbps: Int) {
    _options.value = _options.value.copy(
      targetBitrateKbps = bitrateKbps,
      preset = CompressionPreset.CUSTOM
    )
  }

  fun setSampleRate(sampleRateHz: Int) {
    _options.value = _options.value.copy(
      targetSampleRateHz = sampleRateHz,
      preset = CompressionPreset.CUSTOM
    )
  }

  fun setChannelMode(mode: AudioChannelMode) {
    _options.value = _options.value.copy(
      channelMode = mode,
      preset = CompressionPreset.CUSTOM
    )
  }

  fun setCustomFileName(name: String) {
    _options.value = _options.value.copy(customFileName = if (name.isBlank()) null else name)
  }

  fun startCompression() {
    val meta = _audioMetadata.value ?: return
    val options = _options.value

    audioPlayerManager.stop()
    _progress.value = CompressionProgress(
      state = CompressionState.ANALYZING,
      progressPercent = 0.05f,
      statusMessage = "Iniciando compresión inteligente..."
    )

    compressionJob = viewModelScope.launch(Dispatchers.IO) {
      val result = AudioCompressorProcessor.compressAudio(
        context = getApplication(),
        inputInfo = meta,
        options = options,
        onProgress = { prog ->
          _progress.value = prog
        }
      )

      if (result.isSuccess) {
        loadHistory()
      }
    }
  }

  fun cancelCompression() {
    compressionJob?.cancel()
    compressionJob = null
    _progress.value = CompressionProgress(state = CompressionState.IDLE)
  }

  fun dismissProgressDialog() {
    _progress.value = CompressionProgress(state = CompressionState.IDLE)
  }

  fun loadHistory() {
    viewModelScope.launch(Dispatchers.IO) {
      val files = AppStorageManager.listFilesForFolder(getApplication(), AppAudioFolder.COMPRIMIR)
      val items = files.map { file ->
        val ext = file.extension.lowercase()
        val format = AudioFormat.values().find { it.extension == ext } ?: AudioFormat.M4A_AAC
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
          bitrateKbps = meta?.bitrateKbps ?: 96
        )
      }.sortedByDescending { it.timestamp }
      _history.value = items
    }
  }

  fun togglePlayFile(file: ConvertedAudioFile) {
    audioPlayerManager.playOrPause(file.file, file.name)
  }

  fun togglePlayOriginal() {
    val uri = _selectedAudioUri.value ?: return
    val title = _audioMetadata.value?.name ?: "Audio Original"
    audioPlayerManager.playOrPause(uri, title)
  }

  fun seekTo(positionMs: Long) {
    audioPlayerManager.seekTo(positionMs)
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
      context.startActivity(Intent.createChooser(intent, "Compartir audio comprimido"))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun deleteFile(file: ConvertedAudioFile) {
    viewModelScope.launch(Dispatchers.IO) {
      if (playbackState.value.currentFile?.absolutePath == file.file.absolutePath) {
        audioPlayerManager.stop()
      }
      try {
        file.file.delete()
      } catch (e: Exception) {
        e.printStackTrace()
      }
      loadHistory()
    }
  }

  override fun onCleared() {
    super.onCleared()
    audioPlayerManager.release()
  }
}
