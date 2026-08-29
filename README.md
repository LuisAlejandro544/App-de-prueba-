# Audio Converter - Aplicación Móvil Android

Audio Converter es una aplicación móvil nativa de conversión y procesamiento de audio de alto rendimiento para Android, construida con **Jetpack Compose**, **Kotlin**, **C++ (NDK / CMake)** y **Rust**. Diseñada para ofrecer una experiencia clara, fluida y con procesamiento 100% en el dispositivo sin depender de servidores externos.

---

## 🚀 Características Principales

- **Menú Principal y Centro de Herramientas (Audio Hub)**:
  - Pantalla principal dedicada que centraliza todas las utilidades del laboratorio de audio con navegación fluida y soporte para botón atrás nativo.
  - Tarjetas detalladas de cada herramienta con título, descripción, insignias de estado y accesos directos.
  - Tarjeta de acceso directo y gestión de **Carpetas en tu Almacenamiento**.

- **Sistema de Carpetas Accesibles para el Usuario (Almacenamiento Organizado en Música)**:
  - Organización automática de todos los archivos generados en una estructura de carpetas visibles y accesibles desde cualquier gestor de archivos (Google Files, Xiaomi, Samsung, Solid Explorer, etc.) ubicado en el almacenamiento público (`Música / AudioConverter`):
    ```
    📁 Almacenamiento Público / Música / AudioConverter
      ├── 📁 Convertir         -> Audios convertidos entre formatos (MP3, WAV, FLAC, M4A, etc.)
      ├── 📁 Comprimir         -> Audios optimizados y reducidos de tamaño para compartir
      ├── 📁 Dividir           -> Pistas y canciones separadas automáticamente por silencios
      ├── 📁 Sin Silencio      -> Audios con pausas y silencios eliminados / acelerados (Smart Cut)
      ├── 📁 Video a Audio     -> Pistas de audio extraídas de videos (MP4, MKV, WebM, etc.)
      ├── 📁 Audio 8D          -> Audios espaciales y holofónicos 360° con acústica binaural
      ├── 📁 Fusionar          -> Pistas de audio combinadas y unificadas (hasta 6 pistas)
      ├── 📁 Recortar          -> Segmentos recortados y tonos de llamada
      └── 📁 Grabaciones       -> Notas de voz y muestras
    ```
  - **Indexación Inmediata en MediaStore**: Los audios exportados aparecen al instante en los reproductores de música y galerías del sistema sin importar la versión de Android (incluyendo Android 11, 12, 13, 14+).
  - **Botón de Exploración Rápida**: Acceso directo desde la interfaz de la app para abrir la carpeta correspondiente en el gestor de archivos.

- **Herramienta "Dividir por Silencios" (Smart Audio Splitter & Auto-Chunker)**:
  - **Detección Automática de Pistas**: Analiza grabaciones extensas (sesiones en vivo, ensayos musicales, clases universitarias o podcasts) y detecta los límites de cada pista a través de pausas de silencio prolongadas en decibelios RMS.
  - **Configuración de Sensibilidad y Pausa Mínima**:
    - *Sensibilidad*: Sensible (-45 dB), Equilibrado (-35 dB) y Relajado (-25 dB para grabaciones con soplido de fondo).
    - *Pausa Mínima*: 1.0s (canciones rápidas/frases), 2.0s (álbumes de música estándar) o 3.0s (conferencias y entrevistas).
  - **Previsualización Interactiva y Renombrado**: Muestra cada segmento detectado con duración y marcas de tiempo, preescucha en tiempo real, checkbox de selección individual y edición de nombres de pista antes de exportar.
  - **Exportación en Lote y Empaquetado ZIP**: Genera todas las pistas en MP3, M4A/AAC, WAV, FLAC u OGG y ofrece la opción de comprimirlas en un archivo `.zip` para compartirlas fácilmente en un solo paquete.
  - **Almacenamiento Directo**: Guarda en la subcarpeta pública `Música/AudioConverter/Dividir`.

- **Herramienta "Comprimir Audio" (Smart Shrink & Reducción de Tamaño en MB)**:
  - **Estimación Predictiva en Tiempo Real**: Calcula matemáticamente el tamaño final en MB y el porcentaje de ahorro antes de comenzar el proceso.
  - **Ajustes Predefinidos para Redes y Mensajería**:
    - *WhatsApp / Discord*: Comprime garantizando un tamaño menor a 16 MB para envío instantáneo sin errores de límite.
    - *Email / Gmail*: Optimiza para el límite de 25 MB en adjuntos de correo electrónico.
    - *Ahorro Extremo (75-85%)*: Reduce drásticamente a 48 kbps ideal para audios de muy larga duración o clases grabadas.
    - *Equilibrado (50-60%)*: Reducción balanceada a 96 kbps con gran fidelidad acústica.
    - *Ligero (30-40%)*: Bitrate de 128 kbps manteniendo calidad prácticamente transparente.
    - *Tamaño Específico (MB)*: Slider interactivo para ingresar el peso deseado exacto (ej. 10 MB) calculando la tasa de bits requerida automáticamente.
  - **Perfiles Acústicos Inteligentes**:
    - *Voz / Podcasts*: Aplica mezcla mono y filtrado de frecuencias no vocales con remuestreo eficiente logrando compresión máxima.
    - *Música / Canciones*: Preserva la imagen estéreo y rango dinámico con codificación psicoacústica avanzada.
    - *Máximo Ahorro*: Downsampling a 22050 Hz y mono para exprimir el almacenamiento al máximo.
  - **Almacenamiento Directo**: Guarda en la subcarpeta pública `Música/AudioConverter/Comprimir`.

- **Herramienta "Eliminar Silencios" (Smart Silence Remover & Voice Booster)**:
  - **Detección Acústica por Niveles de Energía RMS (dB)**: Analiza el flujo de audio en bloques continuos para identificar con precisión quirúrgica momentos muertos, pausas de respiración y silencios entre oraciones.
  - **Sensibilidad Configurable**: Tres niveles de umbral adaptados a cada tipo de audio:
    - *Suave (-45 dB)*: Elimina únicamente silencios absolutos sin afectar susurros o palabras tenues.
    - *Equilibrado (-35 dB)*: Ideal para notas de voz, conferencias, podcasts y audiolibros.
    - *Agresivo (-25 dB)*: Elimina pausas breves y ruidos estáticos de fondo.
  - **Duración Mínima de Silencio y Margen de Voz (Padding)**: Permite ajustar la pausa mínima requerida para cortar (150 ms a 1200 ms) y añade un margen de seguridad (Padding) de 20 ms a 150 ms para no recortar inicios o finales de palabras.
  - **Modos de Acción Flexibles**:
    - *Eliminar Silencios (100%)*: Suprime por completo los silencios empalmando las secciones de voz.
    - *Acelerar Silencios (4x)*: Comprime las pausas a velocidad cuádruple sin cortar la continuidad del ambiente.
  - **Micro-Fundidos Anti-Chasquidos**: Empalma las secciones con transiciones de audio suaves para evitar artefactos o clics digitales.
  - **Ahorro de Tiempo en Pantalla**: Muestra el porcentaje exacto de tiempo ahorrado (ej. -35% de duración) y el número de pausas cortadas.
  - **Almacenamiento Directo**: Guarda los resultados en la subcarpeta pública `Sin Silencio`.

- **Herramienta "Audio 8D Espacial" (Efecto Holofónico 360° con DSP Binaural)**:
  - **Paneo Orbital Dinámico 360°**: Hace rotar el sonido continuamente alrededor de la cabeza del oyente creando una experiencia inmersiva para auriculares.
  - **Cálculo de Retardo Temporal Interaural (ITD)**: Simula con precisión el tiempo que tarda la onda acústica en llegar de un oído a otro según el ángulo azimutal.
  - **Filtro de Sombra Craneal (Head Shadow Effect)**: Atenúa y filtra las frecuencias agudas simulando la densidad acústica de la cabeza humana cuando la fuente de sonido está en el lado opuesto.
  - **Simulación Acústica de Sala (Reverberación Schroeder)**: Añade profundidad espacial personalizable con presets (Estudio Intimista, Sala de Conciertos, Catedral Espaciosa o Desactivado).
  - **Radar Orbital 360° Interactivo**: Visualizador gráfico en tiempo real que muestra la trayectoria orbital y la posición tridimensional de la fuente de sonido.
  - **Aceleración Nativa Híbrida**: Procesamiento DSP de alto rendimiento acelerado en C++ (NDK) y Rust, con motor de reserva optimizado en Kotlin.
  - **Exportación Flexible**: Salida a MP3, AAC/M4A, FLAC, WAV, OGG y OPUS con bitrates hasta 320 kbps y guardado en la carpeta pública `Audio 8D`.

- **Herramienta "Unir Audios (Fusionar)" (Hasta 6 Pistas de Audio)**:
  - **Concatenación Múltiple**: Permite seleccionar y fusionar de 2 hasta 6 pistas de audio en un solo archivo con orden reordenable interactivamente.
  - **Normalización DSP Inteligente**: Resuelve heterogeneidades de frecuencia de muestreo (Sample Rate de 22.05k, 44.1k, 48k) y canales (Mono/Estéreo) mediante remuestreo acústico y unificación lineal a 2 canales sin desfases ni saltos de tono.
  - **Micro-Fundido Suave Anti-Chasquidos (Crossfade)**: Aplica una interpolación suave de 15ms en los puntos de corte entre pistas para evitar clics y ruidos transitorios digitales.
  - **Formatos de Salida Versátiles**: Exportación directa a MP3, M4A/AAC, WAV, FLAC, OGG y OPUS con selector de calidad y nombre personalizado.
  - **Guardado Automático**: Almacenamiento directo en `Música/AudioConverter/Fusionar` y registro en MediaStore.

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
  - **Detección Automática de Metadatos de Video**: Análisis inmediato de resolución, miniatura gráfica, duración y códec interno.
  - **Guardado Automático**: Almacenamiento directo en la subcarpeta `Video a Audio`.

- **Protección de Fidelidad y Bloqueo Anti-Sobremuestreo (Anti-Bloat)**:
  - **Detección automática de metadatos**: Al cargar un archivo, se lee su tasa de bits real y frecuencia de muestreo de origen.
  - **Bloqueo preventivo de valores inflados**: Impide seleccionar bitrates o frecuencias mayores al origen, evitando la creación de archivos innecesariamente pesados sin ganancia acústica real.
  - **Aviso pedagógico en formatos sin pérdida (Lossless)**: Alerta contextual al elegir WAV, FLAC o AIFF si el audio de entrada es comprimido.

- **Reproductor Integrado y Visualizador**:
  - Mini reproductor con barra de progreso interactiva y control de reproducción.
  - Soporte para escuchar pistas individuales o el resultado final del audio unido.

---

## 🛠️ Stack Tecnológico

- **UI & Framework**: Kotlin + Jetpack Compose + Material Design 3.
- **Gestión de Almacenamiento**: `AppStorageManager` + `MediaStore API (Scoped Storage)` + `FileProvider`.
- **Motor Nativo C++**: Android NDK + CMake (`libnative_audio_engine.so`) con puente JNI.
- **Motor Rust DSP**: `audio_converter_core` compilado obligatoriamente para 4 arquitecturas ABI (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`).
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
5. El sistema descargará el código, compilará Rust con `cargo-ndk`, compilará C++ con CMake, generará la keystore de firma y empaquetará el APK.
6. Al finalizar la ejecución (en verde), ve a la sección **Artifacts** y descarga el archivo **AudioStudio-v1.0-Debug-APK** directamente en tu teléfono para instalarlo.

### Opción 2: Compilación Local

Para compilar el proyecto en modo depuración localmente:

```bash
gradle assembleDebug
```
