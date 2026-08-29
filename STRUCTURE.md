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
│   │   │   │   ├── MainActivity.kt      # Activity principal con navegación hacia Hub, Convertir, Video y Unir
│   │   │   │   ├── audio/               # Servicios de audio, video, almacenamiento y bridges
│   │   │   │   │   ├── AppStorageManager.kt   # Gestor de carpetas y subcarpetas accesibles en almacenamiento público
│   │   │   │   │   ├── AudioMetadataReader.kt # Extractor de metadatos y duración de audio
│   │   │   │   │   ├── VideoMetadataReader.kt # Analizador de metadatos y miniaturas de video
│   │   │   │   │   ├── VideoAudioExtractor.kt # Motor de demuxing, passthrough y extracción
│   │   │   │   │   ├── Spatial8DAudioProcessor.kt # Motor de audio espacial 8D y DSP holofónico
│   │   │   │   │   ├── AudioMerger.kt         # Motor de unión acústica de hasta 6 pistas con DSP
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
│   │   │   │   │   ├── AudioModels.kt   # Enums y Data Classes para conversión de audio
│   │   │   │   │   ├── VideoModels.kt   # Modelos para análisis y extracción de video
│   │   │   │   │   ├── Spatial8DModels.kt # Modelos para audio 8D (trayectorias, presets, LFO)
│   │   │   │   │   └── MergeModels.kt   # Modelos para unión de pistas, estado y progreso
│   │   │   │   ├── ui/
│   │   │   │   │   ├── MainHubScreen.kt         # Menú principal y explorador de carpetas
│   │   │   │   │   ├── AudioConverterScreen.kt  # Pantalla de conversión con Tabs y Scaffold
│   │   │   │   │   ├── VideoToAudioScreen.kt    # Pantalla de extracción de audio desde video
│   │   │   │   │   ├── Spatial8DScreen.kt       # Pantalla de audio 8D espacial e interactiva
│   │   │   │   │   ├── AudioMergerScreen.kt     # Pantalla de unión de audios (hasta 6 pistas)
│   │   │   │   │   ├── components/              # Componentes modulares Jetpack Compose
│   │   │   │   │   │   ├── AudioPlayerCard.kt
│   │   │   │   │   │   ├── AudioWaveformVisualizer.kt
│   │   │   │   │   │   ├── MorphingConversionAnimation.kt
│   │   │   │   │   │   ├── ConversionProgressDialog.kt
│   │   │   │   │   │   ├── ConvertedFilesList.kt
│   │   │   │   │   │   ├── FormatSelectorSection.kt
│   │   │   │   │   │   ├── QualitySelectorSection.kt
│   │   │   │   │   │   ├── spatial/             # Componentes modulares de Audio 8D
│   │   │   │   │   │   │   ├── SpatialOrbitalRadar.kt    # Radar visual 360° en tiempo real
│   │   │   │   │   │   │   └── Spatial8DProgressDialog.kt# Diálogo de procesamiento y reproductor
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
│   │   │   │       ├── AudioConverterViewModel.kt # Lógica de negocio de convertidor y storage
│   │   │   │       ├── VideoToAudioViewModel.kt   # Lógica de negocio de extracción y storage
│   │   │   │       ├── Spatial8DViewModel.kt      # Lógica de negocio y estado de Audio 8D
│   │   │   │       └── AudioMergerViewModel.kt    # Lógica de negocio de unión de audios
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
├── commit_message.txt                   # Registro del último commit en español
└── AGENTS.md                            # Directrices e instrucciones para agentes de desarrollo
```

---

## 📁 Jerarquía de Almacenamiento Accesible

La aplicación gestiona automáticamente las siguientes carpetas en el almacenamiento accesible del dispositivo:

```
📁 AudioConverter/                   <- Carpeta raíz pública de la app (en Música / Almacenamiento Externo Público)
  ├── 📁 Convertir/                  <- Audios procesados por la herramienta de conversión (MP3, WAV, FLAC, etc.)
  ├── 📁 Video a Audio/              <- Pistas de audio extraídas de archivos de video
  ├── 📁 Audio 8D/                   <- Audios con efecto espacial 360°, ITD y acústica binaural
  ├── 📁 Fusionar/                   <- Pistas de audio combinadas y unificadas (hasta 6 pistas)
  ├── 📁 Recortar/                   <- Segmentos recortados y tonos de llamada (próxima herramienta)
  └── 📁 Grabaciones/                <- Muestras de audio y grabaciones locales
```
