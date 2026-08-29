package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AppStorageManager
import com.example.ui.components.hub.ActiveToolCard
import com.example.ui.components.hub.ActiveToolGridCard
import com.example.ui.components.hub.ActiveToolListRow
import com.example.ui.components.hub.HubHeader
import com.example.ui.components.hub.HubHeroBanner
import com.example.ui.components.hub.HubPrivacyFooter
import com.example.ui.components.hub.HubToolItem
import com.example.ui.components.hub.HubViewMode
import com.example.ui.components.hub.HubViewModeSelector
import com.example.ui.components.hub.StorageFoldersCard
import com.example.ui.components.hub.UpcomingToolCard

@Composable
fun MainHubScreen(
  onNavigateToConverter: () -> Unit,
  onNavigateToCompressor: () -> Unit = {},
  onNavigateToSplitter: () -> Unit = {},
  onNavigateToVideoExtractor: () -> Unit = {},
  onNavigateToSpatial8D: () -> Unit = {},
  onNavigateToSilenceRemover: () -> Unit = {},
  onNavigateToMerger: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var viewMode by rememberSaveable { mutableStateOf(HubViewMode.GRID) }

  val tools = remember(
    onNavigateToConverter,
    onNavigateToCompressor,
    onNavigateToSplitter,
    onNavigateToVideoExtractor,
    onNavigateToSpatial8D,
    onNavigateToSilenceRemover,
    onNavigateToMerger
  ) {
    listOf(
      HubToolItem(
        id = "converter",
        title = "Convertir",
        subtitle = "Cambiar formato y calidad de audio",
        description = "Cambia tus canciones y grabaciones a formatos populares como MP3, M4A, WAV o FLAC. Te ayuda a asegurar compatibilidad, ajustar el volumen y escuchar en cualquier equipo.",
        icon = Icons.Default.Transform,
        badge = "Disponible",
        gradientColors = listOf(Color(0xFF2979FF), Color(0xFF00B0FF)),
        tags = listOf("12 Formatos", "Ajuste de Volumen", "Sin Internet", "Alta Calidad"),
        onClick = onNavigateToConverter,
        testTag = "tool_card_convert"
      ),
      HubToolItem(
        id = "compressor",
        title = "Comprimir",
        subtitle = "Reduce el tamaño en MB sin perder calidad",
        description = "Optimiza tus archivos de audio para compartirlos por WhatsApp, Discord o Email sin errores de límite de tamaño. Ahorra hasta un 85% de espacio en memoria con estimador en tiempo real.",
        icon = Icons.Default.Compress,
        badge = "Nuevo",
        gradientColors = listOf(Color(0xFF00BFA5), Color(0xFF00E676)),
        tags = listOf("WhatsApp (<16MB)", "Email (<25MB)", "Smart Shrink", "Ahorro hasta 85%"),
        onClick = onNavigateToCompressor,
        testTag = "tool_card_compressor"
      ),
      HubToolItem(
        id = "splitter",
        title = "Dividir por Silencios",
        subtitle = "Separa un audio largo en múltiples pistas",
        description = "Corta automáticamente grabaciones de sesiones, álbumes o conferencias en pistas individuales según las pausas detectadas. Previsualiza, renombra y empaqueta en ZIP.",
        icon = Icons.Default.CallSplit,
        badge = "Nuevo",
        gradientColors = listOf(Color(0xFF7C4DFF), Color(0xFF651FFF)),
        tags = listOf("Auto-Chunker", "Múltiples Pistas", "Exportar ZIP", "Preescucha"),
        onClick = onNavigateToSplitter,
        testTag = "tool_card_splitter"
      ),
      HubToolItem(
        id = "silence_remover",
        title = "Eliminar Silencios",
        subtitle = "Recorta pausas vacías y acelera notas de voz",
        description = "Detecta y suprime automáticamente momentos muertos y pausas largas en notas de voz, grabaciones, podcasts y clases. Ahorra hasta un 40% de tiempo de escucha con micro-fundidos anti-chasquidos.",
        icon = Icons.Default.ContentCut,
        badge = "Disponible",
        gradientColors = listOf(Color(0xFFFF5722), Color(0xFFFF9800)),
        tags = listOf("Ahorro de Tiempo", "Detección dB RMS", "Padding de Voz", "Smart Cut"),
        onClick = onNavigateToSilenceRemover,
        testTag = "tool_card_silence_remover"
      ),
      HubToolItem(
        id = "video_extractor",
        title = "Extraer de Video",
        subtitle = "Aislar audio de MP4, MKV y WebM",
        description = "Extrae la pista de sonido de tus videos con opción de copia directa ultra rápida o conversión a MP3, AAC, FLAC y WAV manteniendo la máxima fidelidad acústica.",
        icon = Icons.Default.VideoLibrary,
        badge = "Disponible",
        gradientColors = listOf(Color(0xFF9C27B0), Color(0xFF673AB7)),
        tags = listOf("MP4/MKV a MP3", "Extracción Directa", "Zero Pérdida", "Ajuste de Ganancia"),
        onClick = onNavigateToVideoExtractor,
        testTag = "tool_card_video_extractor"
      ),
      HubToolItem(
        id = "spatial_8d",
        title = "Audio 8D Espacial",
        subtitle = "Sonido 360° binaural envolvente",
        description = "Convierte cualquier canción o audio en una experiencia holofónica 8D. El sonido orbita suavemente alrededor de tu cabeza con retardos interaurales (ITD), sombra craneal y acústica de sala.",
        icon = Icons.Default.SpatialAudio,
        badge = "Disponible",
        gradientColors = listOf(Color(0xFFE91E63), Color(0xFF8E24AA)),
        tags = listOf("Efecto 360°", "Binaural DSP", "Reverb de Sala", "🎧 Auriculares"),
        onClick = onNavigateToSpatial8D,
        testTag = "tool_card_spatial_8d"
      ),
      HubToolItem(
        id = "merger",
        title = "Unir Audios",
        subtitle = "Combina hasta 6 canciones o pistas",
        description = "Une múltiples archivos de audio en el orden que desees. Incluye remuestreo acústico inteligente (DSP), balance de canales (Mono a Estéreo) y micro-fundido suave anti-chasquidos.",
        icon = Icons.Default.Layers,
        badge = "Disponible",
        gradientColors = listOf(Color(0xFF00897B), Color(0xFF00ACC1)),
        tags = listOf("Hasta 6 Pistas", "Reordenable", "Normalización DSP", "Anti-Chasquidos"),
        onClick = onNavigateToMerger,
        testTag = "tool_card_merger"
      )
    )
  }

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
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Header principal
      item {
        HubHeader()
      }

      // 2. Banner descriptivo
      item {
        HubHeroBanner()
      }

      // 3. Título de sección de herramientas con Selector de Vista
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Herramientas",
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
                text = "${tools.size} Activas",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }
          }

          // Selector de Vista: Cuadrícula | Lista Compacta | Detallada
          HubViewModeSelector(
            selectedMode = viewMode,
            onModeSelected = { viewMode = it }
          )
        }
      }

      // 4. Renderizado según el modo de visualización seleccionado
      when (viewMode) {
        HubViewMode.GRID -> {
          // Vista Cuadrícula de 2 Columnas
          tools.chunked(2).forEach { rowTools ->
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                rowTools.forEach { tool ->
                  Box(modifier = Modifier.weight(1f)) {
                    ActiveToolGridCard(tool = tool)
                  }
                }
                if (rowTools.size == 1) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }

        HubViewMode.COMPACT_LIST -> {
          // Vista Lista Horizontal Compacta
          tools.forEach { tool ->
            item {
              ActiveToolListRow(tool = tool)
            }
          }
        }

        HubViewMode.DETAILED -> {
          // Vista Detallada Expandida
          tools.forEach { tool ->
            item {
              ActiveToolCard(tool = tool)
            }
          }
        }
      }

      // 5. Gestión de carpetas en almacenamiento
      item {
        Spacer(modifier = Modifier.height(4.dp))
        StorageFoldersCard(
          onOpenFolder = { folder ->
            AppStorageManager.openFolderInFileManager(context, folder)
          }
        )
      }

      // 6. Próximas herramientas en desarrollo
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Próximas Herramientas en Desarrollo",
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

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

      // 7. Pie de privacidad
      item {
        Spacer(modifier = Modifier.height(10.dp))
        HubPrivacyFooter()
      }
    }
  }
}
