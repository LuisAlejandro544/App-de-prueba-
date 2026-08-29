package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Animación morfológica en bucle:
 * Icono de Video -> Transmutación con partículas y rotación -> Icono de Audio y Ondas
 */
@Composable
fun VideoToAudioMorphAnimation(
  modifier: Modifier = Modifier,
  size: Int = 88
) {
  val infiniteTransition = rememberInfiniteTransition(label = "video_audio_morph")

  // Progreso cíclico de la animación de 0f a 1f cada 2400 ms
  val animationProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "progress"
  )

  // Pulsación continua del aro exterior
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  // Cálculo de fases de transformación:
  // 0.0 - 0.40 -> Mostrar Video
  // 0.40 - 0.60 -> Transición/Morphing
  // 0.60 - 0.90 -> Mostrar Audio
  // 0.90 - 1.00 -> Regreso suave a Video

  val isVideoPhase = animationProgress < 0.5f

  val videoAlpha: Float
  val videoScale: Float
  val videoRotation: Float

  val audioAlpha: Float
  val audioScale: Float
  val audioRotation: Float

  if (animationProgress < 0.40f) {
    // Fase pura de Video
    videoAlpha = 1f
    videoScale = 1f
    videoRotation = 0f

    audioAlpha = 0f
    audioScale = 0.4f
    audioRotation = -90f
  } else if (animationProgress < 0.50f) {
    // Desvaneciendo video
    val t = (animationProgress - 0.40f) / 0.10f
    videoAlpha = 1f - t
    videoScale = 1f - 0.4f * t
    videoRotation = 45f * t

    audioAlpha = 0f
    audioScale = 0.4f
    audioRotation = -90f
  } else if (animationProgress < 0.60f) {
    // Apareciendo audio
    val t = (animationProgress - 0.50f) / 0.10f
    videoAlpha = 0f
    videoScale = 0.6f
    videoRotation = 45f

    audioAlpha = t
    audioScale = 0.4f + 0.6f * t
    audioRotation = -90f + 90f * t
  } else if (animationProgress < 0.90f) {
    // Fase pura de Audio
    videoAlpha = 0f
    videoScale = 0.6f
    videoRotation = 0f

    audioAlpha = 1f
    audioScale = 1f
    audioRotation = 0f
  } else {
    // Retorno suave a Video
    val t = (animationProgress - 0.90f) / 0.10f
    videoAlpha = t
    videoScale = 0.6f + 0.4f * t
    videoRotation = 0f

    audioAlpha = 1f - t
    audioScale = 1f - 0.4f * t
    audioRotation = 45f * t
  }

  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.tertiary
  val containerColor = MaterialTheme.colorScheme.primaryContainer

  Box(
    modifier = modifier.size(size.dp),
    contentAlignment = Alignment.Center
  ) {
    // Aro exterior pulsante de resplandor
    Box(
      modifier = Modifier
        .size((size * 0.95).dp)
        .scale(pulseScale)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            listOf(
              primaryColor.copy(alpha = 0.35f),
              secondaryColor.copy(alpha = 0.15f),
              Color.Transparent
            )
          )
        )
    )

    // Contenedor circular principal
    Box(
      modifier = Modifier
        .size((size * 0.78).dp)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            if (isVideoPhase) listOf(containerColor, primaryColor.copy(alpha = 0.3f))
            else listOf(secondaryColor.copy(alpha = 0.3f), containerColor)
          )
        ),
      contentAlignment = Alignment.Center
    ) {
      // 1. Icono de Video
      if (videoAlpha > 0f) {
        Icon(
          imageVector = Icons.Default.Videocam,
          contentDescription = "Video",
          tint = primaryColor,
          modifier = Modifier
            .size((size * 0.42).dp)
            .graphicsLayer {
              alpha = videoAlpha
              scaleX = videoScale
              scaleY = videoScale
              rotationZ = videoRotation
            }
        )
      }

      // 2. Icono de Audio
      if (audioAlpha > 0f) {
        Icon(
          imageVector = Icons.Default.Audiotrack,
          contentDescription = "Audio",
          tint = secondaryColor,
          modifier = Modifier
            .size((size * 0.42).dp)
            .graphicsLayer {
              alpha = audioAlpha
              scaleX = audioScale
              scaleY = audioScale
              rotationZ = audioRotation
            }
        )
      }

      // 3. Mini destello de transformación en el punto de cruce
      if (animationProgress in 0.45f..0.55f) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .size((size * 0.32).dp)
            .scale(1.2f)
        )
      }
    }

    // Pequeñas barras de ecualizador en la parte inferior durante la fase de audio
    if (audioAlpha > 0.4f) {
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .graphicsLayer { alpha = audioAlpha },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
      ) {
        val bar1Height by infiniteTransition.animateFloat(
          initialValue = 4f,
          targetValue = 14f,
          animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse),
          label = "b1"
        )
        val bar2Height by infiniteTransition.animateFloat(
          initialValue = 12f,
          targetValue = 5f,
          animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
          label = "b2"
        )
        val bar3Height by infiniteTransition.animateFloat(
          initialValue = 6f,
          targetValue = 16f,
          animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse),
          label = "b3"
        )

        Box(
          Modifier
            .width(3.dp)
            .height(bar1Height.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(secondaryColor)
        )
        Box(
          Modifier
            .width(3.dp)
            .height(bar2Height.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(primaryColor)
        )
        Box(
          Modifier
            .width(3.dp)
            .height(bar3Height.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(secondaryColor)
        )
      }
    }
  }
}

/**
 * Animación morfológica para conversión entre formatos de audio:
 * Muestra transcodificación continua con ecualizador, rotación de destello y ondas
 */
@Composable
fun AudioToAudioMorphAnimation(
  modifier: Modifier = Modifier,
  size: Int = 88
) {
  val infiniteTransition = rememberInfiniteTransition(label = "audio_morph")

  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rot"
  )

  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.06f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  val primaryColor = MaterialTheme.colorScheme.primary
  val tertiaryColor = MaterialTheme.colorScheme.tertiary

  Box(
    modifier = modifier.size(size.dp),
    contentAlignment = Alignment.Center
  ) {
    // Anillo giratorio de gradiente exterior
    Box(
      modifier = Modifier
        .size((size * 0.92).dp)
        .graphicsLayer { rotationZ = rotation }
        .clip(CircleShape)
        .background(
          Brush.sweepGradient(
            listOf(
              primaryColor.copy(alpha = 0.5f),
              tertiaryColor.copy(alpha = 0.1f),
              primaryColor.copy(alpha = 0.6f),
              tertiaryColor.copy(alpha = 0.1f),
              primaryColor.copy(alpha = 0.5f)
            )
          )
        )
    )

    // Contenedor central pulsante
    Box(
      modifier = Modifier
        .size((size * 0.76).dp)
        .scale(pulseScale)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size((size * 0.64).dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.GraphicEq,
          contentDescription = "Convirtiendo Audio",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size((size * 0.38).dp)
        )
      }
    }

    // Barras ecualizadoras dinámicas flotantes
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      val b1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "ab1"
      )
      val b2 by infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "ab2"
      )
      val b3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "ab3"
      )
      val b4 by infiniteTransition.animateFloat(
        initialValue = 10f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "ab4"
      )

      Box(Modifier.width(2.5.dp).height(b1.dp).clip(RoundedCornerShape(1.dp)).background(tertiaryColor))
      Box(Modifier.width(2.5.dp).height(b2.dp).clip(RoundedCornerShape(1.dp)).background(primaryColor))
      Box(Modifier.width(2.5.dp).height(b3.dp).clip(RoundedCornerShape(1.dp)).background(tertiaryColor))
      Box(Modifier.width(2.5.dp).height(b4.dp).clip(RoundedCornerShape(1.dp)).background(primaryColor))
    }
  }
}
