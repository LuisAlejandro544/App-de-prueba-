package com.example.ui.components.spatial

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpatialRotationSpeed
import com.example.model.SpatialTrajectory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radar visual interactivo que simula en tiempo real la órbita del sonido alrededor de la cabeza.
 */
@Composable
fun SpatialOrbitalRadar(
  trajectory: SpatialTrajectory,
  speed: SpatialRotationSpeed,
  depth: Float,
  modifier: Modifier = Modifier,
  isProcessing: Boolean = false
) {
  val infiniteTransition = rememberInfiniteTransition(label = "spatial_orbit")
  val durationMillis = (speed.secondsPerRevolution * 1000).toInt()

  val animationProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = durationMillis, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "orbit_phase"
  )

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Headphones,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Simulador Acústico Binaural 360°",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ) {
          Text(
            text = trajectory.displayName,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Canvas del Radar Orbital
      Box(
        modifier = Modifier
          .size(200.dp),
        contentAlignment = Alignment.Center
      ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.tertiary
        val outlineColor = MaterialTheme.colorScheme.outlineVariant

        Canvas(modifier = Modifier.size(200.dp)) {
          val center = Offset(size.width / 2f, size.height / 2f)
          val maxRadius = (size.minDimension / 2f) - 16.dp.toPx()

          // Círculos concéntricos del radar
          drawCircle(
            color = outlineColor.copy(alpha = 0.3f),
            radius = maxRadius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
          )
          drawCircle(
            color = outlineColor.copy(alpha = 0.2f),
            radius = maxRadius * 0.65f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
          )

          // Ejes cardinales (Frente, Atrás, Izquierda, Derecha)
          drawLine(
            color = outlineColor.copy(alpha = 0.25f),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 1.dp.toPx()
          )
          drawLine(
            color = outlineColor.copy(alpha = 0.25f),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 1.dp.toPx()
          )

          // Cálculo de posición según la trayectoria
          val angle = animationProgress * 2.0 * PI
          val azimuth = when (trajectory) {
            SpatialTrajectory.PENDULO_8 -> sin(angle) * (PI * 0.5)
            else -> angle
          }

          val effectiveRadius = maxRadius * depth.coerceIn(0.2f, 1.0f)
          val soundX = center.x + (sin(azimuth) * effectiveRadius).toFloat()
          val soundY = center.y - (cos(azimuth) * effectiveRadius).toFloat()

          // Estela de sonido
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(primaryColor.copy(alpha = 0.5f), Color.Transparent),
              center = Offset(soundX, soundY),
              radius = 28.dp.toPx()
            ),
            radius = 28.dp.toPx(),
            center = Offset(soundX, soundY)
          )

          // Fuente de sonido emisora
          drawCircle(
            color = primaryColor,
            radius = 8.dp.toPx(),
            center = Offset(soundX, soundY)
          )
          drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(soundX, soundY)
          )
        }

        // Cabeza / Oyente central
        Surface(
          modifier = Modifier.size(46.dp),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          shadowElevation = 4.dp
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Headphones,
              contentDescription = "Oyente",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Indicadores L/R
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Canal L (Izq)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = "Giro: ${(animationProgress * 360).toInt()}°",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = "Canal R (Der)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
