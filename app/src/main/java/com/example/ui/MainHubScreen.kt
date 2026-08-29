package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AppAudioFolder
import com.example.audio.AppStorageManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainHubScreen(
  onNavigateToConverter: () -> Unit,
  onNavigateToVideoExtractor: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .statusBarsPadding()
        .navigationBarsPadding(),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // Header Section
      item {
        HubHeader()
      }

      // Hero Banner
      item {
        HubHeroBanner()
      }

      // Section Title: Herramientas
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Herramientas de Audio",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
          ) {
            Text(
              text = "2 Disponibles",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      // Primary Tool Card 1: CONVERTIR
      item {
        ActiveToolCard(
          title = "Convertir",
          subtitle = "Cambiar formato y calidad de audio",
          description = "Cambia tus canciones y grabaciones a formatos populares como MP3, M4A, WAV o FLAC. Te ayuda a ahorrar espacio, ajustar el volumen y asegurar que tus audios se escuchen en cualquier dispositivo sin perder calidad.",
          icon = Icons.Default.Transform,
          badge = "Disponible",
          gradientColors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
          ),
          tags = listOf("12 Formatos", "Ajuste de Volumen", "Sin Internet", "Alta Calidad"),
          onClick = onNavigateToConverter,
          testTag = "tool_card_convert"
        )
      }

      // Primary Tool Card 2: EXTRAER DE VIDEO
      item {
        ActiveToolCard(
          title = "Extraer de Video",
          subtitle = "Aislar audio de MP4, MKV, WebM y grabaciones",
          description = "Extrae la pista de sonido de tus videos con opción de copia directa ultra rápida o conversión a MP3, AAC, FLAC y WAV manteniendo la máxima fidelidad acústica.",
          icon = Icons.Default.VideoLibrary,
          badge = "Nuevo",
          gradientColors = listOf(
            Color(0xFF9C27B0),
            Color(0xFF673AB7)
          ),
          tags = listOf("MP4/MKV a MP3", "Extracción Directa", "Zero Pérdida", "Ajuste de Ganancia"),
          onClick = onNavigateToVideoExtractor,
          testTag = "tool_card_video_extractor"
        )
      }

      // Section: Carpetas en tu Almacenamiento
      item {
        val context = LocalContext.current
        StorageFoldersCard(
          onOpenFolder = { folder ->
            AppStorageManager.openFolderInFileManager(context, folder)
          }
        )
      }

      // Section Title: Próximamente
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Próximas Herramientas en Desarrollo",
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Upcoming Roadmap Tools
      item {
        UpcomingToolCard(
          title = "Recortar Audio",
          description = "Corta segmentos exactos de audio con forma de onda interactiva, fundido suave Fade In/Out y creador de tonos.",
          icon = Icons.Default.ContentCut,
          badge = "Próximamente",
          accentColor = Color(0xFFE91E63)
        )
      }

      item {
        UpcomingToolCard(
          title = "Editor de Metadatos ID3",
          description = "Personaliza carátula, nombre de canción, artista, álbum, año y género en tus archivos de audio.",
          icon = Icons.Default.Label,
          badge = "Próximamente",
          accentColor = Color(0xFFFF9800)
        )
      }

      item {
        UpcomingToolCard(
          title = "Efectos & DSP",
          description = "Ecualizador acústico, normalización de sonoridad EBU R128 y modificación de velocidad sin perder tono.",
          icon = Icons.Default.Tune,
          badge = "Próximamente",
          accentColor = Color(0xFF009688)
        )
      }

      // Footer
      item {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Procesamiento 100% local en tu dispositivo. Tu música y grabaciones nunca salen de tu teléfono.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HubHeader() {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(
            Brush.linearGradient(
              colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary
              )
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Audiotrack,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(26.dp)
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(
          text = "Audio Studio",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Laboratorio de Audio y Herramientas",
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.primary
        )
      }
    }

    Surface(
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "100% Offline",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }
    }
  }
}

@Composable
private fun HubHeroBanner() {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.horizontalGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
              MaterialTheme.colorScheme.tertiary.copy(alpha = 0.04f)
            )
          )
        )
        .padding(18.dp)
    ) {
      Column {
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.ElectricBolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "TU ESTUDIO DE AUDIO PERSONAL",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Todo lo que necesitas para tu música y grabaciones",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Convierte, recorta y mejora tus audios directamente en tu teléfono, rápido, seguro y sin necesidad de conexión a internet.",
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.5.sp,
            lineHeight = 17.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveToolCard(
  title: String,
  subtitle: String,
  description: String,
  icon: ImageVector,
  badge: String,
  gradientColors: List<Color> = emptyList(),
  tags: List<String>,
  onClick: () -> Unit,
  testTag: String
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag)
      .clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(
                Brush.linearGradient(
                  colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary
                  )
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = title,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(26.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
          Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 12.5.sp,
          lineHeight = 18.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Tags
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        tags.forEach { tag ->
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          ) {
            Text(
              text = tag,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("button_open_converter")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Text(
            text = "Abrir Herramienta Convertir",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun UpcomingToolCard(
  title: String,
  description: String,
  icon: ImageVector,
  badge: String,
  accentColor: Color
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accentColor,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
          ) {
            Text(
              text = badge,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.5.sp,
            lineHeight = 16.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun StorageFoldersCard(
  onOpenFolder: (AppAudioFolder) -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Folder,
              contentDescription = "Carpetas",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Carpetas en tu Almacenamiento",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Música / AudioConverter /",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Tus archivos se organizan automáticamente en subcarpetas accesibles desde cualquier gestor de archivos:",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(10.dp))

      AppAudioFolder.values().take(3).forEach { folder ->
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onOpenFolder(folder) }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
              )
              Column {
                Text(
                  text = "📁 ${folder.folderName}",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = folder.description,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            IconButton(
              onClick = { onOpenFolder(folder) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Explorar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }
  }
}
