package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.PlaybackState
import com.example.model.AudioFileInfo
import com.example.model.ConversionOptions
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.ConversionProgressDialog
import com.example.ui.components.ConvertedFilesList
import com.example.ui.components.FormatSelectorSection
import com.example.ui.components.QualitySelectorSection
import com.example.ui.components.converter.ConverterCustomFileNameCard
import com.example.ui.components.converter.ConverterHeader
import com.example.ui.components.converter.ConverterSelectFileCard
import com.example.ui.components.converter.ConverterSelectedAudioCard
import com.example.viewmodel.AudioConverterViewModel

@Composable
fun AudioConverterScreen(
  viewModel: AudioConverterViewModel = viewModel(),
  onNavigateBack: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val selectedAudio by viewModel.selectedAudio.collectAsState()
  val conversionOptions by viewModel.conversionOptions.collectAsState()
  val conversionProgress by viewModel.conversionProgress.collectAsState()
  val convertedFiles by viewModel.convertedFiles.collectAsState()
  val playbackState by viewModel.playerManager.playbackState.collectAsState()
  val selectedTab by viewModel.selectedTab.collectAsState()

  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let { viewModel.selectAudioUri(it) }
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Header Section modular
      ConverterHeader(
        selectedTab = selectedTab,
        convertedCount = convertedFiles.size,
        onTabSelected = { viewModel.setTab(it) },
        onNavigateBack = onNavigateBack
      )

      // Content based on selected tab
      Box(modifier = Modifier.weight(1f)) {
        if (selectedTab == 0) {
          ConverterTabContent(
            selectedAudio = selectedAudio,
            options = conversionOptions,
            onPickFile = { audioPickerLauncher.launch("audio/*") },
            onClearFile = { viewModel.clearSelectedAudio() },
            onFormatSelected = { viewModel.setTargetFormat(it) },
            onPresetSelected = { viewModel.setPreset(it) },
            onBitrateSelected = { viewModel.setBitrate(it) },
            onSampleRateSelected = { viewModel.setSampleRate(it) },
            onChannelModeSelected = { viewModel.setChannelMode(it) },
            onVolumeChanged = { viewModel.setVolumeMultiplier(it) },
            onFileNameChanged = { viewModel.setCustomFileName(it) },
            onStartConvert = { viewModel.startConversion() },
            onPlayInput = { info ->
              viewModel.playerManager.playOrPause(info.uri, info.name)
            },
            playbackState = playbackState,
            onSeekAudio = { viewModel.playerManager.seekTo(it) }
          )
        } else {
          ConvertedFilesList(
            files = convertedFiles,
            playbackState = playbackState,
            onPlayFile = { file ->
              viewModel.playerManager.playOrPause(file.file, file.name)
            },
            onShareFile = { viewModel.shareAudioFile(it) },
            onExportFile = { viewModel.exportToDeviceMusic(it) },
            onDeleteFile = { viewModel.deleteConvertedFile(it) },
            onGoToConverter = { viewModel.setTab(0) },
            onOpenFolder = { viewModel.openOutputFolder(context) },
            folderDisplayPath = "Música / AudioConverter / Convertir"
          )
        }
      }

      // Bottom Player Bar if an audio is playing while browsing
      AnimatedVisibility(
        visible = (playbackState.isPlaying || playbackState.currentPositionMs > 0) &&
          (selectedTab == 1 || selectedAudio == null || (playbackState.currentUri != selectedAudio?.uri)),
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          tonalElevation = 4.dp
        ) {
          AudioPlayerCard(
            playbackState = playbackState,
            onPlayPause = {
              if (playbackState.isPlaying) {
                viewModel.playerManager.pause()
              } else {
                viewModel.playerManager.resume()
              }
            },
            onSeek = { viewModel.playerManager.seekTo(it) },
            modifier = Modifier.padding(4.dp)
          )
        }
      }
    }

    // Conversion Progress / Result Dialog
    ConversionProgressDialog(
      progress = conversionProgress,
      onCancel = { viewModel.cancelConversion() },
      onDismiss = { viewModel.dismissConversionResult() },
      onPlayFile = { file ->
        viewModel.playerManager.playOrPause(file.file, file.name)
      },
      onShareFile = { viewModel.shareAudioFile(it) },
      onExportFile = { viewModel.exportToDeviceMusic(it) }
    )
  }
}

@Composable
private fun ConverterTabContent(
  selectedAudio: AudioFileInfo?,
  options: ConversionOptions,
  onPickFile: () -> Unit,
  onClearFile: () -> Unit,
  onFormatSelected: (com.example.model.AudioFormat) -> Unit,
  onPresetSelected: (com.example.model.QualityPreset) -> Unit,
  onBitrateSelected: (Int) -> Unit,
  onSampleRateSelected: (Int) -> Unit,
  onChannelModeSelected: (com.example.model.AudioChannelMode) -> Unit,
  onVolumeChanged: (Float) -> Unit,
  onFileNameChanged: (String) -> Unit,
  onStartConvert: () -> Unit,
  onPlayInput: (AudioFileInfo) -> Unit,
  playbackState: PlaybackState,
  onSeekAudio: (Long) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(vertical = 16.dp)
  ) {
    if (selectedAudio == null) {
      // 1. Selector inicial y formatos soportados
      item {
        ConverterSelectFileCard(onPickFile = onPickFile)
      }
    } else {
      // 2. Tarjeta con audio seleccionado y mini-reproductor
      item {
        ConverterSelectedAudioCard(
          selectedAudio = selectedAudio,
          playbackState = playbackState,
          onClearFile = onClearFile,
          onPlayInput = onPlayInput,
          onSeekAudio = onSeekAudio
        )
      }

      // 3. Selector de formato
      item {
        FormatSelectorSection(
          selectedFormat = options.targetFormat,
          selectedAudio = selectedAudio,
          onFormatSelected = onFormatSelected
        )
      }

      // 4. Selector de calidad y parámetros avanzados
      item {
        QualitySelectorSection(
          options = options,
          selectedAudio = selectedAudio,
          onPresetSelected = onPresetSelected,
          onBitrateSelected = onBitrateSelected,
          onSampleRateSelected = onSampleRateSelected,
          onChannelModeSelected = onChannelModeSelected,
          onVolumeChanged = onVolumeChanged
        )
      }

      // 5. Nombre de archivo resultante
      item {
        ConverterCustomFileNameCard(
          customFileName = options.customFileName,
          targetFormat = options.targetFormat,
          onFileNameChanged = onFileNameChanged
        )
      }

      // 6. Botón principal de conversión
      item {
        Button(
          onClick = onStartConvert,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("convert_audio_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Convertir a ${options.targetFormat.badge}",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          )
        }
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}
