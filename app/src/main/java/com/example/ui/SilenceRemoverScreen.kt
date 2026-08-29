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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AppAudioFolder
import com.example.audio.AppStorageManager
import com.example.model.ConvertedAudioFile
import com.example.ui.components.silence.SilenceProgressDialog
import com.example.ui.components.silence.SilenceSettingsCard
import com.example.ui.components.spatial.SpatialExportSettingsCard
import com.example.viewmodel.SilenceRemoverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilenceRemoverScreen(
  onNavigateBack: () -> Unit,
  viewModel: SilenceRemoverViewModel = viewModel()
) {
  val context = LocalContext.current
  val selectedUri by viewModel.selectedAudioUri.collectAsState()
  val audioMetadata by viewModel.audioMetadata.collectAsState()
  val options by viewModel.options.collectAsState()
  val progress by viewModel.progress.collectAsState()
  val history by viewModel.history.collectAsState()
  val playbackState by viewModel.playbackState.collectAsState()
  val isLoadingSample by viewModel.isLoadingSample.collectAsState()

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
              imageVector = Icons.Default.ContentCut,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Eliminar Silencios",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("silence_back_button")
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
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.padding(end = 12.dp)
          ) {
            Text(
              text = "Smart Cut",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
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
      // 1. Selector de Pista de Audio
      item {
        Spacer(modifier = Modifier.height(2.dp))
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Pista de Audio Origen",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedUri == null) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Button(
                  onClick = { audioPickerLauncher.launch("audio/*") },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("silence_pick_audio_button"),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Elegir Audio")
                }

                OutlinedButton(
                  onClick = { viewModel.generateSampleAudio() },
                  enabled = !isLoadingSample,
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  if (isLoadingSample) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                  } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Muestra Demo")
                  }
                }
              }
            } else {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = audioMetadata?.name ?: "Audio Seleccionado",
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${audioMetadata?.formattedDuration ?: "--:--"} • ${audioMetadata?.formatExtension?.uppercase() ?: "AUDIO"} • ${audioMetadata?.sampleRate ?: 44100} Hz",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  IconButton(
                    onClick = { viewModel.togglePlayOriginal() },
                    modifier = Modifier
                      .size(36.dp)
                      .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                  ) {
                    val isPlayingOrig = playbackState.isPlaying && playbackState.currentUri == selectedUri
                    Icon(
                      imageVector = if (isPlayingOrig) Icons.Default.Pause else Icons.Default.PlayArrow,
                      contentDescription = "Preescuchar",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp)
                    )
                  }

                  Spacer(modifier = Modifier.width(4.dp))

                  IconButton(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Refresh,
                      contentDescription = "Cambiar",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 2. Configuración de Umbral de Detección, Pausa Mínima, Padding y Modo
      item {
        SilenceSettingsCard(
          options = options,
          onThresholdSelected = { viewModel.updateThresholdLevel(it) },
          onMinDurationChanged = { viewModel.updateMinSilenceDuration(it) },
          onPaddingChanged = { viewModel.updatePaddingVoice(it) },
          onModeSelected = { viewModel.updateCutMode(it) }
        )
      }

      // 3. Configuración de Exportación (Formato, Bitrate y Nombre)
      item {
        SpatialExportSettingsCard(
          options = com.example.model.Spatial8DOptions(
            targetFormat = options.targetFormat,
            bitrateKbps = options.bitrateKbps
          ),
          customFileName = customNameInput,
          onFormatSelected = { viewModel.updateTargetFormat(it) },
          onBitrateSelected = { viewModel.updateBitrate(it) },
          onFileNameChanged = {
            customNameInput = it
            viewModel.updateCustomFileName(it)
          }
        )
      }

      // 4. Botón Principal de Procesamiento
      item {
        val isReady = selectedUri != null
        Button(
          onClick = { viewModel.startProcessing() },
          enabled = isReady,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("silence_process_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          )
        ) {
          Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Limpiar y Eliminar Silencios",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }

      // 5. Historial de Audios Optimizados
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Audios Optimizados (${history.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.weight(1f))
          FilledTonalButton(
            onClick = { AppStorageManager.openFolderInFileManager(context, AppAudioFolder.SIN_SILENCIO) },
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Abrir Carpeta", fontSize = 11.sp)
          }
        }
      }

      if (history.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Aún no has procesado audios sin silencio",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Selecciona un audio o prueba la muestra de demostración.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              )
            }
          }
        }
      } else {
        items(history, key = { it.id }) { file ->
          val isPlaying = playbackState.isPlaying && playbackState.currentFile?.absolutePath == file.file.absolutePath

          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                IconButton(
                  onClick = { viewModel.togglePlayFile(file) },
                  modifier = Modifier
                    .size(38.dp)
                    .background(
                      if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                      CircleShape
                    )
                ) {
                  Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${file.format.displayName} • ${file.formattedSize} • ${file.formattedDuration}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                IconButton(
                  onClick = { viewModel.shareFile(context, file) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(16.dp))
                }

                IconButton(
                  onClick = { viewModel.deleteFile(file) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              if (isPlaying && playbackState.durationMs > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                  value = playbackState.currentPositionMs.toFloat().coerceIn(0f, playbackState.durationMs.toFloat()),
                  onValueChange = { viewModel.seekTo(it.toLong()) },
                  valueRange = 0f..playbackState.durationMs.toFloat(),
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // Diálogo de Progreso
  SilenceProgressDialog(
    progress = progress,
    playbackState = playbackState,
    onCancel = { viewModel.cancelProcessing() },
    onDismiss = { viewModel.dismissProgressDialog() },
    onTogglePlay = { viewModel.togglePlayFile(it) },
    onSeekTo = { viewModel.seekTo(it) },
    onShare = { viewModel.shareFile(context, it) }
  )
}
