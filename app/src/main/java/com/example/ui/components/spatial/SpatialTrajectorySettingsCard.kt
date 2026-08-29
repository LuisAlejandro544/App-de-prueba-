package com.example.ui.components.spatial

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Spatial8DOptions
import com.example.model.SpatialReverbPreset
import com.example.model.SpatialRotationSpeed
import com.example.model.SpatialTrajectory

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpatialTrajectorySettingsCard(
  options: Spatial8DOptions,
  onTrajectorySelected: (SpatialTrajectory) -> Unit,
  onSpeedSelected: (SpatialRotationSpeed) -> Unit,
  onDepthChanged: (Float) -> Unit,
  onReverbSelected: (SpatialReverbPreset) -> Unit,
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
        text = "Patrón de Movimiento Espacial",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = options.trajectory.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(10.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        SpatialTrajectory.values().forEach { traj ->
          val selected = options.trajectory == traj
          FilterChip(
            selected = selected,
            onClick = { onTrajectorySelected(traj) },
            label = { Text(traj.displayName, fontSize = 12.sp) },
            leadingIcon = if (selected) {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
            } else null
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Velocidad de Rotación
      Text(
        text = "Velocidad de Rotación",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        SpatialRotationSpeed.values().forEach { spd ->
          val selected = options.rotationSpeed == spd
          FilterChip(
            selected = selected,
            onClick = { onSpeedSelected(spd) },
            label = { Text(spd.displayName, fontSize = 12.sp) },
            leadingIcon = if (selected) {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
            } else null
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Slider de Profundidad Binaural
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Profundidad Binaural 3D",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = "${(options.spatialDepth * 100).toInt()}%",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary
        )
      }

      Slider(
        value = options.spatialDepth,
        onValueChange = onDepthChanged,
        valueRange = 0.2f..1.0f,
        modifier = Modifier.fillMaxWidth()
      )

      // Reverb Acústica de Sala
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Ambiente Acústico (Reverb)",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = options.reverbPreset.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        SpatialReverbPreset.values().forEach { rev ->
          val selected = options.reverbPreset == rev
          FilterChip(
            selected = selected,
            onClick = { onReverbSelected(rev) },
            label = { Text(rev.displayName, fontSize = 12.sp) },
            leadingIcon = if (selected) {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
            } else null
          )
        }
      }
    }
  }
}
