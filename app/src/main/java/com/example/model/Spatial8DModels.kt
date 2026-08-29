package com.example.model

import android.net.Uri
import java.io.File

/**
 * Velocidades de rotación para la trayectoria del audio 8D.
 */
enum class SpatialRotationSpeed(
  val id: String,
  val displayName: String,
  val secondsPerRevolution: Float,
  val frequencyHz: Float,
  val description: String
) {
  LENTA(
    id = "lenta",
    displayName = "Lenta (16s)",
    secondsPerRevolution = 16.0f,
    frequencyHz = 1.0f / 16.0f,
    description = "Giro pausado y suave, ideal para canciones acústicas, Lo-Fi y relajación."
  ),
  NORMAL(
    id = "normal",
    displayName = "Normal (10s)",
    secondsPerRevolution = 10.0f,
    frequencyHz = 1.0f / 10.0f,
    description = "Velocidad estándar balanceada recomendada para Pop, Rock y Podcasts."
  ),
  RAPIDA(
    id = "rapida",
    displayName = "Rápida (6s)",
    secondsPerRevolution = 6.0f,
    frequencyHz = 1.0f / 6.0f,
    description = "Movimiento dinámico continuo, perfecto para EDM, Trap y Electrónica."
  ),
  DINAMICA(
    id = "dinamica",
    displayName = "Ultra Rápida (4s)",
    secondsPerRevolution = 4.0f,
    frequencyHz = 1.0f / 4.0f,
    description = "Efecto envolvente de alta rotación para transiciones y beats intensos."
  )
}

/**
 * Patrones de movimiento espacial del sonido.
 */
enum class SpatialTrajectory(
  val id: String,
  val displayName: String,
  val description: String
) {
  CIRCULAR_360(
    id = "circular_360",
    displayName = "Orbital 360°",
    description = "El sonido gira en círculo continuo alrededor de tu cabeza en el plano horizontal."
  ),
  PENDULO_8(
    id = "pendulo_8",
    displayName = "Péndulo Infinito (∞)",
    description = "Vaivén fluido de izquierda a derecha pasando por el centro auditivo."
  ),
  EXPANSION_3D(
    id = "expansion_3d",
    displayName = "Envolvente 3D",
    description = "Profundidad binaural ampliada con modulación armónica frontal y trasera."
  )
}

/**
 * Ajustes de acústica y sala (Reverb Espacial).
 */
enum class SpatialReverbPreset(
  val id: String,
  val displayName: String,
  val roomSize: Float,
  val damping: Float,
  val wetLevel: Float,
  val description: String
) {
  DESACTIVADO(
    id = "off",
    displayName = "Directo",
    roomSize = 0.0f,
    damping = 0.0f,
    wetLevel = 0.0f,
    description = "Sin reverberación añadida, paneo 8D puro y nítido."
  ),
  ESTUDIO(
    id = "estudio",
    displayName = "Estudio",
    roomSize = 0.4f,
    damping = 0.5f,
    wetLevel = 0.18f,
    description = "Ambiente cálido y controlado con reflexiones acústicas sutiles."
  ),
  SALA_CONCIERTOS(
    id = "sala_conciertos",
    displayName = "Sala de Conciertos",
    roomSize = 0.72f,
    damping = 0.35f,
    wetLevel = 0.28f,
    description = "Gran amplitud estéreo con sensación de escenario en vivo."
  ),
  CATEDRAL(
    id = "catedral",
    displayName = "Catedral Espaciosa",
    roomSize = 0.88f,
    damping = 0.2f,
    wetLevel = 0.38f,
    description = "Eco profundo y envolvente para una experiencia cinematográfica."
  )
}

/**
 * Configuración completa para el procesamiento de Audio 8D.
 */
data class Spatial8DOptions(
  val rotationSpeed: SpatialRotationSpeed = SpatialRotationSpeed.NORMAL,
  val trajectory: SpatialTrajectory = SpatialTrajectory.CIRCULAR_360,
  val reverbPreset: SpatialReverbPreset = SpatialReverbPreset.ESTUDIO,
  val spatialDepth: Float = 0.85f, // 0.2f (sutil) a 1.0f (máxima inmersión)
  val targetFormat: AudioFormat = AudioFormat.MP3,
  val bitrateKbps: Int = 320,
  val customFileName: String? = null
)

/**
 * Estados del flujo de procesamiento 8D.
 */
enum class Spatial8DState {
  IDLE,
  ANALYZING,
  DECODING_PCM,
  APPLYING_8D_DSP,
  ENCODING_OUTPUT,
  COMPLETED,
  ERROR
}

/**
 * Progreso en tiempo real de la generación de Audio 8D.
 */
data class Spatial8DProgress(
  val state: Spatial8DState = Spatial8DState.IDLE,
  val progressPercent: Float = 0f,
  val currentPhaseText: String = "",
  val processedDurationMs: Long = 0L,
  val totalDurationMs: Long = 0L,
  val outputFile: ConvertedAudioFile? = null,
  val errorMessage: String? = null
)
