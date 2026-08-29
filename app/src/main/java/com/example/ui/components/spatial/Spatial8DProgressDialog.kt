package com.example.ui.components.spatial

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AppAudioFolder
import com.example.audio.AppStorageManager
import com.example.audio.PlaybackState
import com.example.model.ConvertedAudioFile
import com.example.model.Spatial8DOptions
import com.example.model.Spatial8DProgress
import com.example.model.Spatial8DState

@Composable
fun Spatial8DProgressDialog(
  progress: Spatial8DProgress,
  options: Spatial8DOptions,
  playbackState: PlaybackState,
  onCancel: () -> Unit,
  onDismiss: () -> Unit,
  onTogglePlay: (ConvertedAudioFile) -> Unit,
  onSeekTo: (Long) -> Unit,
  onShare: (ConvertedAudioFile) -> Unit
) {
  val context = LocalContext.current

  if (progress.state == Spatial8DState.IDLE) return

  Dialog(
    onDismissRequest = {
      if (progress.state == Spatial8DState.COMPLETED || progress.state == Spatial8DState.ERROR) {
        onDismiss()
      }
    },
    properties = DialogProperties(
      dismissOnBackPress = progress.state == Spatial8DState.COMPLETED || progress.state == Spatial8DState.ERROR,
      dismissOnClickOutside = false
    )
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("spatial_progress_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        when (progress.state) {
          Spatial8DState.ANALYZING,
          Spatial8DState.DECODING_PCM,
          Spatial8DState.APPLYING_8D_DSP,
          Spatial8DState.ENCODING_OUTPUT -> {
            // Processing View
            Icon(
              imageVector = Icons.Default.Headphones,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "Generando Audio 8D Espacial",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = progress.currentPhaseText,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.height(36.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

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
              text = "${(progress.progressPercent * 100).toInt()}%",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
              onClick = onCancel,
              modifier = Modifier.testTag("spatial_cancel_button")
            ) {
              Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Cancelar")
            }
          }

          Spatial8DState.COMPLETED -> {
            val file = progress.outputFile
            // Completed View
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Color(0xFF4CAF50),
              modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "¡Audio 8D Creado con Éxito!",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center
            )

            Text(
              text = "🎧 Usa auriculares para escuchar el efecto 360°",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.primary,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (file != null) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "${file.format.displayName} • ${file.formattedSize} • ${file.formattedDuration}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  // Mini Reproductor
                  Spacer(modifier = Modifier.height(8.dp))
                  val isPlayingThis = playbackState.isPlaying && playbackState.currentFile?.absolutePath == file.file.absolutePath
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    IconButton(
                      onClick = { onTogglePlay(file) },
                      modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                      Icon(
                        imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingThis) "Pausar" else "Reproducir",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                      )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    if (isPlayingThis && playbackState.durationMs > 0) {
                      val currentPos = playbackState.currentPositionMs.toFloat()
                      val maxPos = playbackState.durationMs.toFloat()
                      Slider(
                        value = currentPos.coerceIn(0f, maxPos),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..maxPos,
                        modifier = Modifier.weight(1f)
                      )
                    } else {
                      Text(
                        text = "Toca para probar el efecto 8D",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { onShare(file) },
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Compartir", fontSize = 12.sp)
                }

                FilledTonalButton(
                  onClick = {
                    AppStorageManager.openFolderInFileManager(context, AppAudioFolder.AUDIO_8D)
                  },
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Ver Carpeta", fontSize = 12.sp)
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = onDismiss,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("spatial_dismiss_button"),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
              )
            ) {
              Text("Aceptar")
            }
          }

          Spatial8DState.ERROR -> {
            Icon(
              imageVector = Icons.Default.Error,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "Error en el Procesamiento 8D",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = progress.errorMessage ?: "Ocurrió un fallo desconocido.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
              onClick = onDismiss,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Cerrar")
            }
          }

          Spatial8DState.IDLE -> {}
        }
      }
    }
  }
}
