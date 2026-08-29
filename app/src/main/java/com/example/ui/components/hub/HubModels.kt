package com.example.ui.components.hub

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Modos de visualización para el menú principal de herramientas.
 */
enum class HubViewMode {
  GRID,         // Cuadrícula de 2 columnas (muy compacta y visual)
  COMPACT_LIST, // Lista horizontal delgada (estilo explorador/ajustes)
  DETAILED      // Tarjetas expandidas con descripciones largas y tags
}

/**
 * Modelo que encapsula los datos de una herramienta en el Hub.
 */
data class HubToolItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val description: String,
  val icon: ImageVector,
  val badge: String,
  val gradientColors: List<Color>,
  val tags: List<String>,
  val onClick: () -> Unit,
  val testTag: String
)
