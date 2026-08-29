package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AppAudioFolder
import com.example.audio.AppStorageManager
import com.example.model.Spatial8DState
import com.example.ui.components.spatial.Spatial8DProgressDialog
import com.example.ui.components.spatial.SpatialAudioSourceCard
import com.example.ui.components.spatial.SpatialExportSettingsCard
import com.example.ui.components.spatial.SpatialHistoryEmptyState
import com.example.ui.components.spatial.SpatialHistoryHeader
import com.example.ui.components.spatial.SpatialHistoryItemCard
import com.example.ui.components.spatial.SpatialOrbitalRadar
import com.example.ui.components.spatial.SpatialTrajectorySettingsCard
import com.example.viewmodel.Spatial8DViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Spatial8DScreen(
  onNavigateBack: () -> Unit,
  viewModel: Spatial8DViewModel = viewModel()
) {
  val context = LocalContext.current
  val selectedUri by viewModel.selectedAudioUri.collectAsState()
  val audioMetadata by viewModel.audioMetadata.collectAsState()
  val options by viewModel.options.collectAsState()
  val progress by viewModel.progress.collectAsState()
  val history by viewModel.convertedHistory.collectAsState()
  val playbackState by viewModel.playbackState.collectAsState()

  var customNameInput by remember { mutableStateOf("") }

  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let { viewModel.selectAudio(it) }
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.SpatialAudio,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Audio 8D Espacial",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("spatial_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Volver al menú"
            )
          }
        },
        actions = {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.padding(end = 12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Auriculares",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
            }
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Radar Visualizador 360°
      item {
        Spacer(modifier = Modifier.height(2.dp))
        SpatialOrbitalRadar(
          trajectory = options.trajectory,
          speed = options.rotationSpeed,
          depth = options.spatialDepth,
          isProcessing = progress.state != Spatial8DState.IDLE && progress.state != Spatial8DState.COMPLETED
        )
      }

      // 2. Selector de archivo de audio
      item {
        SpatialAudioSourceCard(
          selectedUri = selectedUri,
          audioMetadata = audioMetadata,
          playbackState = playbackState,
          onPickAudio = { audioPickerLauncher.launch("audio/*") },
          onTogglePlayOriginal = { viewModel.togglePlayOriginal() }
        )
      }

      // 3. Ajustes de Trayectoria 8D, Velocidad, Profundidad y Reverb
      item {
        SpatialTrajectorySettingsCard(
          options = options,
          onTrajectorySelected = { viewModel.updateTrajectory(it) },
          onSpeedSelected = { viewModel.updateRotationSpeed(it) },
          onDepthChanged = { viewModel.updateSpatialDepth(it) },
          onReverbSelected = { viewModel.updateReverbPreset(it) }
        )
      }

      // 4. Formato de Salida, Bitrate y Nombre Personalizado
      item {
        SpatialExportSettingsCard(
          options = options,
          customFileName = customNameInput,
          onFormatSelected = { viewModel.updateTargetFormat(it) },
          onBitrateSelected = { viewModel.updateBitrate(it) },
          onFileNameChanged = {
            customNameInput = it
            viewModel.updateCustomFileName(it)
          }
        )
      }

      // 5. Botón de Acción Principal
      item {
        val isReady = selectedUri != null
        Button(
          onClick = { viewModel.startProcessing() },
          enabled = isReady,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("spatial_convert_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Icon(Icons.Default.SpatialAudio, contentDescription = null, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Convertir a Audio 8D Inmersivo",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }

      // 6. Sección de Audios 8D Creados (Historial)
      item {
        SpatialHistoryHeader(
          count = history.size,
          onOpenFolder = { AppStorageManager.openFolderInFileManager(context, AppAudioFolder.AUDIO_8D) }
        )
      }

      if (history.isEmpty()) {
        item {
          SpatialHistoryEmptyState()
        }
      } else {
        items(history, key = { it.id }) { file ->
          SpatialHistoryItemCard(
            file = file,
            playbackState = playbackState,
            onTogglePlay = { viewModel.togglePlayFile(file) },
            onSeek = { viewModel.seekTo(it) },
            onShare = { viewModel.shareFile(context, file) },
            onDelete = { viewModel.deleteFile(file) }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // Dialog de Progreso y Finalización
  Spatial8DProgressDialog(
    progress = progress,
    options = options,
    playbackState = playbackState,
    onCancel = { viewModel.cancelProcessing() },
    onDismiss = { viewModel.dismissProgressDialog() },
    onTogglePlay = { viewModel.togglePlayFile(it) },
    onSeekTo = { viewModel.seekTo(it) },
    onShare = { viewModel.shareFile(context, it) }
  )
}
