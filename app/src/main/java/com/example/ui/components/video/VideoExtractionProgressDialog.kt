package com.example.ui.components.video

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.PlaybackState
import com.example.model.ConvertedAudioFile
import com.example.model.VideoExtractionProgress
import com.example.model.VideoExtractionState
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.VideoToAudioMorphAnimation

@Composable
fun VideoExtractionProgressDialog(
  progress: VideoExtractionProgress,
  playbackState: PlaybackState,
  onPlayPause: () -> Unit,
  onSeek: (Long) -> Unit,
  onDismiss: () -> Unit,
  onCancel: () -> Unit,
  onShare: (ConvertedAudioFile) -> Unit,
  onSaveToMusic: (ConvertedAudioFile) -> Unit
) {
  Dialog(onDismissRequest = {
    if (progress.state != VideoExtractionState.EXTRACTING) {
      onDismiss()
    }
  }) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        when (progress.state) {
          VideoExtractionState.EXTRACTING -> {
            VideoToAudioMorphAnimation(
              size = 80,
              modifier = Modifier.padding(bottom = 6.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Extrayendo Pista de Audio",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = progress.statusMessage,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
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
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
              onClick = onCancel,
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Cancelar Extracción")
            }
          }

          VideoExtractionState.COMPLETED -> {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Color(0xFF4CAF50),
              modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "¡Audio Extraído con Éxito!",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = progress.statusMessage,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            progress.extractedFile?.let { file ->
              Spacer(modifier = Modifier.height(16.dp))
              AudioPlayerCard(
                playbackState = playbackState,
                onPlayPause = onPlayPause,
                onSeek = onSeek
              )
              Spacer(modifier = Modifier.height(14.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { onShare(file) },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Compartir", fontSize = 12.sp)
                }

                Button(
                  onClick = { onSaveToMusic(file) },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Guardar", fontSize = 12.sp)
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onDismiss,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Aceptar")
            }
          }

          VideoExtractionState.ERROR -> {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Error en la Extracción",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = progress.errorMessage ?: "Ocurrió un error al procesar el archivo de video.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onDismiss,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Cerrar")
            }
          }

          else -> {}
        }
      }
    }
  }
}
