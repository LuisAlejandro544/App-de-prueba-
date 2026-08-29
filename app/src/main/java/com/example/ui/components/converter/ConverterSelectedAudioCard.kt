package com.example.ui.components.converter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioMetadataReader
import com.example.audio.PlaybackState
import com.example.model.AudioFileInfo
import com.example.ui.components.AudioPlayerCard

@Composable
fun ConverterSelectedAudioCard(
  selectedAudio: AudioFileInfo,
  playbackState: PlaybackState,
  onClearFile: () -> Unit,
  onPlayInput: (AudioFileInfo) -> Unit,
  onSeekAudio: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
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
          PlaybackState(
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
