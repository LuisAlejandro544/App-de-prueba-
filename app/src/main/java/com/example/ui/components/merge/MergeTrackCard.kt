package com.example.ui.components.merge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioMetadataReader
import com.example.audio.PlaybackState
import com.example.model.MergeTrackItem

@Composable
fun MergeTrackCard(
  track: MergeTrackItem,
  index: Int,
  totalTracks: Int,
  playbackState: PlaybackState,
  onPlayPause: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isCurrentTrackPlaying = playbackState.isPlaying && playbackState.currentUri == track.uri

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("merge_track_card_$index"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(
      width = 1.dp,
      color = if (isCurrentTrackPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Track number badge & Title
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${index + 1}",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = track.name,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
              ) {
                Text(
                  text = track.formatExtension,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }

              Text(
                text = "${AudioMetadataReader.formatDuration(track.durationMs)} • ${AudioMetadataReader.formatFileSize(track.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Action Buttons (Play preview, Move Up, Move Down, Delete)
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Play preview button
          IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(34.dp).testTag("btn_play_track_$index"),
            colors = IconButtonDefaults.iconButtonColors(
              containerColor = if (isCurrentTrackPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              contentColor = if (isCurrentTrackPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
          ) {
            Icon(
              imageVector = if (isCurrentTrackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = "Vista previa",
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(4.dp))

          // Move Up
          IconButton(
            onClick = onMoveUp,
            enabled = index > 0,
            modifier = Modifier.size(32.dp).testTag("btn_move_up_$index")
          ) {
            Icon(
              imageVector = Icons.Default.ArrowUpward,
              contentDescription = "Subir pista",
              tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
              modifier = Modifier.size(16.dp)
            )
          }

          // Move Down
          IconButton(
            onClick = onMoveDown,
            enabled = index < totalTracks - 1,
            modifier = Modifier.size(32.dp).testTag("btn_move_down_$index")
          ) {
            Icon(
              imageVector = Icons.Default.ArrowDownward,
              contentDescription = "Bajar pista",
              tint = if (index < totalTracks - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
              modifier = Modifier.size(16.dp)
            )
          }

          // Delete
          IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp).testTag("btn_remove_track_$index")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Eliminar pista",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Acoustic metadata info (sample rate, channels, bitrate)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val channelText = if (track.channelCount == 1) "Mono" else if (track.channelCount == 2) "Estéreo" else "${track.channelCount} Ch"
        Text(
          text = "🎯 ${track.sampleRate} Hz • $channelText • ${track.bitrateKbps} kbps",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
      }
    }
  }
}
