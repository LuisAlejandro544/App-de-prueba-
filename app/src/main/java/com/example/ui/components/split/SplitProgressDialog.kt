package com.example.ui.components.split

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AppAudioFolder
import com.example.audio.AppStorageManager
import com.example.audio.PlaybackState
import com.example.model.AudioSplitProgress
import com.example.model.SplitProcessingState
import java.io.File

@Composable
fun SplitProgressDialog(
  progress: AudioSplitProgress,
  playbackState: PlaybackState,
  onDismiss: () -> Unit,
  onCancel: () -> Unit,
  onTogglePlay: (File) -> Unit,
  onShareFile: (File, String) -> Unit
) {
  if (progress.state == SplitProcessingState.IDLE) return

  val context = LocalContext.current

  Dialog(onDismissRequest = {
    if (progress.state == SplitProcessingState.COMPLETED || progress.state == SplitProcessingState.ERROR) {
      onDismiss()
    }
  }) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        when (progress.state) {
          SplitProcessingState.ANALYZING,
          SplitProcessingState.SPLITTING_EXPORT -> {
            CircularProgressIndicator(
              modifier = Modifier.size(56.dp),
              color = MaterialTheme.colorScheme.primary,
              strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              if (progress.state == SplitProcessingState.ANALYZING) "Analizando Pistas..." else "Exportando Pistas...",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              progress.statusMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
              progress = { progress.progress },
              modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(onClick = onCancel) {
              Text("Cancelar")
            }
          }

          SplitProcessingState.COMPLETED -> {
            Box(
              modifier = Modifier
                .size(60.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              "¡Pistas Creadas con Éxito!",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "Se exportaron ${progress.totalTracksCount} archivos en la carpeta Dividir.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de pistas generadas para reproducir / compartir
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              items(progress.exportedTracks) { trackFile ->
                val isPlaying = playbackState.isPlaying && playbackState.currentFile?.absolutePath == trackFile.absolutePath
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    IconButton(
                      onClick = { onTogglePlay(trackFile) },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      trackFile.name,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f)
                    )
                    IconButton(
                      onClick = { onShareFile(trackFile, "audio/*") },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        Icons.Default.Share,
                        contentDescription = "Compartir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
            }

            if (progress.zipFile != null) {
              Spacer(modifier = Modifier.height(10.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Archivo ZIP completo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(progress.zipFile.name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  IconButton(
                    onClick = { onShareFile(progress.zipFile, "application/zip") }
                  ) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir ZIP", tint = MaterialTheme.colorScheme.secondary)
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  AppStorageManager.openFolderInFileManager(context, AppAudioFolder.DIVIDIR)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ver Carpeta", fontSize = 12.sp)
              }
              Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("Listo", fontSize = 12.sp)
              }
            }
          }

          SplitProcessingState.ERROR -> {
            Icon(
              Icons.Default.Error,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Error en el Proceso", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              progress.errorMessage ?: "Ocurrió un error inesperado.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onDismiss) {
              Text("Cerrar")
            }
          }
          else -> {}
        }
      }
    }
  }
}
