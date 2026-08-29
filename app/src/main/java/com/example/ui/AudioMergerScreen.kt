package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.MergeState
import com.example.ui.components.merge.MergeOptionsSection
import com.example.ui.components.merge.MergeProgressDialog
import com.example.ui.components.merge.MergeSummaryCard
import com.example.ui.components.merge.MergeTrackCard
import com.example.ui.components.video.ExtractedFileCard
import com.example.viewmodel.AudioMergerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMergerScreen(
  onNavigateBack: () -> Unit,
  viewModel: AudioMergerViewModel = viewModel()
) {
  val context = LocalContext.current
  val tracks by viewModel.selectedTracks.collectAsState()
  val options by viewModel.options.collectAsState()
  val progress by viewModel.progress.collectAsState()
  val history by viewModel.mergedHistory.collectAsState()
  val playbackState by viewModel.playbackState.collectAsState()
  val isAddingTracks by viewModel.isAddingTracks.collectAsState()

  // Selector múltiple de audios
  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetMultipleContents()
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      viewModel.addTracks(context, uris)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Unir Audios",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
              )
            )
            Text(
              text = "Combina hasta 6 archivos en uno solo",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("btn_back_merger_screen")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Volver al Hub"
            )
          }
        },
        actions = {
          if (tracks.isNotEmpty()) {
            IconButton(
              onClick = { viewModel.clearAllTracks() },
              modifier = Modifier.testTag("btn_clear_all_tracks")
            ) {
              Icon(
                imageVector = Icons.Default.ClearAll,
                contentDescription = "Limpiar pistas seleccionadas",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      if (tracks.size >= 2) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
          tonalElevation = 8.dp,
          shadowElevation = 12.dp,
          color = MaterialTheme.colorScheme.surface
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 14.dp)
          ) {
            Button(
              onClick = { viewModel.startMerge(context) },
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_merge_button"),
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
              )
            ) {
              Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Unir ${tracks.size} Pistas en ${options.targetFormat.badge}",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp
                )
              )
            }
          }
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Selector de Pistas o Tarjeta de Pistas Seleccionadas
      if (tracks.isEmpty()) {
        item {
          EmptyTracksPlaceholder(
            isAdding = isAddingTracks,
            onPickAudios = { audioPickerLauncher.launch("audio/*") }
          )
        }
      } else {
        // Resumen de unión y normalización
        item {
          MergeSummaryCard(tracks = tracks)
        }

        // Título de la lista de pistas
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Orden de Unión (${tracks.size}/6)",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )

            if (tracks.size < AudioMergerViewModel.MAX_TRACKS) {
              OutlinedButton(
                onClick = { audioPickerLauncher.launch("audio/*") },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_add_more_tracks")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
              }
            }
          }
        }

        // Lista interactiva de pistas reordenables
        itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
          MergeTrackCard(
            track = track,
            index = index,
            totalTracks = tracks.size,
            playbackState = playbackState,
            onPlayPause = { viewModel.togglePlayTrack(context, track) },
            onMoveUp = { viewModel.moveTrackUp(index) },
            onMoveDown = { viewModel.moveTrackDown(index) },
            onRemove = { viewModel.removeTrack(index) }
          )
        }

        if (tracks.size == 1) {
          item {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Audiotrack,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Añade al menos 1 pista más para habilitar la unión de audio.",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onTertiaryContainer
                )
              }
            }
          }
        }

        // Opciones de configuración (Formato, Bitrate, Micro-fundido, Nombre)
        item {
          Spacer(modifier = Modifier.height(6.dp))
          MergeOptionsSection(
            options = options,
            onFormatSelected = { viewModel.setTargetFormat(it) },
            onBitrateSelected = { viewModel.setBitrate(it) },
            onCrossfadeChanged = { viewModel.setEnableCrossfade(it) },
            onFileNameChanged = { viewModel.setCustomFileName(it) }
          )
        }
      }

      // Historial de audios unidos
      if (history.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(10.dp))

          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Folder,
                  contentDescription = "Carpeta de almacenamiento",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Column {
                  Text(
                    text = "Carpeta de destino",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = "Música / AudioConverter / Fusionar",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              IconButton(
                onClick = { viewModel.openOutputFolder(context) },
                modifier = Modifier.size(36.dp).testTag("btn_open_merge_folder")
              ) {
                Icon(
                  imageVector = Icons.Default.FolderOpen,
                  contentDescription = "Abrir carpeta en el explorador",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Audios Unidos Recientemente (${history.size})",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp
            )
          )
        }

        itemsIndexed(history, key = { _, item -> item.id }) { _, item ->
          ExtractedFileCard(
            file = item,
            playbackState = playbackState,
            onPlayPause = { viewModel.togglePlayFile(context, item) },
            onSeek = { viewModel.seekTo(it) },
            onShare = { viewModel.shareMergedFile(context, item) },
            onSaveToMusic = { /* Ya guardado en Fusionar */ },
            onDelete = { viewModel.deleteMergedFile(item) }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(40.dp))
      }
    }
  }

  // Diálogo de progreso en tiempo real
  if (progress.state != MergeState.IDLE) {
    MergeProgressDialog(
      progress = progress,
      playbackState = playbackState,
      onPlayPause = { progress.mergedFile?.let { viewModel.togglePlayFile(context, it) } },
      onSeek = { viewModel.seekTo(it) },
      onDismiss = { viewModel.dismissProgressDialog() },
      onCancel = { viewModel.cancelMerge() },
      onShare = { file -> viewModel.shareMergedFile(context, file) },
      onOpenFolder = { viewModel.openOutputFolder(context) }
    )
  }
}

@Composable
private fun EmptyTracksPlaceholder(
  isAdding: Boolean,
  onPickAudios: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = !isAdding) { onPickAudios() }
      .testTag("placeholder_pick_merge_audios")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        if (isAdding) {
          CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = MaterialTheme.colorScheme.primary
          )
        } else {
          Icon(
            imageVector = Icons.Default.Layers,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Text(
        text = "Seleccionar Audios para Unir",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Toca aquí para elegir entre 2 y 6 pistas de audio (MP3, WAV, M4A, FLAC, OGG, etc.). Podrás reordenarlas y unirlas en un solo archivo con calidad homogénea.",
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 12.5.sp,
          lineHeight = 17.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(18.dp))

      Button(
        onClick = onPickAudios,
        enabled = !isAdding,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        )
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isAdding) "Analizando pistas..." else "Explorar Archivos de Audio",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}
