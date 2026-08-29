package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AudioMetadataReader
import com.example.model.AudioFileInfo
import com.example.model.ConvertedAudioFile
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.ConversionProgressDialog
import com.example.ui.components.ConvertedFilesList
import com.example.ui.components.FormatSelectorSection
import com.example.ui.components.QualitySelectorSection
import com.example.viewmodel.AudioConverterViewModel

@Composable
fun AudioConverterScreen(
  viewModel: AudioConverterViewModel = viewModel(),
  onNavigateBack: (() -> Unit)? = null
) {
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
      // Header Section
      AppHeader(
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
            onGoToConverter = { viewModel.setTab(0) }
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
private fun AppHeader(
  selectedTab: Int,
  convertedCount: Int,
  onTabSelected: (Int) -> Unit,
  onNavigateBack: (() -> Unit)? = null
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        if (onNavigateBack != null) {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
              .size(40.dp)
              .testTag("button_back_to_hub")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Volver al Menú Principal",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              Brush.linearGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.primary,
                  MaterialTheme.colorScheme.tertiary
                )
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Audiotrack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Convertir Audio",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Cambiar formato y calidad",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
      ) {
        Text(
          text = "Sin Internet",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Navigation Tabs
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
      contentColor = MaterialTheme.colorScheme.primary,
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp)),
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          height = 3.dp,
          color = MaterialTheme.colorScheme.primary
        )
      },
      divider = {}
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { onTabSelected(0) },
        text = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Convertir", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
          }
        },
        modifier = Modifier.testTag("tab_convert")
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { onTabSelected(1) },
        text = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mis Audios", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
            if (convertedCount > 0) {
              Spacer(modifier = Modifier.width(6.dp))
              Badge(containerColor = MaterialTheme.colorScheme.primary) {
                Text(convertedCount.toString(), color = MaterialTheme.colorScheme.onPrimary)
              }
            }
          }
        },
        modifier = Modifier.testTag("tab_library")
      )
    }
  }
}

@Composable
private fun ConverterTabContent(
  selectedAudio: AudioFileInfo?,
  options: com.example.model.ConversionOptions,
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
  playbackState: com.example.audio.PlaybackState,
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
      // Empty state / File Picker Box
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onPickFile() }
            .testTag("pick_audio_card"),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.DriveFileMove,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "Selecciona un Archivo de Audio",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
              ),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = "Toca aquí para elegir cualquier canción, nota de voz o grabación de tu teléfono.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = onPickFile,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth(0.85f)
                .testTag("pick_audio_button"),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Elegir de Mis Archivos", fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "Formatos soportados",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "• MP3: Compatible con cualquier reproductor y equipo de música.\n• M4A / AAC: Gran claridad sonora en menor espacio.\n• WAV: Calidad pura sin pérdida (PCM 16-bit).\n• FLAC: Compresión sin pérdidas de grado audiófilo.\n• OGG / Opus: Ultra ligero para mensajería y web.",
              style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      // Selected File Card with metadata & mini-player
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_file_card"),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Text(
                  text = selectedAudio.formatExtension,
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = selectedAudio.name,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${AudioMetadataReader.formatFileSize(selectedAudio.sizeBytes)} • ${AudioMetadataReader.formatDuration(selectedAudio.durationMs)} • ${selectedAudio.sampleRate / 1000.0} kHz",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              IconButton(
                onClick = onClearFile,
                modifier = Modifier
                  .size(36.dp)
                  .testTag("clear_selected_audio_button")
              ) {
                Icon(
                  Icons.Default.Close,
                  contentDescription = "Cambiar archivo",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Embedded player for the selected input
            AudioPlayerCard(
              playbackState = if (playbackState.currentUri == selectedAudio.uri) {
                playbackState
              } else {
                com.example.audio.PlaybackState(
                  isPlaying = false,
                  currentUri = selectedAudio.uri,
                  durationMs = selectedAudio.durationMs,
                  title = selectedAudio.name
                )
              },
              onPlayPause = { onPlayInput(selectedAudio) },
              onSeek = onSeekAudio
            )
          }
        }
      }

      // Format Selector Section
      item {
        FormatSelectorSection(
          selectedFormat = options.targetFormat,
          selectedAudio = selectedAudio,
          onFormatSelected = onFormatSelected
        )
      }

      // Quality & Advanced Settings Section
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

      // Output file name
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "Nombre del archivo resultante",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = options.customFileName,
              onValueChange = onFileNameChanged,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("output_filename_input"),
              shape = RoundedCornerShape(12.dp),
              singleLine = true,
              trailingIcon = {
                Text(
                  text = ".${options.targetFormat.extension}",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(end = 12.dp)
                )
              },
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
              )
            )
          }
        }
      }

      // Primary Convert Button
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
