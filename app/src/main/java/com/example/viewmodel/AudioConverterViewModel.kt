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
import com.example.audio.AudioTranscoder
import com.example.model.AudioChannelMode
import com.example.model.AudioFileInfo
import com.example.model.AudioFormat
import com.example.model.ConversionOptions
import com.example.model.ConversionProgress
import com.example.model.ConversionState
import com.example.model.ConvertedAudioFile
import com.example.model.QualityPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AudioConverterViewModel(application: Application) : AndroidViewModel(application) {

  val playerManager = AudioPlayerManager(application, viewModelScope)

  private val _selectedAudio = MutableStateFlow<AudioFileInfo?>(null)
  val selectedAudio: StateFlow<AudioFileInfo?> = _selectedAudio.asStateFlow()

  private val _conversionOptions = MutableStateFlow(ConversionOptions())
  val conversionOptions: StateFlow<ConversionOptions> = _conversionOptions.asStateFlow()

  private val _conversionProgress = MutableStateFlow(ConversionProgress())
  val conversionProgress: StateFlow<ConversionProgress> = _conversionProgress.asStateFlow()

  private val _convertedFiles = MutableStateFlow<List<ConvertedAudioFile>>(emptyList())
  val convertedFiles: StateFlow<List<ConvertedAudioFile>> = _convertedFiles.asStateFlow()

  private val _selectedTab = MutableStateFlow(0) // 0: Convertidor, 1: Biblioteca
  val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

  private var conversionJob: Job? = null

  init {
    loadSavedFiles()
  }

  fun setTab(index: Int) {
    _selectedTab.value = index
  }

  fun selectAudioUri(uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      val info = AudioMetadataReader.readMetadata(getApplication(), uri)
      if (info != null) {
        _selectedAudio.value = info
        val defaultName = info.name.substringBeforeLast(".") + "_converted"
        
        // Auto-adaptar y limitar opciones a la calidad detectada del archivo original
        val clampedBitrate = _conversionOptions.value.bitrateKbps.coerceAtMost(info.bitrateKbps)
        val clampedSampleRate = _conversionOptions.value.sampleRateHz.coerceAtMost(info.sampleRate)
        val safePreset = if (_conversionOptions.value.preset.bitrateKbps > info.bitrateKbps) {
          QualityPreset.ORIGINAL
        } else {
          _conversionOptions.value.preset
        }

        _conversionOptions.value = _conversionOptions.value.copy(
          customFileName = defaultName,
          bitrateKbps = clampedBitrate,
          sampleRateHz = clampedSampleRate,
          preset = safePreset
        )
      } else {
        withContext(Dispatchers.Main) {
          Toast.makeText(getApplication(), "No se pudo leer el archivo de audio seleccionado", Toast.LENGTH_LONG).show()
        }
      }
    }
  }

  fun clearSelectedAudio() {
    playerManager.stop()
    _selectedAudio.value = null
  }

  fun setTargetFormat(format: AudioFormat) {
    val maxAllowedBitrate = _selectedAudio.value?.bitrateKbps ?: format.recommendedBitrateKbps
    val finalBitrate = format.recommendedBitrateKbps.coerceAtMost(maxAllowedBitrate)
    _conversionOptions.value = _conversionOptions.value.copy(
      targetFormat = format,
      bitrateKbps = finalBitrate
    )
  }

  fun setPreset(preset: QualityPreset) {
    val audio = _selectedAudio.value
    val maxBitrate = audio?.bitrateKbps ?: 320
    val maxSampleRate = audio?.sampleRate ?: 48000

    val targetBitrate = if (preset.bitrateKbps > 0) {
      preset.bitrateKbps.coerceAtMost(maxBitrate)
    } else {
      _conversionOptions.value.bitrateKbps.coerceAtMost(maxBitrate)
    }

    val targetSampleRate = if (preset.sampleRateHz > 0) {
      preset.sampleRateHz.coerceAtMost(maxSampleRate)
    } else {
      _conversionOptions.value.sampleRateHz.coerceAtMost(maxSampleRate)
    }

    _conversionOptions.value = _conversionOptions.value.copy(
      preset = preset,
      bitrateKbps = targetBitrate,
      sampleRateHz = targetSampleRate
    )
  }

  fun setBitrate(bitrateKbps: Int) {
    val maxBitrate = _selectedAudio.value?.bitrateKbps ?: 320
    val finalBitrate = bitrateKbps.coerceAtMost(maxBitrate)
    _conversionOptions.value = _conversionOptions.value.copy(
      bitrateKbps = finalBitrate,
      preset = QualityPreset.CUSTOM
    )
  }

  fun setSampleRate(sampleRateHz: Int) {
    val maxSampleRate = _selectedAudio.value?.sampleRate ?: 48000
    val finalSampleRate = sampleRateHz.coerceAtMost(maxSampleRate)
    _conversionOptions.value = _conversionOptions.value.copy(
      sampleRateHz = finalSampleRate,
      preset = QualityPreset.CUSTOM
    )
  }

  fun setChannelMode(mode: AudioChannelMode) {
    _conversionOptions.value = _conversionOptions.value.copy(channelMode = mode)
  }

  fun setVolumeMultiplier(volume: Float) {
    _conversionOptions.value = _conversionOptions.value.copy(volumeMultiplier = volume)
  }

  fun setCustomFileName(name: String) {
    _conversionOptions.value = _conversionOptions.value.copy(customFileName = name)
  }

  fun startConversion() {
    val input = _selectedAudio.value ?: return
    val options = _conversionOptions.value

    playerManager.stop()
    _conversionProgress.value = ConversionProgress(
      state = ConversionState.PREPARING,
      progressPercent = 0.05f,
      statusMessage = "Iniciando proceso..."
    )

    conversionJob = viewModelScope.launch(Dispatchers.IO) {
      val result = AudioTranscoder.convertAudio(
        context = getApplication(),
        inputInfo = input,
        options = options,
        onProgress = { prog, msg ->
          _conversionProgress.value = _conversionProgress.value.copy(
            state = if (prog < 0.5f) ConversionState.DECODING else ConversionState.ENCODING,
            progressPercent = prog,
            statusMessage = msg
          )
        }
      )

      result.fold(
        onSuccess = { converted ->
          _conversionProgress.value = ConversionProgress(
            state = ConversionState.COMPLETED,
            progressPercent = 1.0f,
            statusMessage = "¡Conversión exitosa!",
            convertedFile = converted
          )
          loadSavedFiles()
        },
        onFailure = { err ->
          _conversionProgress.value = ConversionProgress(
            state = ConversionState.ERROR,
            progressPercent = 0f,
            statusMessage = "Error en la conversión",
            errorMessage = err.localizedMessage ?: "Ocurrió un error inesperado al convertir el archivo."
          )
        }
      )
    }
  }

  fun cancelConversion() {
    conversionJob?.cancel()
    conversionJob = null
    _conversionProgress.value = ConversionProgress(state = ConversionState.IDLE)
  }

  fun dismissConversionResult() {
    _conversionProgress.value = ConversionProgress(state = ConversionState.IDLE)
  }

  fun loadSavedFiles() {
    viewModelScope.launch(Dispatchers.IO) {
      val convertFolder = AppStorageManager.getFolder(getApplication(), AppAudioFolder.CONVERTIR)
      val legacyDir = File(getApplication<Application>().filesDir, "converted")

      // Mover archivos legacy si existen a la nueva subcarpeta accesible
      if (legacyDir.exists()) {
        legacyDir.listFiles()?.forEach { legacyFile ->
          if (legacyFile.isFile) {
            val dest = File(convertFolder, legacyFile.name)
            if (!dest.exists()) {
              legacyFile.copyTo(dest, overwrite = true)
            }
            legacyFile.delete()
          }
        }
      }

      val files = convertFolder.listFiles() ?: emptyArray()
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

      _convertedFiles.value = items
    }
  }

  fun openOutputFolder(context: Context) {
    AppStorageManager.openFolderInFileManager(context, AppAudioFolder.CONVERTIR)
  }

  fun deleteConvertedFile(item: ConvertedAudioFile) {
    viewModelScope.launch(Dispatchers.IO) {
      if (playerManager.playbackState.value.currentFile?.absolutePath == item.file.absolutePath) {
        playerManager.stop()
      }
      try {
        item.file.delete()
      } catch (e: Exception) {
        e.printStackTrace()
      }
      loadSavedFiles()
    }
  }

  fun shareAudioFile(item: ConvertedAudioFile) {
    try {
      val context = getApplication<Application>()
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        item.file
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = item.format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      val chooser = Intent.createChooser(shareIntent, "Compartir audio con...").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(getApplication(), "No se pudo compartir el archivo: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun exportToDeviceMusic(item: ConvertedAudioFile) {
    viewModelScope.launch(Dispatchers.IO) {
      val context = getApplication<Application>()
      val success = AppStorageManager.exportToPublicMusicFolder(
        context = context,
        sourceFile = item.file,
        mimeType = item.format.mimeType,
        folderType = AppAudioFolder.CONVERTIR,
        customDisplayName = item.name
      )

      withContext(Dispatchers.Main) {
        if (success) {
          Toast.makeText(context, "Archivo guardado en: ${AppStorageManager.getDisplayPath(context, AppAudioFolder.CONVERTIR)}", Toast.LENGTH_LONG).show()
        } else {
          Toast.makeText(context, "No se pudo exportar a la carpeta pública", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    playerManager.release()
  }
}
