package com.example.ui.components.split

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DetectedTrackSegment

@Composable
fun DetectedTracksPreviewCard(
  segments: List<DetectedTrackSegment>,
  onToggleTrack: (Int) -> Unit,
  onUpdateTrackTitle: (Int, String) -> Unit,
  onPlayPreview: (Long, Long) -> Unit,
  onSelectAll: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val selectedCount = segments.count { it.isSelected }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            Icons.Default.LibraryMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Text(
            "Pistas Detectadas (${segments.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }

        Row {
          TextButton(onClick = { onSelectAll(selectedCount != segments.size) }) {
            Text(if (selectedCount == segments.size) "Deseleccionar" else "Todas", fontSize = 12.sp)
          }
        }
      }

      Text(
        "Se exportarán $selectedCount de ${segments.size} pistas. Puedes preescuchar o cambiar títulos individuales:",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEach { segment ->
          TrackSegmentItemRow(
            segment = segment,
            onToggle = { onToggleTrack(segment.trackIndex) },
            onUpdateTitle = { title -> onUpdateTrackTitle(segment.trackIndex, title) },
            onPlay = { onPlayPreview(segment.startMs, segment.durationMs) }
          )
        }
      }
    }
  }
}

@Composable
private fun TrackSegmentItemRow(
  segment: DetectedTrackSegment,
  onToggle: () -> Unit,
  onUpdateTitle: (String) -> Unit,
  onPlay: () -> Unit
) {
  var isEditing by remember { mutableStateOf(false) }
  var editedTitle by remember(segment.customTitle) { mutableStateOf(segment.customTitle) }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (segment.isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
    tonalElevation = if (segment.isSelected) 2.dp else 0.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Checkbox(
          checked = segment.isSelected,
          onCheckedChange = { onToggle() }
        )

        Spacer(modifier = Modifier.width(4.dp))

        Column(modifier = Modifier.weight(1f)) {
          if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              OutlinedTextField(
                value = editedTitle,
                onValueChange = { editedTitle = it },
                label = { Text("Título pista ${segment.trackIndex}") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              IconButton(onClick = {
                onUpdateTitle(editedTitle)
                isEditing = false
              }) {
                Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
              }
            }
          } else {
            val displayName = if (segment.customTitle.isNotBlank()) segment.customTitle else "Pista ${String.format("%02d", segment.trackIndex)}"
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (segment.isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
              )
              Spacer(modifier = Modifier.width(4.dp))
              IconButton(
                onClick = { isEditing = true },
                modifier = Modifier.size(20.dp)
              ) {
                Icon(
                  Icons.Default.Edit,
                  contentDescription = "Editar nombre",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
            Text(
              "Inicio: ${formatTime(segment.startMs)} | Fin: ${formatTime(segment.endMs)} (Duración: ${formatTime(segment.durationMs)})",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        IconButton(
          onClick = onPlay,
          modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
          Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Preescucha",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

private fun formatTime(ms: Long): String {
  val totalSec = ms / 1000
  val minutes = totalSec / 60
  val seconds = totalSec % 60
  return String.format("%02d:%02d", minutes, seconds)
}
