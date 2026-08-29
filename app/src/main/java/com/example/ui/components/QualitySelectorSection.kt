package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioChannelMode
import com.example.model.AudioFileInfo
import com.example.model.ConversionOptions
import com.example.model.QualityPreset
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QualitySelectorSection(
  options: ConversionOptions,
  selectedAudio: AudioFileInfo? = null,
  onPresetSelected: (QualityPreset) -> Unit,
  onBitrateSelected: (Int) -> Unit,
  onSampleRateSelected: (Int) -> Unit,
  onChannelModeSelected: (AudioChannelMode) -> Unit,
  onVolumeChanged: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  var isAdvancedExpanded by remember { mutableStateOf(false) }

  val maxAllowedBitrate = selectedAudio?.bitrateKbps ?: 320
  val maxAllowedSampleRate = selectedAudio?.sampleRate ?: 48000

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Calidad y Ajustes de Audio",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // Auto-detection and fidelity protection info card
      if (selectedAudio != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Protección de Fidelidad Activa",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Origen detectado: ${selectedAudio.bitrateKbps} kbps • ${selectedAudio.sampleRate / 1000.0} kHz. Los valores superiores se bloquean automáticamente para evitar sobremuestreo artificial.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Quality Presets
      Text(
        text = "Perfil de Calidad",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          QualityPreset.ORIGINAL,
          QualityPreset.HIGH,
          QualityPreset.STANDARD,
          QualityPreset.ECONOMY,
          QualityPreset.VOICE
        ).forEach { preset ->
          val isExceeded = if (selectedAudio != null && preset != QualityPreset.ORIGINAL && preset != QualityPreset.CUSTOM) {
            preset.bitrateKbps > maxAllowedBitrate
          } else {
            false
          }
          val isSelected = options.preset == preset && !isExceeded

          FilterChip(
            selected = isSelected,
            enabled = !isExceeded,
            onClick = {
              if (!isExceeded) {
                onPresetSelected(preset)
              }
            },
            label = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (isExceeded) {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado",
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                  text = if (isExceeded) "${preset.displayName} (Excede)" else preset.displayName,
                  fontSize = 13.sp
                )
              }
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
              disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.testTag("preset_chip_${preset.name.lowercase()}")
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Advanced Settings Toggle
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable { isAdvancedExpanded = !isAdvancedExpanded }
          .testTag("advanced_settings_toggle")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = if (isAdvancedExpanded) "Ocultar Ajustes Avanzados" else "Mostrar Ajustes Avanzados (Bitrate, Canales, Volumen)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary
          )
          Icon(
            imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // Advanced Settings Panel
      AnimatedVisibility(
        visible = isAdvancedExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
        ) {
          // Bitrate
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Tasa de Bits (Bitrate): ${options.bitrateKbps} kbps",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedAudio != null) {
              Text(
                text = "Máx: ${selectedAudio.bitrateKbps} kbps",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(64, 128, 192, 256, 320).forEach { kbps ->
              val isExceeded = selectedAudio != null && kbps > maxAllowedBitrate
              val isSelected = options.bitrateKbps == kbps && !isExceeded

              FilterChip(
                selected = isSelected,
                enabled = !isExceeded,
                onClick = {
                  if (!isExceeded) {
                    onBitrateSelected(kbps)
                  }
                },
                label = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExceeded) {
                      Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        modifier = Modifier.size(11.dp)
                      )
                      Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                      text = if (isExceeded) "$kbps kbps 🔒" else "$kbps kbps",
                      fontSize = 12.sp
                    )
                  }
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                  disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.testTag("bitrate_chip_$kbps")
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Sample Rate
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Frecuencia de Muestreo: ${options.sampleRateHz / 1000.0} kHz",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedAudio != null) {
              Text(
                text = "Máx: ${selectedAudio.sampleRate / 1000.0} kHz",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(48000 to "48 kHz", 44100 to "44.1 kHz", 32000 to "32 kHz", 22050 to "22 kHz").forEach { (rate, label) ->
              val isExceeded = selectedAudio != null && rate > maxAllowedSampleRate
              val isSelected = options.sampleRateHz == rate && !isExceeded

              FilterChip(
                selected = isSelected,
                enabled = !isExceeded,
                onClick = {
                  if (!isExceeded) {
                    onSampleRateSelected(rate)
                  }
                },
                label = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExceeded) {
                      Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        modifier = Modifier.size(11.dp)
                      )
                      Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                      text = if (isExceeded) "$label 🔒" else label,
                      fontSize = 12.sp
                    )
                  }
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                  disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.testTag("samplerate_chip_$rate")
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Channel Mode
          Text(
            text = "Canales de Audio",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            AudioChannelMode.values().forEach { mode ->
              val isSelected = options.channelMode == mode
              FilterChip(
                selected = isSelected,
                onClick = { onChannelModeSelected(mode) },
                label = { Text(mode.displayName, fontSize = 12.sp) },
                modifier = Modifier.testTag("channel_chip_${mode.name.lowercase()}")
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Volume Booster
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Ganancia de Volumen",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = "${(options.volumeMultiplier * 100).roundToInt()}%",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
          }
          Slider(
            value = options.volumeMultiplier,
            onValueChange = onVolumeChanged,
            valueRange = 0.5f..2.0f,
            steps = 14,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("volume_multiplier_slider"),
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary
            )
          )
        }
      }
    }
  }
}
