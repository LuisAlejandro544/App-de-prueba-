package com.example.ui.components.silence

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
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AudioMetadataReader
import com.example.audio.PlaybackState
import com.example.model.AudioFormat
import com.example.model.ConvertedAudioFile
import com.example.model.SilenceProgress
import com.example.model.SilenceState

@Composable
fun SilenceProgressDialog(
  progress: SilenceProgress,
  playbackState: PlaybackState,
  onCancel: () -> Unit,
  onDismiss: () -> Unit,
  onTogglePlay: (ConvertedAudioFile) -> Unit,
  onSeekTo: (Long) -> Unit,
  onShare: (ConvertedAudioFile) -> Unit
) {
  if (progress.state == SilenceState.IDLE) return

  Dialog(
    onDismissRequest = {
      if (progress.state == SilenceState.COMPLETED || progress.state == SilenceState.ERROR) {
        onDismiss()
      }
    },
    properties = DialogProperties(
      dismissOnBackPress = progress.state == SilenceState.COMPLETED || progress.state == SilenceState.ERROR,
      dismissOnClickOutside = false
    )
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        when (progress.state) {
          SilenceState.DECODING,
          SilenceState.ANALYZING_DSP,
          SilenceState.CUTTING_AUDIO,
          SilenceState.ENCODING -> {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.ContentCut,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "Eliminando Silencios",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = progress.statusMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            LinearProgressIndicator(
              progress = { progress.progress },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
              onClick = onCancel,
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Cancelar")
            }
          }

          SilenceState.COMPLETED -> {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "¡Audio Optimizado!",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Métricas de tiempo ahorrado
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "Ahorro de Tiempo: -${progress.savedPercentage}%",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Original: ${AudioMetadataReader.formatDuration(progress.originalDurationMs)}  ➔  Final: ${AudioMetadataReader.formatDuration(progress.finalDurationMs)}",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                  text = "Pausas eliminadas: ${progress.segmentsCut}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini Reproductor
            progress.outputFile?.let { file ->
              val convertedFile = ConvertedAudioFile(
                id = file.absolutePath.hashCode().toString(),
                file = file,
                name = file.name,
                format = AudioFormat.MP3,
                sizeBytes = file.length(),
                durationMs = progress.finalDurationMs,
                timestamp = System.currentTimeMillis(),
                sampleRate = 44100,
                channels = 2,
                bitrateKbps = 192
              )

              val isPlaying = playbackState.isPlaying && playbackState.currentFile?.absolutePath == file.absolutePath

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                IconButton(
                  onClick = { onTogglePlay(convertedFile) },
                  modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                  Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                  )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                  )
                  Text(
                    text = AudioMetadataReader.formatFileSize(file.length()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                IconButton(onClick = { onShare(convertedFile) }) {
                  Icon(Icons.Default.Share, contentDescription = "Compartir")
                }
              }

              if (isPlaying && playbackState.durationMs > 0) {
                Slider(
                  value = playbackState.currentPositionMs.toFloat().coerceIn(0f, playbackState.durationMs.toFloat()),
                  onValueChange = { onSeekTo(it.toLong()) },
                  valueRange = 0f..playbackState.durationMs.toFloat(),
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
              onClick = onDismiss,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp)
            ) {
              Text("Aceptar y Guardar")
            }
          }

          SilenceState.ERROR -> {
            Icon(
              Icons.Default.Error,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "Error al procesar",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = progress.errorMessage ?: "Ocurrió un error inesperado al procesar el audio.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Cerrar")
            }
          }

          SilenceState.IDLE -> {}
        }
      }
    }
  }
}
