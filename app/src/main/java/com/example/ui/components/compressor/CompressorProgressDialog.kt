package com.example.ui.components.compressor

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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.PlaybackState
import com.example.model.CompressionProgress
import com.example.model.CompressionState
import com.example.model.ConvertedAudioFile
import java.util.Locale

@Composable
fun CompressorProgressDialog(
  progress: CompressionProgress,
  playbackState: PlaybackState,
  onCancel: () -> Unit,
  onDismiss: () -> Unit,
  onTogglePlay: (ConvertedAudioFile) -> Unit,
  onSeekTo: (Long) -> Unit,
  onShare: (ConvertedAudioFile) -> Unit,
  modifier: Modifier = Modifier
) {
  if (progress.state == CompressionState.IDLE) return

  Dialog(
    onDismissRequest = {
      if (progress.state == CompressionState.COMPLETED || progress.state == CompressionState.ERROR) {
        onDismiss()
      }
    },
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
      modifier = modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("compress_progress_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        when (progress.state) {
          CompressionState.ANALYZING,
          CompressionState.DECODING,
          CompressionState.ENCODING -> {
            CircularProgressIndicator(
              progress = { progress.progressPercent },
              modifier = Modifier.size(64.dp),
              strokeWidth = 5.dp,
              color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
              text = "Comprimiendo Audio...",
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

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
              progress = { progress.progressPercent },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(
              onClick = onCancel,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(0.6f)
            ) {
              Text("Cancelar")
            }
          }

          CompressionState.COMPLETED -> {
            Surface(
              shape = CircleShape,
              color = Color(0xFF4CAF50).copy(alpha = 0.15f),
              modifier = Modifier.size(64.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Color(0xFF2E7D32),
                  modifier = Modifier.size(36.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "¡Audio Comprimido!",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Resumen de ahorro
            val origMb = progress.originalSizeBytes / (1024f * 1024f)
            val finalMb = progress.finalSizeBytes / (1024f * 1024f)

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                  Text(
                    text = "Ahorraste un ${progress.savedPercentage}% de espacio",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF2E7D32)
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = String.format(Locale.getDefault(), "De %.1f MB a solo %.1f MB", origMb, finalMb),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reproductor / Archivo generado
            progress.outputFile?.let { file ->
              val dummyItem = ConvertedAudioFile(
                id = file.absolutePath,
                file = file,
                name = file.name,
                format = com.example.model.AudioFormat.M4A_AAC,
                sizeBytes = file.length(),
                durationMs = 0L,
                timestamp = file.lastModified(),
                sampleRate = 44100,
                channels = 2,
                bitrateKbps = 96
              )
              val isPlaying = playbackState.isPlaying && playbackState.currentFile?.absolutePath == file.absolutePath

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    IconButton(
                      onClick = { onTogglePlay(dummyItem) },
                      modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                      Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                      )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                      text = file.name,
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                      maxLines = 1,
                      modifier = Modifier.weight(1f)
                    )

                    IconButton(
                      onClick = { onShare(dummyItem) },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(16.dp))
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
              }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Aceptar", fontWeight = FontWeight.Bold)
            }
          }

          CompressionState.ERROR -> {
            Icon(
              imageVector = Icons.Default.Error,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "Error al Comprimir",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = progress.errorMessage ?: "Ocurrió un error inesperado al procesar el archivo.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
              onClick = onDismiss,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Cerrar")
            }
          }

          CompressionState.IDLE -> {}
        }
      }
    }
  }
}
