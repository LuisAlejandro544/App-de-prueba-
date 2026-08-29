package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioMetadataReader
import com.example.audio.PlaybackState
import com.example.model.AudioFormat
import com.example.model.ConvertedAudioFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConvertedFilesList(
  files: List<ConvertedAudioFile>,
  playbackState: PlaybackState,
  onPlayFile: (ConvertedAudioFile) -> Unit,
  onShareFile: (ConvertedAudioFile) -> Unit,
  onExportFile: (ConvertedAudioFile) -> Unit,
  onDeleteFile: (ConvertedAudioFile) -> Unit,
  onGoToConverter: () -> Unit,
  onOpenFolder: (() -> Unit)? = null,
  folderDisplayPath: String = "Música / AudioConverter / Convertir",
  modifier: Modifier = Modifier
) {
  if (files.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Sin archivos convertidos aún",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Selecciona un archivo de audio y conviértelo a MP3, WAV, M4A, FLAC u OGG para verlo aquí.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onGoToConverter() }
            .testTag("empty_state_convert_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Audiotrack,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Convertir mi primer audio",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }
    }
    return
  }

  val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))
      
      // Tarjeta informativa de la carpeta del almacenamiento accesible
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
                text = folderDisplayPath,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          if (onOpenFolder != null) {
            IconButton(
              onClick = onOpenFolder,
              modifier = Modifier.size(36.dp).testTag("btn_open_folder")
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
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "${files.size} ${if (files.size == 1) "archivo convertido" else "archivos convertidos"}",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
      )
    }

    items(files, key = { it.id }) { fileItem ->
      val isThisPlaying = playbackState.isPlaying && playbackState.currentFile?.absolutePath == fileItem.file.absolutePath
      val isThisActive = playbackState.currentFile?.absolutePath == fileItem.file.absolutePath

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("converted_item_${fileItem.file.name}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isThisActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
          1.dp,
          if (isThisActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Format Badge
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = when (fileItem.format) {
                AudioFormat.MP3, AudioFormat.M4A_AAC, AudioFormat.M4R -> MaterialTheme.colorScheme.primaryContainer
                AudioFormat.WAV, AudioFormat.AIFF, AudioFormat.FLAC -> MaterialTheme.colorScheme.secondaryContainer
                AudioFormat.OGG_OPUS, AudioFormat.OPUS, AudioFormat.AMR -> MaterialTheme.colorScheme.tertiaryContainer
                AudioFormat.AC3, AudioFormat.MP2, AudioFormat.WMA -> MaterialTheme.colorScheme.surfaceVariant
              }
            ) {
              Text(
                text = fileItem.format.badge,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = when (fileItem.format) {
                  AudioFormat.MP3, AudioFormat.M4A_AAC, AudioFormat.M4R -> MaterialTheme.colorScheme.onPrimaryContainer
                  AudioFormat.WAV, AudioFormat.AIFF, AudioFormat.FLAC -> MaterialTheme.colorScheme.onSecondaryContainer
                  AudioFormat.OGG_OPUS, AudioFormat.OPUS, AudioFormat.AMR -> MaterialTheme.colorScheme.onTertiaryContainer
                  AudioFormat.AC3, AudioFormat.MP2, AudioFormat.WMA -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = fileItem.name,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 14.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = AudioMetadataReader.formatFileSize(fileItem.sizeBytes),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "•",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = AudioMetadataReader.formatDuration(fileItem.durationMs),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "•",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = dateFormat.format(Date(fileItem.timestamp)),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Play / Pause Icon Button
            FilledIconButton(
              onClick = { onPlayFile(fileItem) },
              modifier = Modifier
                .size(40.dp)
                .testTag("play_converted_item_${fileItem.file.name}"),
              colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isThisPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Icon(
                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isThisPlaying) "Pausar" else "Reproducir",
                modifier = Modifier.size(22.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Bottom action icons: Share, Save/Export, Delete
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = { onShareFile(fileItem) },
              modifier = Modifier
                .size(36.dp)
                .testTag("share_item_${fileItem.file.name}")
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Compartir audio",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = { onExportFile(fileItem) },
              modifier = Modifier
                .size(36.dp)
                .testTag("export_item_${fileItem.file.name}")
            ) {
              Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Exportar a Música",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = { onDeleteFile(fileItem) },
              modifier = Modifier
                .size(36.dp)
                .testTag("delete_item_${fileItem.file.name}")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Eliminar archivo",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
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
