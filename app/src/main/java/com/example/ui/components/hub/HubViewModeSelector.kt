package com.example.ui.components.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Selector de vista con botones de alternancia estilo segmented control (Cuadrícula, Lista Compacta, Detallada).
 */
@Composable
fun HubViewModeSelector(
  selectedMode: HubViewMode,
  onModeSelected: (HubViewMode) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(3.dp),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      ViewModeIconButton(
        icon = Icons.Default.GridView,
        contentDescription = "Vista Cuadrícula",
        isSelected = selectedMode == HubViewMode.GRID,
        onClick = { onModeSelected(HubViewMode.GRID) },
        testTag = "view_mode_grid"
      )

      ViewModeIconButton(
        icon = Icons.Default.ViewList,
        contentDescription = "Vista Lista Compacta",
        isSelected = selectedMode == HubViewMode.COMPACT_LIST,
        onClick = { onModeSelected(HubViewMode.COMPACT_LIST) },
        testTag = "view_mode_list"
      )

      ViewModeIconButton(
        icon = Icons.Default.ViewStream,
        contentDescription = "Vista Detallada",
        isSelected = selectedMode == HubViewMode.DETAILED,
        onClick = { onModeSelected(HubViewMode.DETAILED) },
        testTag = "view_mode_detailed"
      )
    }
  }
}

@Composable
private fun ViewModeIconButton(
  icon: ImageVector,
  contentDescription: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  Box(
    modifier = Modifier
      .size(34.dp)
      .clip(RoundedCornerShape(9.dp))
      .background(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
      )
      .testTag(testTag)
      .clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp)
    )
  }
}
