package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.audio.AppStorageManager
import com.example.ui.AudioConverterScreen
import com.example.ui.MainHubScreen
import com.example.ui.VideoToAudioScreen
import com.example.ui.theme.MyApplicationTheme

enum class AudioAppScreen {
  HUB,
  CONVERTER,
  VIDEO_EXTRACTOR
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Crear jerarquía de carpetas accesibles en el almacenamiento
    AppStorageManager.ensureAllFoldersExist(this)

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          var currentScreen by remember { mutableStateOf(AudioAppScreen.HUB) }

          BackHandler(enabled = currentScreen != AudioAppScreen.HUB) {
            currentScreen = AudioAppScreen.HUB
          }

          AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
              if (targetState != AudioAppScreen.HUB) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                  slideOutHorizontally { width -> -width / 3 } + fadeOut()
                )
              } else {
                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                  slideOutHorizontally { width -> width } + fadeOut()
                )
              }
            },
            label = "screen_transition"
          ) { screen ->
            when (screen) {
              AudioAppScreen.HUB -> {
                MainHubScreen(
                  onNavigateToConverter = {
                    currentScreen = AudioAppScreen.CONVERTER
                  },
                  onNavigateToVideoExtractor = {
                    currentScreen = AudioAppScreen.VIDEO_EXTRACTOR
                  }
                )
              }
              AudioAppScreen.CONVERTER -> {
                AudioConverterScreen(
                  onNavigateBack = {
                    currentScreen = AudioAppScreen.HUB
                  }
                )
              }
              AudioAppScreen.VIDEO_EXTRACTOR -> {
                VideoToAudioScreen(
                  onNavigateBack = {
                    currentScreen = AudioAppScreen.HUB
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}


