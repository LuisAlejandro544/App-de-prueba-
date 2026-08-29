package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ExtractionMode
import com.example.model.VideoExtractionState
import com.example.ui.components.video.CustomFileNameSection
import com.example.ui.components.video.ExtractedFileCard
import com.example.ui.components.video.ExtractionModeSelector
import com.example.ui.components.video.SelectedVideoCard
import com.example.ui.components.video.VideoExtractionProgressDialog
import com.example.ui.components.video.VideoQualitySettingsSection
import com.example.ui.components.video.VideoSelectPlaceholder
import com.example.ui.components.video.VideoTargetFormatSection
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

      // Opciones de configuración si hay un video seleccionado
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

        // 4. Parámetros de Calidad y Volumen (si es modo conversión)
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

      // 6. Historial de audios extraídos y carpeta de almacenamiento
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
  if (progress.state == VideoExtractionState.EXTRACTING ||
      progress.state == VideoExtractionState.COMPLETED ||
      progress.state == VideoExtractionState.ERROR) {
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
