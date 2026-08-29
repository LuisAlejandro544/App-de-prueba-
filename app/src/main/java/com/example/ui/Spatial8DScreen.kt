package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AppAudioFolder
import com.example.audio.AppStorageManager
import com.example.model.AudioFormat
import com.example.model.ConvertedAudioFile
import com.example.model.Spatial8DState
import com.example.model.SpatialReverbPreset
import com.example.model.SpatialRotationSpeed
import com.example.model.SpatialTrajectory
import com.example.ui.components.spatial.Spatial8DProgressDialog
import com.example.ui.components.spatial.SpatialOrbitalRadar
import com.example.viewmodel.Spatial8DViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                    .testTag("spatial_pick_audio_button"),
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
              // Audio seleccionado
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

      // 3. Ajustes de Trayectoria 8D
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Patrón de Movimiento Espacial",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = options.trajectory.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              SpatialTrajectory.values().forEach { traj ->
                val selected = options.trajectory == traj
                FilterChip(
                  selected = selected,
                  onClick = { viewModel.updateTrajectory(traj) },
                  label = { Text(traj.displayName, fontSize = 12.sp) },
                  leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                  } else null
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Velocidad de Rotación
            Text(
              text = "Velocidad de Rotación",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              SpatialRotationSpeed.values().forEach { spd ->
                val selected = options.rotationSpeed == spd
                FilterChip(
                  selected = selected,
                  onClick = { viewModel.updateRotationSpeed(spd) },
                  label = { Text(spd.displayName, fontSize = 12.sp) },
                  leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                  } else null
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Slider de Profundidad Binaural
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Profundidad Binaural 3D",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.weight(1f))
              Text(
                text = "${(options.spatialDepth * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
            }

            Slider(
              value = options.spatialDepth,
              onValueChange = { viewModel.updateSpatialDepth(it) },
              valueRange = 0.2f..1.0f,
              modifier = Modifier.fillMaxWidth()
            )

            // Reverb Acústica de Sala
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Ambiente Acústico (Reverb)",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = options.reverbPreset.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              SpatialReverbPreset.values().forEach { rev ->
                val selected = options.reverbPreset == rev
                FilterChip(
                  selected = selected,
                  onClick = { viewModel.updateReverbPreset(rev) },
                  label = { Text(rev.displayName, fontSize = 12.sp) },
                  leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                  } else null
                )
              }
            }
          }
        }
      }

      // 4. Formato de Salida y Bitrate
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Formato de Exportación",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            val formats = listOf(AudioFormat.MP3, AudioFormat.M4A_AAC, AudioFormat.WAV, AudioFormat.FLAC, AudioFormat.OGG_OPUS, AudioFormat.OPUS)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              formats.forEach { fmt ->
                val selected = options.targetFormat == fmt
                FilterChip(
                  selected = selected,
                  onClick = { viewModel.updateTargetFormat(fmt) },
                  label = { Text(fmt.displayName, fontSize = 12.sp) },
                  leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                  } else null
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bitrates si aplica
            if (options.targetFormat.supportsBitrateCustomization) {
              Text(
                text = "Calidad de Audio (Bitrate)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(6.dp))
              val bitrates = listOf(128, 192, 256, 320)
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                bitrates.forEach { kbps ->
                  val selected = options.bitrateKbps == kbps
                  FilterChip(
                    selected = selected,
                    onClick = { viewModel.updateBitrate(kbps) },
                    label = { Text("$kbps kbps", fontSize = 12.sp) },
                    leadingIcon = if (selected) {
                      { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                  )
                }
              }
              Spacer(modifier = Modifier.height(12.dp))
            }

            // Nombre personalizado opcional
            OutlinedTextField(
              value = customNameInput,
              onValueChange = {
                customNameInput = it
                viewModel.updateCustomFileName(it)
              },
              label = { Text("Nombre de salida (Opcional)") },
              placeholder = { Text("Ej: MiCancion_8D") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              shape = RoundedCornerShape(12.dp)
            )
          }
        }
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

      // 6. Sección de Audios 8D Creados
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Historial de Audios 8D (${history.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.weight(1f))
          FilledTonalButton(
            onClick = { AppStorageManager.openFolderInFileManager(context, AppAudioFolder.AUDIO_8D) },
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
                text = "Aún no has creado audios 8D",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Selecciona una canción o prueba la muestra para empezar.",
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
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface
            ),
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
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
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
