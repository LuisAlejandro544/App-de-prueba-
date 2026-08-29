package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AudioMetadataReader
import com.example.model.AudioFormat
import com.example.model.ConvertedAudioFile
import com.example.model.ExtractionMode
import com.example.model.QualityPreset
import com.example.model.VideoExtractionProgress
import com.example.model.VideoExtractionState
import com.example.model.VideoFileInfo
import com.example.ui.components.AudioPlayerCard
import com.example.viewmodel.VideoToAudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoToAudioScreen(
  onNavigateBack: () -> Unit,
  viewModel: VideoToAudioViewModel = viewModel()
) {
  val context = LocalContext.current
  val selectedVideo by viewModel.selectedVideo.collectAsState()
  val options by viewModel.options.collectAsState()
  val progress by viewModel.progress.collectAsState()
  val isAnalyzing by viewModel.isAnalyzing.collectAsState()
  val history by viewModel.extractedHistory.collectAsState()
  val playbackState by viewModel.playbackState.collectAsState()

  val videoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let { viewModel.selectVideo(context, it) }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Extraer Audio de Video",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
              )
            )
            Text(
              text = "MP4, MKV, WebM a MP3 / AAC / WAV",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("btn_back_video_screen")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Volver al Hub"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      if (selectedVideo != null) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
          tonalElevation = 8.dp,
          shadowElevation = 12.dp,
          color = MaterialTheme.colorScheme.surface
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 14.dp)
          ) {
            Button(
              onClick = { viewModel.startExtraction(context) },
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("extract_audio_button"),
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
              )
            ) {
              Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Extraer en ${options.targetFormat.badge}",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp
                )
              )
            }
          }
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // 1. Selector de Video o Tarjeta de Video Seleccionado
      item {
        if (selectedVideo == null) {
          VideoSelectPlaceholder(
            isAnalyzing = isAnalyzing,
            onPickVideo = { videoPickerLauncher.launch("video/*") }
          )
        } else {
          SelectedVideoCard(
            video = selectedVideo!!,
            onChangeVideo = { videoPickerLauncher.launch("video/*") },
            onRemove = { viewModel.clearSelectedVideo() }
          )
        }
      }

      // Si hay un video seleccionado, mostramos las opciones de configuración
      if (selectedVideo != null) {
        val video = selectedVideo!!

        // 2. Selector de Modo de Extracción (Directo vs Conversión)
        item {
          ExtractionModeSelector(
            currentMode = options.mode,
            videoAudioCodec = video.audioCodecName,
            onModeSelected = { viewModel.setExtractionMode(it) }
          )
        }

        // 3. Selector de Formato de Audio Destino
        item {
          VideoTargetFormatSection(
            selectedFormat = options.targetFormat,
            onFormatSelected = { viewModel.setTargetFormat(it) }
          )
        }

        // 4. Parámetros de Calidad y Volumen (si no es modo directo)
        if (options.mode == ExtractionMode.HIGH_FIDELITY_CONVERSION) {
          item {
            VideoQualitySettingsSection(
              selectedPreset = options.preset,
              currentBitrate = options.bitrateKbps,
              volumeMultiplier = options.volumeMultiplier,
              onPresetSelected = { viewModel.setQualityPreset(it) },
              onBitrateChanged = { viewModel.setBitrate(it) },
              onVolumeChanged = { viewModel.setVolume(it) }
            )
          }
        }

        // 5. Nombre de archivo personalizado
        item {
          CustomFileNameSection(
            fileName = options.customFileName,
            targetExtension = options.targetFormat.extension,
            onFileNameChange = { viewModel.setCustomFileName(it) }
          )
        }
      }

      // 6. Historial de audios extraídos
      if (history.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(10.dp))
          
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Folder,
                  contentDescription = "Carpeta de almacenamiento",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Column {
                  Text(
                    text = "Carpeta de destino",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = "Música / AudioConverter / Video a Audio",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              IconButton(
                onClick = { viewModel.openOutputFolder(context) },
                modifier = Modifier.size(36.dp).testTag("btn_open_video_folder")
              ) {
                Icon(
                  imageVector = Icons.Default.FolderOpen,
                  contentDescription = "Abrir carpeta en el explorador",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Audios Extraídos (${history.size})",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
              )
            )
          }
        }

        items(history, key = { it.id }) { item ->
          ExtractedFileCard(
            file = item,
            playbackState = playbackState,
            onPlayPause = { viewModel.togglePlayFile(context, item) },
            onSeek = { viewModel.seekTo(it) },
            onShare = { viewModel.shareExtractedFile(context, item) },
            onSaveToMusic = { viewModel.saveToMusicFolder(context, item) },
            onDelete = { viewModel.deleteExtractedFile(item) }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(30.dp))
      }
    }
  }

  // Diálogo de progreso en tiempo real
  if (progress.state == VideoExtractionState.EXTRACTING || progress.state == VideoExtractionState.COMPLETED || progress.state == VideoExtractionState.ERROR) {
    VideoExtractionProgressDialog(
      progress = progress,
      playbackState = playbackState,
      onPlayPause = { progress.extractedFile?.let { viewModel.togglePlayFile(context, it) } },
      onSeek = { viewModel.seekTo(it) },
      onDismiss = { viewModel.dismissProgressDialog() },
      onCancel = { viewModel.cancelExtraction() },
      onShare = { file -> viewModel.shareExtractedFile(context, file) },
      onSaveToMusic = { file -> viewModel.saveToMusicFolder(context, file) }
    )
  }
}

@Composable
private fun VideoSelectPlaceholder(
  isAnalyzing: Boolean,
  onPickVideo: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_select_video"),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.verticalGradient(
        listOf(
          MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
          MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
        )
      )
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
      ) {
        if (isAnalyzing) {
          CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
          )
        } else {
          Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = if (isAnalyzing) "Analizando pistas del video..." else "Selecciona un Video",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Extrae el audio en alta calidad de videos grabados con tu cámara, descargados o compartidos en WhatsApp.",
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = onPickVideo,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("btn_choose_video_file"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        )
      ) {
        Icon(
          imageVector = Icons.Default.VideoFile,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Explorar Videos del Dispositivo", fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
private fun SelectedVideoCard(
  video: VideoFileInfo,
  onChangeVideo: () -> Unit,
  onRemove: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_selected_video"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Thumbnail o Icono
        if (video.thumbnail != null) {
          Box(
            modifier = Modifier
              .size(68.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color.Black)
          ) {
            Image(
              bitmap = video.thumbnail.asImageBitmap(),
              contentDescription = "Miniatura del video",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
            Surface(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp),
              shape = RoundedCornerShape(4.dp),
              color = Color.Black.copy(alpha = 0.7f)
            ) {
              Text(
                text = AudioMetadataReader.formatDuration(video.durationMs),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }
        } else {
          Box(
            modifier = Modifier
              .size(68.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Movie,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(32.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = video.name,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "${video.containerExtension} • ${AudioMetadataReader.formatFileSize(video.sizeBytes)} • ${video.resolutionLabel}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Badge de pista de audio
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Audio: ${video.audioCodecName} • ${video.audioBitrateKbps} kbps • ${video.audioSampleRate / 1000} kHz",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              )
            }
          }
        }

        IconButton(onClick = onRemove) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Quitar video",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        OutlinedButton(
          onClick = onChangeVideo,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Text("Cambiar Video", fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun ExtractionModeSelector(
  currentMode: ExtractionMode,
  videoAudioCodec: String,
  onModeSelected: (ExtractionMode) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "Modo de Extracción",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Opción 1: Conversión Personalizada
      ExtractionModeCard(
        title = "Conversión de Audio",
        subtitle = "Elige formato (MP3, WAV, FLAC), calidad y volumen.",
        icon = Icons.Default.Tune,
        isSelected = currentMode == ExtractionMode.HIGH_FIDELITY_CONVERSION,
        modifier = Modifier.weight(1f),
        onClick = { onModeSelected(ExtractionMode.HIGH_FIDELITY_CONVERSION) }
      )

      // Opción 2: Extracción Directa
      ExtractionModeCard(
        title = "Extracción Directa",
        subtitle = "Ultra rápida en segundos (pista $videoAudioCodec original).",
        icon = Icons.Default.FlashOn,
        isSelected = currentMode == ExtractionMode.DIRECT_STREAM_COPY,
        modifier = Modifier.weight(1f),
        onClick = { onModeSelected(ExtractionMode.DIRECT_STREAM_COPY) }
      )
    }
  }
}

@Composable
private fun ExtractionModeCard(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .animateContentSize(),
    shape = RoundedCornerShape(14.dp),
    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
      )
    ) else CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier.padding(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          ),
          color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun VideoTargetFormatSection(
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
private fun FormatChipItem(
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

@Composable
private fun VideoQualitySettingsSection(
  selectedPreset: QualityPreset,
  currentBitrate: Int,
  volumeMultiplier: Float,
  onPresetSelected: (QualityPreset) -> Unit,
  onBitrateChanged: (Int) -> Unit,
  onVolumeChanged: (Float) -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.HighQuality,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Calidad & Ganancia de Audio",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Presets
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(QualityPreset.STANDARD, QualityPreset.HIGH, QualityPreset.ULTRA).forEach { preset ->
          val isSelected = selectedPreset == preset
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .clickable { onPresetSelected(preset) },
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
          ) {
            Text(
              text = "${preset.bitrateKbps} kbps",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
              ),
              color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Slider de Volumen
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Volumen de salida",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
          )
        }
        Text(
          text = "${(volumeMultiplier * 100).toInt()}%",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        )
      }

      Slider(
        value = volumeMultiplier,
        onValueChange = onVolumeChanged,
        valueRange = 0.5f..1.5f,
        steps = 9,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
          thumbColor = MaterialTheme.colorScheme.primary,
          activeTrackColor = MaterialTheme.colorScheme.primary
        )
      )
    }
  }
}

@Composable
private fun CustomFileNameSection(
  fileName: String,
  targetExtension: String,
  onFileNameChange: (String) -> Unit
) {
  OutlinedTextField(
    value = fileName,
    onValueChange = onFileNameChange,
    label = { Text("Nombre del archivo de audio resultante") },
    suffix = { Text(".$targetExtension", fontWeight = FontWeight.Bold) },
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
    )
  )
}

@Composable
private fun ExtractedFileCard(
  file: ConvertedAudioFile,
  playbackState: com.example.audio.PlaybackState,
  onPlayPause: () -> Unit,
  onSeek: (Long) -> Unit,
  onShare: () -> Unit,
  onSaveToMusic: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("card_extracted_file_${file.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primary
        ) {
          Text(
            text = file.format.badge,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = file.name,
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${AudioMetadataReader.formatFileSize(file.sizeBytes)} • ${AudioMetadataReader.formatDuration(file.durationMs)} • ${file.bitrateKbps} kbps",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Eliminar",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Reproductor de audio integrado
      AudioPlayerCard(
        playbackState = playbackState,
        onPlayPause = onPlayPause,
        onSeek = onSeek
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onShare,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(vertical = 6.dp)
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Compartir", fontSize = 12.sp)
        }

        Button(
          onClick = onSaveToMusic,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(vertical = 6.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
          )
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Guardar", fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun VideoExtractionProgressDialog(
  progress: VideoExtractionProgress,
  playbackState: com.example.audio.PlaybackState,
  onPlayPause: () -> Unit,
  onSeek: (Long) -> Unit,
  onDismiss: () -> Unit,
  onCancel: () -> Unit,
  onShare: (ConvertedAudioFile) -> Unit,
  onSaveToMusic: (ConvertedAudioFile) -> Unit
) {
  Dialog(onDismissRequest = {
    if (progress.state != VideoExtractionState.EXTRACTING) {
      onDismiss()
    }
  }) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        when (progress.state) {
          VideoExtractionState.EXTRACTING -> {
            CircularProgressIndicator(
              progress = { progress.progressPercent },
              modifier = Modifier.size(64.dp),
              strokeWidth = 6.dp,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
              text = "Extrayendo Pista de Audio",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = progress.statusMessage,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
              progress = { progress.progressPercent },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
              onClick = onCancel,
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Cancelar Extracción")
            }
          }

          VideoExtractionState.COMPLETED -> {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Color(0xFF4CAF50),
              modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "¡Audio Extraído con Éxito!",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = progress.statusMessage,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            progress.extractedFile?.let { file ->
              Spacer(modifier = Modifier.height(16.dp))
              AudioPlayerCard(
                playbackState = playbackState,
                onPlayPause = onPlayPause,
                onSeek = onSeek
              )
              Spacer(modifier = Modifier.height(14.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { onShare(file) },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Compartir", fontSize = 12.sp)
                }

                Button(
                  onClick = { onSaveToMusic(file) },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Guardar", fontSize = 12.sp)
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onDismiss,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Aceptar")
            }
          }

          VideoExtractionState.ERROR -> {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Error en la Extracción",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = progress.errorMessage ?: "Ocurrió un error al procesar el archivo de video.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onDismiss,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Cerrar")
            }
          }

          else -> {}
        }
      }
    }
  }
}
