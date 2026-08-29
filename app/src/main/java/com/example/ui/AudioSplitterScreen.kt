package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AppAudioFolder
import com.example.audio.AppStorageManager
import com.example.model.ConvertedAudioFile
import com.example.model.SplitProcessingState
import com.example.ui.components.split.DetectedTracksPreviewCard
import com.example.ui.components.split.SplitProgressDialog
import com.example.ui.components.split.SplitSettingsCard
import com.example.viewmodel.AudioSplitterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSplitterScreen(
  onNavigateBack: () -> Unit,
  viewModel: AudioSplitterViewModel = viewModel()
) {
  val context = LocalContext.current
  val selectedUri by viewModel.selectedAudioUri.collectAsState()
  val audioMetadata by viewModel.audioMetadata.collectAsState()
  val options by viewModel.options.collectAsState()
  val detectedSegments by viewModel.detectedSegments.collectAsState()
  val progress by viewModel.progress.collectAsState()
  val history by viewModel.history.collectAsState()
  val playbackState by viewModel.playbackState.collectAsState()

  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let { viewModel.selectAudio(it) }
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Dividir por Silencios", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Separar audio en múltiples pistas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("split_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al Menú")
          }
        },
        actions = {
          IconButton(
            onClick = { AppStorageManager.openFolderInFileManager(context, AppAudioFolder.DIVIDIR) },
            modifier = Modifier.testTag("split_open_folder_button")
          ) {
            Icon(Icons.Default.Folder, contentDescription = "Abrir Carpeta Dividir")
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
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        // 1. Selector de Audio
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("Audio Origen a Dividir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (selectedUri == null) {
              Button(
                onClick = { audioPickerLauncher.launch("audio/*") },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("split_pick_audio_button"),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Elegir Archivo de Audio")
              }
            } else {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  IconButton(
                    onClick = { viewModel.togglePlayOriginal() },
                    modifier = Modifier
                      .size(40.dp)
                      .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                  ) {
                    Icon(
                      if (playbackState.isPlaying && playbackState.currentUri == selectedUri) Icons.Default.Pause else Icons.Default.PlayArrow,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      audioMetadata?.name ?: "Archivo seleccionado",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      "Duración: ${formatDuration(audioMetadata?.durationMs ?: 0L)} | ${(audioMetadata?.sizeBytes ?: 0L) / (1024 * 1024)} MB",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  IconButton(onClick = { audioPickerLauncher.launch("audio/*") }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Cambiar archivo", tint = MaterialTheme.colorScheme.primary)
                  }
                }
              }
            }
          }
        }
      }

      if (selectedUri != null) {
        item {
          // 2. Tarjeta de Configuración
          SplitSettingsCard(
            options = options,
            onSensitivityChange = { viewModel.updateSensitivity(it) },
            onMinSilenceDurationChange = { viewModel.updateMinSilenceDuration(it) },
            onTargetFormatChange = { viewModel.updateTargetFormat(it) },
            onBaseNameChange = { viewModel.updateBaseFileName(it) },
            onZipToggle = { viewModel.updateZipAllTracks(it) }
          )
        }

        item {
          // 3. Botón de Análisis o Exportación
          if (detectedSegments.isEmpty()) {
            Button(
              onClick = { viewModel.analyzeAudio() },
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("split_analyze_button"),
              shape = RoundedCornerShape(14.dp)
            ) {
              Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Analizar y Detectar Pistas", fontWeight = FontWeight.Bold)
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              DetectedTracksPreviewCard(
                segments = detectedSegments,
                onToggleTrack = { viewModel.toggleTrackSelection(it) },
                onUpdateTrackTitle = { idx, title -> viewModel.updateTrackCustomTitle(idx, title) },
                onPlayPreview = { start, dur -> viewModel.playTrackPreview(start, dur) },
                onSelectAll = { viewModel.selectAllTracks(it) }
              )

              Button(
                onClick = { viewModel.exportTracks() },
                enabled = detectedSegments.any { it.isSelected },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .testTag("split_export_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar ${detectedSegments.count { it.isSelected }} Pistas Seleccionadas", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 4. Historial de Pistas Divididas
      item {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text("Pistas Divididas Guardadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          IconButton(onClick = { viewModel.refreshHistory() }) {
            Icon(Icons.Default.Refresh, contentDescription = "Actualizar", modifier = Modifier.size(18.dp))
          }
        }
      }

      if (history.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text("No hay pistas divididas aún.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("Los archivos exportados aparecerán aquí y en Música/AudioConverter/Dividir.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
          }
        }
      } else {
        items(history, key = { it.id }) { item ->
          HistorySplitItemCard(
            item = item,
            isPlaying = playbackState.isPlaying && playbackState.currentFile?.absolutePath == item.file.absolutePath,
            onTogglePlay = { viewModel.togglePlayFile(item.file) },
            onShare = { viewModel.shareFile(context, item.file, item.format.mimeType) },
            onDelete = { viewModel.deleteFile(item.file) }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // Diálogo de Progreso
  SplitProgressDialog(
    progress = progress,
    playbackState = playbackState,
    onDismiss = { viewModel.cancelProcessing() },
    onCancel = { viewModel.cancelProcessing() },
    onTogglePlay = { viewModel.togglePlayFile(it) },
    onShareFile = { file, mime -> viewModel.shareFile(context, file, mime) }
  )
}

@Composable
private fun HistorySplitItemCard(
  item: ConvertedAudioFile,
  isPlaying: Boolean,
  onTogglePlay: () -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onTogglePlay,
        modifier = Modifier
          .size(38.dp)
          .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
      ) {
        Icon(
          if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          item.name,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          "${item.format.displayName} | ${formatDuration(item.durationMs)} | ${item.sizeBytes / 1024} KB",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
      }
      IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
      }
    }
  }
}

private fun formatDuration(ms: Long): String {
  val totalSec = ms / 1000
  val minutes = totalSec / 60
  val seconds = totalSec % 60
  return String.format("%02d:%02d", minutes, seconds)
}
