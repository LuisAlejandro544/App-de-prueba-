package com.example.ui.components.spatial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioFormat
import com.example.model.Spatial8DOptions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpatialExportSettingsCard(
  options: Spatial8DOptions,
  customFileName: String,
  onFormatSelected: (AudioFormat) -> Unit,
  onBitrateSelected: (Int) -> Unit,
  onFileNameChanged: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "Formato de Exportación",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(8.dp))

      val formats = listOf(
        AudioFormat.MP3,
        AudioFormat.M4A_AAC,
        AudioFormat.WAV,
        AudioFormat.FLAC,
        AudioFormat.OGG_OPUS,
        AudioFormat.OPUS
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        formats.forEach { fmt ->
          val selected = options.targetFormat == fmt
          FilterChip(
            selected = selected,
            onClick = { onFormatSelected(fmt) },
            label = { Text(fmt.displayName, fontSize = 12.sp) },
            leadingIcon = if (selected) {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
            } else null
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Bitrates si aplica
      if (options.targetFormat.supportsBitrateCustomization) {
        Text(
          text = "Calidad de Audio (Bitrate)",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        val bitrates = listOf(128, 192, 256, 320)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          bitrates.forEach { kbps ->
            val selected = options.bitrateKbps == kbps
            FilterChip(
              selected = selected,
              onClick = { onBitrateSelected(kbps) },
              label = { Text("$kbps kbps", fontSize = 12.sp) },
              leadingIcon = if (selected) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
              } else null
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Nombre personalizado opcional
      OutlinedTextField(
        value = customFileName,
        onValueChange = onFileNameChanged,
        label = { Text("Nombre de salida (Opcional)") },
        placeholder = { Text("Ej: MiCancion_8D") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )
    }
  }
}
