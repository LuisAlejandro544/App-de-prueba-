package com.example.ui.components.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioFormat

@Composable
fun VideoTargetFormatSection(
  selectedFormat: AudioFormat,
  onFormatSelected: (AudioFormat) -> Unit
) {
  val formats = listOf(
    AudioFormat.MP3,
    AudioFormat.M4A_AAC,
    AudioFormat.WAV,
    AudioFormat.FLAC,
    AudioFormat.OGG_OPUS,
    AudioFormat.OPUS
  )

  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "Formato de Audio de Salida",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      formats.take(3).forEach { format ->
        FormatChipItem(
          format = format,
          isSelected = format == selectedFormat,
          modifier = Modifier.weight(1f),
          onSelect = { onFormatSelected(format) }
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      formats.drop(3).forEach { format ->
        FormatChipItem(
          format = format,
          isSelected = format == selectedFormat,
          modifier = Modifier.weight(1f),
          onSelect = { onFormatSelected(format) }
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = selectedFormat.description,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
      color = MaterialTheme.colorScheme.primary
    )
  }
}

@Composable
fun FormatChipItem(
  format: AudioFormat,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onSelect: () -> Unit
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onSelect),
    shape = RoundedCornerShape(12.dp),
    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
  ) {
    Box(
      modifier = Modifier.padding(vertical = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = format.badge,
        style = MaterialTheme.typography.labelLarge.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        ),
        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
      )
    }
  }
}
