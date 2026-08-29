package com.example.ui.components.split

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioFormat
import com.example.model.AudioSplitOptions
import com.example.model.SplitSilenceDuration
import com.example.model.SplitSilenceSensitivity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitSettingsCard(
  options: AudioSplitOptions,
  onSensitivityChange: (SplitSilenceSensitivity) -> Unit,
  onMinSilenceDurationChange: (SplitSilenceDuration) -> Unit,
  onTargetFormatChange: (AudioFormat) -> Unit,
  onBaseNameChange: (String) -> Unit,
  onZipToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          Icons.Default.Tune,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Text(
          "Configuración de Detección",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 1. Sensibilidad dB
      Text(
        "Sensibilidad del Silencio:",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SplitSilenceSensitivity.values().forEach { sens ->
          val isSelected = options.sensitivity == sens
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSensitivityChange(sens) },
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  sens.displayName,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 13.sp,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  sens.description,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. Duración de Pausa
      Text(
        "Pausa mínima para dividir pista:",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SplitSilenceDuration.values().forEach { dur ->
          val isSelected = options.minSilenceDuration == dur
          Surface(
            modifier = Modifier
              .weight(1f)
              .clickable { onMinSilenceDurationChange(dur) },
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null
          ) {
            Column(
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                dur.displayName,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 3. Formato de Exportación
      Text(
        "Formato de Pistas Generadas:",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))

      val formats = listOf(
        AudioFormat.MP3,
        AudioFormat.M4A_AAC,
        AudioFormat.WAV,
        AudioFormat.FLAC,
        AudioFormat.OGG_OPUS
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        formats.forEach { fmt ->
          val isSelected = options.targetFormat == fmt
          FilterChip(
            selected = isSelected,
            onClick = { onTargetFormatChange(fmt) },
            label = { Text(fmt.displayName) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 4. Nombre base
      OutlinedTextField(
        value = options.baseFileName,
        onValueChange = onBaseNameChange,
        label = { Text("Nombre Base de las Pistas") },
        placeholder = { Text("Ej: Mi_Album_En_Vivo") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 5. Opcional: Generar ZIP
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onZipToggle(!options.zipAllTracks) },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              Icons.Default.Archive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Empaquetar también en archivo ZIP", fontWeight = FontWeight.Medium, fontSize = 13.sp)
              Text("Facilita compartir todas las pistas a la vez", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Switch(
            checked = options.zipAllTracks,
            onCheckedChange = onZipToggle
          )
        }
      }
    }
  }
}
