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
│   │   │   │   │   ├── VideoAudioExtractor.kt # Motor de demuxing y extracción de audio de video
│   │   │   │   │   ├── AudioPlayerManager.kt  # Controlador de MediaPlayer y progreso
│   │   │   │   │   ├── AudioTranscoder.kt     # Pipeline de decodificación y transcodificación
│   │   │   │   │   ├── NativeAudioBridge.kt   # Conexión JNI con librería C++ / FFmpeg
│   │   │   │   │   └── RustAudioBridge.kt     # Conexión JNI con librería Rust
│   │   │   │   ├── model/
│   │   │   │   │   ├── AudioModels.kt   # Enums y Data Classes para conversión de audio
│   │   │   │   │   └── VideoModels.kt   # Modelos para análisis y extracción de video
│   │   │   │   ├── ui/
│   │   │   │   │   ├── MainHubScreen.kt         # Menú principal y explorador de carpetas
│   │   │   │   │   ├── AudioConverterScreen.kt  # Pantalla de conversión con Tabs y Scaffold
│   │   │   │   │   ├── VideoToAudioScreen.kt    # Pantalla de extracción de audio desde video
│   │   │   │   │   ├── components/              # Componentes modulares Jetpack Compose
│   │   │   │   │   │   ├── AudioPlayerCard.kt
│   │   │   │   │   │   ├── AudioWaveformVisualizer.kt
│   │   │   │   │   │   ├── ConversionProgressDialog.kt
│   │   │   │   │   │   ├── ConvertedFilesList.kt
│   │   │   │   │   │   ├── FormatSelectorSection.kt
│   │   │   │   │   │   └── QualitySelectorSection.kt
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
