# Audio Converter - Aplicación Móvil Android

Audio Converter es una aplicación móvil nativa de conversión y procesamiento de audio de alto rendimiento para Android, construida con **Jetpack Compose**, **Kotlin**, **C++ (NDK / CMake)** y **Rust**. Diseñada para ofrecer una experiencia clara, fluida y con procesamiento 100% en el dispositivo sin depender de servidores externos.

---

## 🚀 Características Principales

- **Menú Principal y Centro de Herramientas (Audio Hub)**:
  - Pantalla principal dedicada que centraliza todas las utilidades del laboratorio de audio con navegación fluida y soporte para botón atrás nativo.
  - Tarjetas detalladas de cada herramienta con título, descripción, insignias de estado y accesos directos.
  - Tarjeta de acceso directo y gestión de **Carpetas en tu Almacenamiento**.

- **Sistema de Carpetas Accesibles para el Usuario (Almacenamiento Organizado)**:
  - Organización automática de todos los archivos generados en una estructura de carpetas visibles y accesibles desde cualquier gestor de archivos (Google Files, Xiaomi, Samsung, Solid Explorer, etc.):
    ```
    📁 Almacenamiento / Música / AudioConverter (Carpeta principal con el nombre provisional de la app)
      ├── 📁 Convertir         -> Audios convertidos entre formatos (MP3, WAV, FLAC, M4A, etc.)
      ├── 📁 Video a Audio     -> Pistas de audio extraídas de videos (MP4, MKV, WebM, etc.)
      ├── 📁 Recortar          -> Segmentos recortados y tonos de llamada
      ├── 📁 Fusionar          -> Pistas de audio combinadas
      └── 📁 Grabaciones       -> Notas de voz y muestras
    ```
  - **Indexación Inmediata en MediaStore**: Los audios exportados aparecen al instante en los reproductores de música y galerías del sistema.
  - **Botón de Exploración Rápida**: Acceso directo desde la interfaz de la app para abrir la carpeta correspondiente en el gestor de archivos.

- **Herramienta "Convertir" (12 formatos soportados)**:
  - **MP3**: Audio universal con compatibilidad total.
  - **M4A / AAC**: Alta fidelidad y compresión eficiente.
  - **WAV**: Audio PCM puro sin compresión (16-bit).
  - **FLAC**: Compresión de calidad de estudio sin pérdida (Lossless).
  - **OGG**: Contenedor Vorbis para streaming y ligereza.
  - **OPUS**: Códec moderno de ultra baja latencia y alta compresión de voz.
  - **WMA**: Audio estándar del ecosistema Windows.
  - **AIFF**: Formato PCM sin compresión para ecosistema Apple y estaciones DAW.
  - **AMR**: Compresión extrema para notas de voz telefónicas.
  - **M4R**: Tonos de llamada (Ringtone) para smartphones.
  - **AC3**: Formato envolvente para cine y multimedia.
  - **MP2**: Formato clásico de radiodifusión digital.

- **Herramienta "Extraer Audio de Video" (MP4, MKV, WebM, MOV, AVI a MP3/AAC/WAV/FLAC)**:
  - **Demultiplexado Nativo y Rápido**: Aísla la pista de audio de contenedores de video sin procesar innecesariamente los fotogramas de video.
  - **Modo Extracción Directa (Ultra Rápida / Sin Pérdida)**: Copia el flujo de audio original en milisegundos sin recodificación si el contenedor y destino lo permiten.
  - **Modo Conversión de Alta Fidelidad**: Transcodifica el audio del video a formatos populares (MP3, AAC, FLAC, WAV, OGG, OPUS) con control de bitrate (hasta 320 kbps) y ajuste de volumen.
  - **Detección Automática de Metadatos de Video**: Análisis inmediato de resolución (1080p, 4K, 720p), miniatura gráfica de fotograma, duración y códec de audio interno.
  - **Guardado Automático**: Almacenamiento directo en la subcarpeta `Video a Audio` y exportación a la biblioteca pública.

- **Protección de Fidelidad y Bloqueo Anti-Sobremuestreo (Anti-Bloat)**:
  - **Detección automática de metadatos**: Al cargar un archivo, se lee su tasa de bits real y frecuencia de muestreo de origen.
  - **Bloqueo preventivo de valores inflados**: Impide seleccionar bitrates o frecuencias mayores al origen, evitando la creación de archivos innecesariamente pesados sin ganancia acústica real.
  - **Aviso pedagógico en formatos sin pérdida (Lossless)**: Alerta contextual al elegir WAV, FLAC o AIFF si el audio de entrada es comprimido (como MP3 o AAC), advirtiendo que no aumentará la calidad y solo multiplicará el espacio ocupado.

- **Perfiles y Ajustes de Audio**:
  - Perfiles rápidos: *Original*, *Ultra (320 kbps)*, *Alta (256 kbps)*, *Estándar (192 kbps)*, *Económico (128 kbps)* y *Voz (64 kbps)* con bloqueo dinámico de perfiles excedentes.
  - Frecuencias de muestreo personalizables (48 kHz, 44.1 kHz, 32 kHz, 22.05 kHz) delimitadas al máximo del archivo de entrada.
  - Selector de canales: Estéreo, Mono o conservación de canal original.
  - Ganancia / Amplificador de volumen (desde 50% hasta 200%).

- **Reproductor Integrado y Visualizador**:
  - Mini reproductor con forma de onda de audio animada.
  - Soporte para escuchar tanto el audio de origen como el archivo convertido final.

- **Gestor de Audios y Exportación**:
  - Historial de archivos convertidos clasificados por herramienta.
  - Opción directa para compartir mediante cualquier aplicación instalada.
  - Exportación con un toque a la carpeta pública de Música del dispositivo con rutas relativas limpias.

---

## 🛠️ Stack Tecnológico

- **UI & Framework**: Kotlin + Jetpack Compose + Material Design 3.
- **Gestión de Almacenamiento**: `AppStorageManager` + `MediaStore API (Scoped Storage)` + `FileProvider`.
- **Motor Nativo C++**: Android NDK + CMake (`libnative_audio_engine.so`) con puente JNI.
- **Motor Rust DSP**: `audio_converter_core` con soporte para decodificación y resampling audiófilo.
- **Arquitectura**: MVVM (Model-View-ViewModel) + StateFlow reactivo.

---

## 📦 Compilación y Generación del APK

### Opción 1: Compilación Automática en la Nube con GitHub Actions (Recomendado para Móviles)

El repositorio incluye un flujo de trabajo de GitHub Actions (`.github/workflows/build-debug-apk.yml`) configurado para compilar el APK Debug completo con soporte para C++, Rust y firma integrada.

**Pasos para activarlo desde el teléfono (GitHub Web / App):**
1. Entra a tu repositorio en GitHub.
2. Ve a la pestaña **Actions**.
3. En la barra lateral izquierda, selecciona el flujo **Compilar APK Debug (Manual)**.
4. Toca el botón **Run workflow** (Ejecutar flujo de trabajo) y confirma.
5. El sistema descargará el código, instalará Android NDK, CMake, Rust toolchain con `cargo-ndk`, generará la keystore de firma y compilará el archivo APK.
6. Al finalizar la ejecución (en verde), ve a la sección **Artifacts** y descarga el archivo **AudioStudio-v1.0-Debug-APK** directamente en tu teléfono para instalarlo.

### Opción 2: Compilación Local

Para compilar el proyecto en modo depuración localmente:

```bash
gradle assembleDebug
```

Para ejecutar las pruebas unitarias y de captura de interfaz:

```bash
gradle :app:testDebugUnitTest
```
