package com.example.ui.components.merge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AudioMetadataReader
import com.example.audio.PlaybackState
import com.example.model.ConvertedAudioFile
import com.example.model.MergeProgress
import com.example.model.MergeState

@Composable
fun MergeProgressDialog(
  progress: MergeProgress,
  playbackState: PlaybackState,
  onPlayPause: () -> Unit,
  onSeek: (Long) -> Unit,
  onDismiss: () -> Unit,
  onCancel: () -> Unit,
  onShare: (ConvertedAudioFile) -> Unit,
  onOpenFolder: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isCompleted = progress.state == MergeState.COMPLETED
  val isError = progress.state == MergeState.ERROR
  val isRunning = !isCompleted && !isError

  Dialog(
    onDismissRequest = {
      if (isCompleted || isError) onDismiss()
    },
    properties = DialogProperties(
      dismissOnBackPress = isCompleted || isError,
      dismissOnClickOutside = isCompleted || isError
    )
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
      modifier = modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("merge_progress_dialog")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top Icon Status
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
              when {
                isCompleted -> MaterialTheme.colorScheme.primaryContainer
                isError -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          when {
            isCompleted -> Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp)
            )
            isError -> Icon(
              imageVector = Icons.Default.Error,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(36.dp)
            )
            else -> Icon(
              imageVector = Icons.Default.GraphicEq,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(32.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
          text = when {
            isCompleted -> "¡Unión Completada!"
            isError -> "Error en la Unión"
            else -> "Uniendo Pistas de Audio"
          },
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
          ),
          color = when {
            isError -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
          },
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle / Status Message
        Text(
          text = if (isError) {
            progress.errorMessage ?: "Ocurrió un error inesperado al procesar las pistas."
          } else {
            progress.statusMessage
          },
          style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 13.5.sp,
            lineHeight = 18.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Progress Bar (when running)
        if (isRunning) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            LinearProgressIndicator(
              progress = { progress.progressPercent },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "${(progress.progressPercent * 100).toInt()}% completado",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        // Completed Audio Card & Player
        if (isCompleted && progress.mergedFile != null) {
          val file = progress.mergedFile
          val isFilePlaying = playbackState.isPlaying && playbackState.currentFile?.absolutePath == file.file.absolutePath

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${AudioMetadataReader.formatDuration(file.durationMs)} • ${AudioMetadataReader.formatFileSize(file.sizeBytes)} • ${file.format.badge}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.primary
              )

              Spacer(modifier = Modifier.height(8.dp))

              // Player controls
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                IconButton(
                  onClick = onPlayPause,
                  modifier = Modifier.size(40.dp),
                  colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                  )
                ) {
                  Icon(
                    imageVector = if (isFilePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Reproducir audio unido"
                  )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  val currentPos = if (isFilePlaying) playbackState.currentPositionMs else 0L
                  val dur = if (isFilePlaying && playbackState.durationMs > 0) playbackState.durationMs else file.durationMs.coerceAtLeast(1L)
                  val sliderVal = (currentPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

                  Slider(
                    value = sliderVal,
                    onValueChange = { frac ->
                      onSeek((frac * dur).toLong())
                    },
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = AudioMetadataReader.formatDuration(currentPos),
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = AudioMetadataReader.formatDuration(dur),
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        if (isRunning) {
          OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Cancelar")
          }
        } else if (isCompleted && progress.mergedFile != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { onShare(progress.mergedFile) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Compartir")
            }

            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Aceptar")
            }
          }
        } else {
          Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Cerrar")
          }
        }
      }
    }
  }
}
