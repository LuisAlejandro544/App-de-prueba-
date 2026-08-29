package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AppAudioFolder
import com.example.audio.AppStorageManager
import com.example.audio.AudioMetadataReader
import com.example.audio.AudioPlayerManager
import com.example.audio.PlaybackState
import com.example.audio.VideoAudioExtractor
import com.example.audio.VideoMetadataReader
import com.example.model.AudioFormat
import com.example.model.ConvertedAudioFile
import com.example.model.ExtractionMode
import com.example.model.QualityPreset
import com.example.model.VideoExtractionOptions
import com.example.model.VideoExtractionProgress
import com.example.model.VideoExtractionState
import com.example.model.VideoFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class VideoToAudioViewModel(application: Application) : AndroidViewModel(application) {

  private val audioPlayerManager = AudioPlayerManager(application, viewModelScope)
  val playbackState: StateFlow<PlaybackState> = audioPlayerManager.playbackState

  private val _selectedVideo = MutableStateFlow<VideoFileInfo?>(null)
  val selectedVideo: StateFlow<VideoFileInfo?> = _selectedVideo.asStateFlow()

  private val _options = MutableStateFlow(VideoExtractionOptions())
  val options: StateFlow<VideoExtractionOptions> = _options.asStateFlow()

  private val _progress = MutableStateFlow(VideoExtractionProgress())
  val progress: StateFlow<VideoExtractionProgress> = _progress.asStateFlow()

  private val _isAnalyzing = MutableStateFlow(false)
  val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

  private val _extractedHistory = MutableStateFlow<List<ConvertedAudioFile>>(emptyList())
  val extractedHistory: StateFlow<List<ConvertedAudioFile>> = _extractedHistory.asStateFlow()

  private var extractionJob: Job? = null

  init {
    loadExtractedHistory()
  }

  fun loadExtractedHistory() {
    viewModelScope.launch(Dispatchers.IO) {
      val videoAudioFolder = AppStorageManager.getFolder(getApplication(), AppAudioFolder.VIDEO_A_AUDIO)
      val legacyDir = File(getApplication<Application>().filesDir, "extracted_audio")

      // Mover archivos legacy si existen
      if (legacyDir.exists()) {
        legacyDir.listFiles()?.forEach { legacyFile ->
          if (legacyFile.isFile) {
            val dest = File(videoAudioFolder, legacyFile.name)
            if (!dest.exists()) {
              legacyFile.copyTo(dest, overwrite = true)
            }
            legacyFile.delete()
          }
        }
      }

      val files = videoAudioFolder.listFiles() ?: emptyArray()
      val items = files.filter { it.isFile && it.length() > 0 }.map { file ->
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
          bitrateKbps = meta?.bitrateKbps ?: 192
        )
      }.sortedByDescending { it.timestamp }

      _extractedHistory.value = items
    }
  }

  fun selectVideo(context: Context, uri: Uri) {
    _isAnalyzing.value = true
    viewModelScope.launch {
      val info = VideoMetadataReader.readVideoMetadata(context, uri)
      _selectedVideo.value = info
      _isAnalyzing.value = false

      if (info != null) {
        val suggestedBitrate = info.audioBitrateKbps.coerceIn(64, 320)
        _options.update { current ->
          current.copy(
            bitrateKbps = suggestedBitrate,
            sampleRateHz = info.audioSampleRate,
            customFileName = info.name.substringBeforeLast(".") + "_audio"
          )
        }
      }
    }
  }

  fun setTargetFormat(format: AudioFormat) {
    _options.update { it.copy(targetFormat = format) }
  }

  fun setExtractionMode(mode: ExtractionMode) {
    _options.update { it.copy(mode = mode) }
  }

  fun setQualityPreset(preset: QualityPreset) {
    val currentVideo = _selectedVideo.value
    val baseBitrate = currentVideo?.audioBitrateKbps ?: 192
    val maxBitrate = baseBitrate.coerceAtLeast(64)

    val targetBitrate = when (preset) {
      QualityPreset.ORIGINAL -> baseBitrate
      QualityPreset.ULTRA -> 320.coerceAtMost(maxBitrate)
      QualityPreset.HIGH -> 256.coerceAtMost(maxBitrate)
      QualityPreset.STANDARD -> 192.coerceAtMost(maxBitrate)
      QualityPreset.ECONOMY -> 128.coerceAtMost(maxBitrate)
      QualityPreset.VOICE -> 64
      QualityPreset.CUSTOM -> _options.value.bitrateKbps
    }

    _options.update { it.copy(preset = preset, bitrateKbps = targetBitrate) }
  }

  fun setBitrate(bitrateKbps: Int) {
    _options.update { it.copy(bitrateKbps = bitrateKbps, preset = QualityPreset.CUSTOM) }
  }

  fun setVolume(volumeMultiplier: Float) {
    _options.update { it.copy(volumeMultiplier = volumeMultiplier) }
  }

  fun setCustomFileName(name: String) {
    _options.update { it.copy(customFileName = name) }
  }

  fun startExtraction(context: Context) {
    val video = _selectedVideo.value ?: return

    extractionJob?.cancel()
    _progress.value = VideoExtractionProgress(
      state = VideoExtractionState.EXTRACTING,
      progressPercent = 0.05f,
      statusMessage = "Iniciando extracción de audio..."
    )

    extractionJob = viewModelScope.launch {
      val result = VideoAudioExtractor.extractAudio(
        context = context,
        videoInfo = video,
        options = _options.value,
        onProgress = { prog, msg ->
          _progress.value = _progress.value.copy(
            progressPercent = prog,
            statusMessage = msg
          )
        }
      )

      result.fold(
        onSuccess = { extractedFile ->
          _progress.value = VideoExtractionProgress(
            state = VideoExtractionState.COMPLETED,
            progressPercent = 1.0f,
            statusMessage = "¡Extracción completada con éxito!",
            extractedFile = extractedFile
          )
          _extractedHistory.update { listOf(extractedFile) + it }
        },
        onFailure = { error ->
          _progress.value = VideoExtractionProgress(
            state = VideoExtractionState.ERROR,
            progressPercent = 0f,
            statusMessage = "Error en la extracción",
            errorMessage = error.localizedMessage ?: "Error desconocido durante la extracción de audio"
          )
        }
      )
    }
  }

  fun cancelExtraction() {
    extractionJob?.cancel()
    _progress.value = VideoExtractionProgress(
      state = VideoExtractionState.IDLE,
      statusMessage = "Extracción cancelada por el usuario"
    )
  }

  fun dismissProgressDialog() {
    _progress.value = _progress.value.copy(state = VideoExtractionState.IDLE)
  }

  fun clearSelectedVideo() {
    _selectedVideo.value = null
  }

  fun shareExtractedFile(context: Context, file: ConvertedAudioFile) {
    try {
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file.file
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = file.format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      val chooser = Intent.createChooser(shareIntent, "Compartir audio extraído")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(context, "No se pudo compartir el archivo: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun saveToMusicFolder(context: Context, file: ConvertedAudioFile) {
    viewModelScope.launch(Dispatchers.IO) {
      val success = AppStorageManager.exportToPublicMusicFolder(
        context = context,
        sourceFile = file.file,
        mimeType = file.format.mimeType,
        folderType = AppAudioFolder.VIDEO_A_AUDIO,
        customDisplayName = file.name
      )

      withContext(Dispatchers.Main) {
        if (success) {
          Toast.makeText(context, "Guardado en: ${AppStorageManager.getDisplayPath(context, AppAudioFolder.VIDEO_A_AUDIO)}", Toast.LENGTH_LONG).show()
        } else {
          Toast.makeText(context, "Error al exportar a la carpeta pública", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  fun openOutputFolder(context: Context) {
    AppStorageManager.openFolderInFileManager(context, AppAudioFolder.VIDEO_A_AUDIO)
  }

  fun togglePlayFile(context: Context, file: ConvertedAudioFile) {
    audioPlayerManager.playOrPause(file.file, file.name)
  }

  fun togglePlayPause() {
    val state = playbackState.value
    if (state.isPlaying) {
      audioPlayerManager.pause()
    } else {
      audioPlayerManager.resume()
    }
  }

  fun seekTo(positionMs: Long) {
    audioPlayerManager.seekTo(positionMs)
  }

  fun deleteExtractedFile(file: ConvertedAudioFile) {
    try {
      if (file.file.exists()) {
        file.file.delete()
      }
      _extractedHistory.update { list -> list.filter { it.id != file.id } }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onCleared() {
    super.onCleared()
    audioPlayerManager.release()
  }
}
