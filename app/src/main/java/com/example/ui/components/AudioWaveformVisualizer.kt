package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioWaveformVisualizer(
  isPlaying: Boolean,
  progress: Float,
  modifier: Modifier = Modifier,
  primaryColor: Color = MaterialTheme.colorScheme.primary,
  secondaryColor: Color = MaterialTheme.colorScheme.secondary
) {
  val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
  val phase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 2f * Math.PI.toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "phase"
  )

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
  ) {
    val barCount = 36
    val barSpacing = size.width / barCount
    val barWidth = barSpacing * 0.55f
    val centerY = size.height / 2f
    val maxBarHeight = size.height * 0.85f

    for (i in 0 until barCount) {
      val x = i * barSpacing + (barSpacing - barWidth) / 2f
      val fraction = i.toFloat() / barCount.toFloat()

      // Calculate bar height with a natural acoustic curve
      val baseHeight = (sin(fraction * 3.5f * Math.PI.toFloat()) * 0.5f + 0.5f).coerceIn(0.15f, 1f)
      val animatedMultiplier = if (isPlaying) {
        (sin(fraction * 6f + phase) * 0.35f + 0.65f)
      } else {
        0.55f
      }

      val barHeight = (maxBarHeight * baseHeight * animatedMultiplier).coerceAtLeast(6f)
      val top = centerY - (barHeight / 2f)

      val isPlayed = fraction <= progress
      val barBrush = if (isPlayed) {
        Brush.verticalGradient(
          colors = listOf(secondaryColor, primaryColor),
          startY = top,
          endY = top + barHeight
        )
      } else {
        Brush.verticalGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.25f),
            primaryColor.copy(alpha = 0.15f)
          ),
          startY = top,
          endY = top + barHeight
        )
      }

      drawRoundRect(
        brush = barBrush,
        topLeft = Offset(x, top),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
      )
    }
  }
}
