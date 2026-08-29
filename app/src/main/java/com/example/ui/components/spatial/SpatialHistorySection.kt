package com.example.ui.components.spatial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.model.ConvertedAudioFile

@Composable
fun SpatialHistoryHeader(
  count: Int,
  onOpenFolder: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "Historial de Audios 8D ($count)",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.weight(1f))
    FilledTonalButton(
      onClick = onOpenFolder,
      shape = RoundedCornerShape(8.dp)
    ) {
      Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text("Abrir Carpeta", fontSize = 11.sp)
    }
  }
}

@Composable
fun SpatialHistoryEmptyState(
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    modifier = modifier.fillMaxWidth()
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

@Composable
fun SpatialHistoryItemCard(
  file: ConvertedAudioFile,
  playbackState: PlaybackState,
  onTogglePlay: () -> Unit,
  onSeek: (Long) -> Unit,
  onShare: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isPlaying = playbackState.isPlaying && playbackState.currentFile?.absolutePath == file.file.absolutePath

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onTogglePlay,
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
          onClick = onShare,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(16.dp))
        }

        IconButton(
          onClick = onDelete,
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
          onValueChange = { onSeek(it.toLong()) },
          valueRange = 0f..playbackState.durationMs.toFloat(),
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}
