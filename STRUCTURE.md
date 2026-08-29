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
│   │   │   │   ├── MainActivity.kt      # Activity principal con inicialización de almacenamiento
│   │   │   │   ├── audio/               # Servicios de audio, video, almacenamiento y bridges
│   │   │   │   │   ├── AppStorageManager.kt   # Gestor de carpetas y subcarpetas accesibles
│   │   │   │   │   ├── AudioMetadataReader.kt # Extractor de metadatos y duración de audio
│   │   │   │   │   ├── VideoMetadataReader.kt # Analizador de metadatos y miniaturas de video
│   │   │   │   │   ├── VideoAudioExtractor.kt # Motor de demuxing, passthrough y extracción
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
│   │   │   │   │   └── VideoModels.kt   # Modelos para análisis y extracción de video
│   │   │   │   ├── ui/
│   │   │   │   │   ├── MainHubScreen.kt         # Menú principal y explorador de carpetas
│   │   │   │   │   ├── AudioConverterScreen.kt  # Pantalla de conversión con Tabs y Scaffold
│   │   │   │   │   ├── VideoToAudioScreen.kt    # Pantalla modular de extracción de audio desde video
│   │   │   │   │   ├── components/              # Componentes modulares Jetpack Compose
│   │   │   │   │   │   ├── AudioPlayerCard.kt
│   │   │   │   │   │   ├── AudioWaveformVisualizer.kt
│   │   │   │   │   │   ├── MorphingConversionAnimation.kt # Animación en bucle de video a audio / audio a audio
│   │   │   │   │   │   ├── ConversionProgressDialog.kt
│   │   │   │   │   │   ├── ConvertedFilesList.kt
│   │   │   │   │   │   ├── FormatSelectorSection.kt
│   │   │   │   │   │   ├── QualitySelectorSection.kt
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
│   │   │   │       └── VideoToAudioViewModel.kt   # Lógica de negocio de extracción y storage
│   │   │   └── res/                     # Recursos Android (strings, drawables, file_paths.xml)
│   │   └── test/                        # Pruebas unitarias JVM y Screenshot Tests
│   └── build.gradle.kts                 # Configuración de Gradle, NDK y dependencias
│
├── rust/                                # Módulo nativo Rust
│   ├── Cargo.toml                       # Dependencias de Rust (Symphonia, Rubato, JNI)
│   └── src/
│       ├── lib.rs                       # Entrypoint con métodos JNI exportados
│       ├── converter.rs                 # Orquestador del flujo de conversión Rust
│       └── audio_dsp.rs                 # Procesamiento de señal y algoritmos de ganancia
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
📁 AudioConverter/                   <- Carpeta raíz provisional de la app (en Música / Almacenamiento Externo)
  ├── 📁 Convertir/                  <- Audios procesados por la herramienta de conversión (MP3, WAV, FLAC, etc.)
  ├── 📁 Video a Audio/              <- Pistas de audio extraídas de archivos de video
  ├── 📁 Recortar/                   <- Segmentos recortados y tonos de llamada (próxima herramienta)
  ├── 📁 Fusionar/                   <- Pistas de audio combinadas (próxima herramienta)
  └── 📁 Grabaciones/                <- Muestras de audio y grabaciones locales
```
