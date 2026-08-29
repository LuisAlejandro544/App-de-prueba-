package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AudioMetadataReader
import com.example.model.ConversionProgress
import com.example.model.ConversionState
import com.example.model.ConvertedAudioFile

@Composable
fun ConversionProgressDialog(
  progress: ConversionProgress,
  onCancel: () -> Unit,
  onDismiss: () -> Unit,
  onPlayFile: (ConvertedAudioFile) -> Unit,
  onShareFile: (ConvertedAudioFile) -> Unit,
  onExportFile: (ConvertedAudioFile) -> Unit,
  modifier: Modifier = Modifier
) {
  if (progress.state == ConversionState.IDLE) return

  val animatedProgress by animateFloatAsState(
    targetValue = progress.progressPercent,
    label = "conv_prog"
  )

  Dialog(
    onDismissRequest = {
      if (progress.state == ConversionState.COMPLETED || progress.state == ConversionState.ERROR) {
        onDismiss()
      }
    },
    properties = DialogProperties(
      dismissOnBackPress = progress.state == ConversionState.COMPLETED || progress.state == ConversionState.ERROR,
      dismissOnClickOutside = false
    )
  ) {
    Card(
      modifier = modifier
        .fillMaxWidth()
        .padding(8.dp)
        .testTag("conversion_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        when (progress.state) {
          ConversionState.PREPARING, ConversionState.DECODING, ConversionState.ENCODING, ConversionState.SAVING -> {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
              )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
              text = "Convirtiendo Audio",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = progress.statusMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            LinearProgressIndicator(
              progress = { animatedProgress },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .testTag("conversion_linear_progress"),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "${(animatedProgress * 100).toInt()}%",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
              onClick = onCancel,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("cancel_conversion_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Cancelar")
            }
          }

          ConversionState.COMPLETED -> {
            val file = progress.convertedFile
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "¡Conversión Finalizada!",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )

            if (file != null) {
              Spacer(modifier = Modifier.height(12.dp))
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = "Formato: ${file.format.badge}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = "Tamaño: ${AudioMetadataReader.formatFileSize(file.sizeBytes)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(20.dp))

              // Action buttons
              Button(
                onClick = {
                  onPlayFile(file)
                  onDismiss()
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("play_converted_result_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reproducir Audio")
              }

              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { onShareFile(file) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("share_converted_button"),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Compartir", fontSize = 13.sp)
                }

                OutlinedButton(
                  onClick = { onExportFile(file) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("export_converted_button"),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Guardar", fontSize = 13.sp)
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
              onClick = onDismiss,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("close_dialog_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Cerrar")
            }
          }

          ConversionState.ERROR -> {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "No se pudo completar la conversión",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = progress.errorMessage ?: "Verifica que el archivo no esté protegido o dañado.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = onDismiss,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("error_dismiss_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Aceptar")
            }
          }

          ConversionState.IDLE -> {}
        }
      }
    }
  }
}
