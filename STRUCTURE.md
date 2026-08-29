# Estructura del Proyecto - Audio Converter

Árbol de directorios y organización de módulos del proyecto:

```
├── app/                                 # Módulo principal Android
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml      # Manifiesto con permisos y configuración
│   │   │   ├── cpp/                     # Código fuente C++ (NDK / CMake)
│   │   │   │   ├── CMakeLists.txt       # Configuración de compilación CMake
│   │   │   │   ├── ffmpeg_bridge.h      # Declaración de clases del puente nativo
│   │   │   │   ├── ffmpeg_bridge.cpp    # Implementación del motor de transcodificación C++
│   │   │   │   └── native_audio_engine.cpp # Puntos de entrada JNI hacia Kotlin
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt      # Activity principal con navegación hacia Hub, Convertir, Comprimir, Video, 8D, Unir y Silencios
│   │   │   │   ├── audio/               # Servicios de audio, video, almacenamiento y bridges
│   │   │   │   │   ├── AppStorageManager.kt   # Gestor de carpetas y subcarpetas accesibles en almacenamiento público
│   │   │   │   │   ├── AudioMetadataReader.kt # Extractor de metadatos y duración de audio
│   │   │   │   │   ├── VideoMetadataReader.kt # Analizador de metadatos y miniaturas de video
│   │   │   │   │   ├── VideoAudioExtractor.kt # Motor de demuxing, passthrough y extracción
│   │   │   │   │   ├── SilenceRemoverProcessor.kt # Motor DSP de detección de energía RMS y corte de silencios
│   │   │   │   │   ├── Spatial8DAudioProcessor.kt # Motor de audio espacial 8D y DSP holofónico
│   │   │   │   │   ├── AudioMerger.kt         # Motor de unión acústica de hasta 6 pistas con DSP
│   │   │   │   │   ├── AudioCompressorProcessor.kt # Motor de compresión y reducción de peso en MB
│   │   │   │   │   ├── AudioPlayerManager.kt  # Controlador de MediaPlayer y progreso
│   │   │   │   │   ├── AudioTranscoder.kt     # Orquestador del pipeline de transcodificación
│   │   │   │   │   ├── NativeAudioBridge.kt   # Conexión JNI con librería C++ / FFmpeg
│   │   │   │   │   ├── RustAudioBridge.kt     # Conexión JNI con librería Rust
│   │   │   │   │   └── codec/                 # Submódulo modular de bajo nivel (Códecs y DSP)
│   │   │   │   │       ├── PcmDecoder.kt          # Decodificador universal MediaCodec / MediaExtractor
│   │   │   │   │       ├── PcmDspProcessor.kt     # Procesador DSP: ganancia, mezcla de canales y remuestreo
│   │   │   │   │       ├── WavEncoder.kt          # Generador de cabeceras y flujo RIFF WAVE
│   │   │   │   │       └── MediaCodecEncoder.kt   # Codificador por hardware AAC, FLAC y Opus
│   │   │   │   ├── model/
│   │   │   │   │   ├── AudioModels.kt       # Enums y Data Classes para conversión de audio
│   │   │   │   │   ├── CompressorModels.kt  # Modelos para compresión, perfiles y presets
│   │   │   │   │   ├── VideoModels.kt       # Modelos para análisis y extracción de video
│   │   │   │   │   ├── SilenceModels.kt     # Modelos para eliminación de silencios
│   │   │   │   │   ├── Spatial8DModels.kt   # Modelos para audio 8D (trayectorias, presets, LFO)
│   │   │   │   │   └── MergeModels.kt       # Modelos para unión de pistas, estado y progreso
│   │   │   │   ├── ui/
│   │   │   │   │   ├── MainHubScreen.kt         # Menú principal y explorador de carpetas (Orquestador)
│   │   │   │   │   ├── AudioConverterScreen.kt  # Pantalla de conversión con Tabs y Scaffold (Orquestador)
│   │   │   │   │   ├── AudioCompressorScreen.kt  # Pantalla de compresión con selector de perfiles y presets (Orquestador)
│   │   │   │   │   ├── SilenceRemoverScreen.kt  # Pantalla de eliminación inteligente de silencios (Orquestador)
│   │   │   │   │   ├── VideoToAudioScreen.kt    # Pantalla de extracción de audio desde video
│   │   │   │   │   ├── Spatial8DScreen.kt       # Pantalla de audio 8D espacial e interactiva (Orquestador)
│   │   │   │   │   ├── AudioMergerScreen.kt     # Pantalla de unión de audios (hasta 6 pistas)
│   │   │   │   │   ├── components/              # Componentes modulares Jetpack Compose
│   │   │   │   │   │   ├── AudioPlayerCard.kt
│   │   │   │   │   │   ├── AudioWaveformVisualizer.kt
│   │   │   │   │   │   ├── MorphingConversionAnimation.kt
│   │   │   │   │   │   ├── ConversionProgressDialog.kt
│   │   │   │   │   │   ├── ConvertedFilesList.kt
│   │   │   │   │   │   ├── FormatSelectorSection.kt
│   │   │   │   │   │   ├── QualitySelectorSection.kt
│   │   │   │   │   │   ├── compressor/          # Componentes modulares de Compresión de Audio
│   │   │   │   │   │   │   ├── CompressorSizeComparisonCard.kt # Comparador de tamaño antes/después
│   │   │   │   │   │   │   ├── CompressorPresetsCard.kt        # Presets (WhatsApp, Email, Ahorro, Slider MB)
│   │   │   │   │   │   │   ├── CompressorProfileCard.kt        # Perfiles (Voz, Música, Ahorro Extremo)
│   │   │   │   │   │   │   └── CompressorProgressDialog.kt     # Diálogo de compresión y reproductor
│   │   │   │   │   │   ├── split/               # Componentes modulares de Dividir por Silencios
│   │   │   │   │   │   │   ├── SplitSettingsCard.kt            # Configuración de sensibilidad dB, pausa mínima y ZIP
│   │   │   │   │   │   │   ├── DetectedTracksPreviewCard.kt    # Vista previa de segmentos, checkboxes y preescucha
│   │   │   │   │   │   │   └── SplitProgressDialog.kt          # Diálogo de progreso de análisis, exportación y ZIP
│   │   │   │   │   │   ├── hub/                 # Componentes modulares del Menú Principal
│   │   │   │   │   │   │   ├── HubModels.kt           # Modos de visualización (Grid, Lista, Detallada) y datos
│   │   │   │   │   │   │   ├── HubViewModeSelector.kt # Selector de vista con botones de alternancia
│   │   │   │   │   │   │   ├── HubHeader.kt           # Header principal y Hero Banner
│   │   │   │   │   │   │   ├── ActiveToolCard.kt      # Tarjetas interactivas (Cuadrícula, Fila compacta y Detallada)
│   │   │   │   │   │   │   ├── UpcomingToolCard.kt    # Tarjetas de herramientas del roadmap
│   │   │   │   │   │   │   └── StorageFoldersCard.kt  # Explorador de carpetas y pie de privacidad
│   │   │   │   │   │   ├── silence/             # Componentes modulares de Eliminar Silencios
│   │   │   │   │   │   │   ├── SilenceSettingsCard.kt   # Selector de umbrales dB, pausas mínimas y padding
│   │   │   │   │   │   │   └── SilenceProgressDialog.kt # Diálogo de progreso con métricas de ahorro
│   │   │   │   │   │   ├── converter/           # Componentes modulares de Conversión de Audio
│   │   │   │   │   │   │   ├── ConverterHeader.kt              # Header con Tabs y selector de vistas
│   │   │   │   │   │   │   ├── ConverterSelectFileCard.kt      # Tarjeta de selección y formatos soportados
│   │   │   │   │   │   │   ├── ConverterSelectedAudioCard.kt   # Tarjeta de archivo seleccionado con reproductor
│   │   │   │   │   │   │   └── ConverterCustomFileNameCard.kt  # Campo para nombre de archivo resultante
│   │   │   │   │   │   ├── spatial/             # Componentes modulares de Audio 8D
│   │   │   │   │   │   │   ├── SpatialOrbitalRadar.kt            # Radar visual 360° en tiempo real
│   │   │   │   │   │   │   ├── SpatialAudioSourceCard.kt         # Selector y vista previa de pista origen
│   │   │   │   │   │   │   ├── SpatialTrajectorySettingsCard.kt  # Configuración de trayectoria, velocidad y reverb
│   │   │   │   │   │   │   ├── SpatialExportSettingsCard.kt      # Formato de salida, bitrate y nombre
│   │   │   │   │   │   │   ├── SpatialHistorySection.kt          # Lista e items de audios 8D procesados
│   │   │   │   │   │   │   └── Spatial8DProgressDialog.kt        # Diálogo de procesamiento y reproductor
│   │   │   │   │   │   ├── merge/               # Componentes desacoplados de Unión de Audios
│   │   │   │   │   │   │   ├── MergeTrackCard.kt
│   │   │   │   │   │   │   ├── MergeSummaryCard.kt
│   │   │   │   │   │   │   ├── MergeOptionsSection.kt
│   │   │   │   │   │   │   └── MergeProgressDialog.kt
│   │   │   │   │   │   └── video/               # Componentes desacoplados de Video a Audio
│   │   │   │   │   │       ├── VideoSelectPlaceholder.kt
│   │   │   │   │   │       ├── SelectedVideoCard.kt
│   │   │   │   │   │       ├── ExtractionModeSelector.kt
│   │   │   │   │   │       ├── VideoTargetFormatSection.kt
│   │   │   │   │   │       ├── VideoQualitySettingsSection.kt
│   │   │   │   │   │       ├── CustomFileNameSection.kt
│   │   │   │   │   │       ├── ExtractedFileCard.kt
│   │   │   │   │   │       └── VideoExtractionProgressDialog.kt
│   │   │   │   │   └── theme/           # Paleta de colores M3, tipografía y tema
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   └── viewmodel/
│   │   │   │       ├── AudioConverterViewModel.kt  # Lógica de negocio de convertidor y storage
│   │   │   │       ├── AudioCompressorViewModel.kt  # Lógica de negocio de compresión y storage
│   │   │   │       ├── SilenceRemoverViewModel.kt  # Lógica de negocio de eliminación de silencios
│   │   │   │       ├── VideoToAudioViewModel.kt    # Lógica de negocio de extracción y storage
│   │   │   │       ├── Spatial8DViewModel.kt       # Lógica de negocio y estado de Audio 8D
│   │   │   │       └── AudioMergerViewModel.kt     # Lógica de negocio de unión de audios
│   │   │   └── res/                     # Recursos Android (strings, drawables, file_paths.xml)
│   │   └── test/                        # Pruebas unitarias JVM y Screenshot Tests
│   └── build.gradle.kts                 # Configuración de Gradle, NDK y dependencias
│
├── rust/                                # Módulo nativo Rust
│   ├── Cargo.toml                       # Dependencias de Rust (Symphonia, Rubato, JNI)
│   └── src/
│       ├── lib.rs                       # Entrypoint con métodos JNI exportados
│       ├── converter.rs                 # Orquestador del flujo de conversión Rust
│       └── audio_dsp.rs                 # Procesamiento de señal, DSP 8D y algoritmos de ganancia
│
├── gradle/libs.versions.toml            # Catálogo de versiones centralizado
├── metadata.json                        # Metadatos para AI Studio
├── README.md                            # Documentación general y características
├── ROADMAP.md                           # Hoja de ruta del proyecto
├── STRUCTURE.md                         # Este archivo con el árbol de componentes y almacenamiento
├── AI_CONTEXT.md                        # Contexto para asistentes de inteligencia artificial
├── commit_message.txt                   # Registro de commits en español
└── AGENTS.md                            # Directrices e instrucciones para agentes de desarrollo
```

---

## 📁 Jerarquía de Almacenamiento Accesible

La aplicación gestiona automáticamente las siguientes carpetas en el almacenamiento accesible del dispositivo:

```
📁 AudioConverter/                   <- Carpeta raíz pública de la app (en Música / Almacenamiento Externo Público)
  ├── 📁 Convertir/                  <- Audios procesados por la herramienta de conversión (MP3, WAV, FLAC, etc.)
  ├── 📁 Comprimir/                  <- Audios reducidos y optimizados de tamaño para compartir
  ├── 📁 Dividir/                    <- Pistas y canciones separadas automáticamente por silencios
  ├── 📁 Sin Silencio/               <- Audios con pausas y silencios eliminados / acelerados (Smart Cut)
  ├── 📁 Video a Audio/              <- Pistas de audio extraídas de archivos de video
  ├── 📁 Audio 8D/                   <- Audios con efecto espacial 360°, ITD y acústica binaural
  ├── 📁 Fusionar/                   <- Pistas de audio combinadas y unificadas (hasta 6 pistas)
  ├── 📁 Recortar/                   <- Segmentos recortados y tonos de llamada (próxima herramienta)
  └── 📁 Grabaciones/                <- Muestras de audio y grabaciones locales
```
