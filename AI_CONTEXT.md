# Contexto para Asistentes de IA (AI Context)

Este archivo proporciona contexto técnico integral sobre el propósito, la arquitectura y las directrices de codificación de **Audio Converter** para cualquier modelo o agente de IA que trabaje sobre este repositorio.

---

## 🎯 Propósito del Proyecto
Construir una aplicación móvil de conversión de audio para Android que combine la flexibilidad de **Jetpack Compose** en la interfaz con la potencia de bajo nivel de **C++ (FFmpeg)** y **Rust (DSP / Symphonia)** en el procesamiento de señales de audio.

---

## 🧱 Pilares de la Arquitectura

1. **Capa de Presentación (UI)**:
   - Construida exclusivamente con Jetpack Compose y Material Design 3.
   - Navegación desacoplada entre el Menú Principal (`MainHubScreen.kt`) con el catálogo de herramientas y las pantallas individuales de cada función (`AudioConverterScreen.kt`, `VideoToAudioScreen.kt`).
   - Estado gestionado mediante `ViewModel` y `StateFlow`.
   - Se utilizan `testTag` con formato `snake_case` en todos los componentes interactivos clave.

2. **Capa de Transcodificación y Motores Nativos**:
   - `AudioTranscoder.kt`: Controla el flujo de decodificación a buffers PCM lineales y posterior empaquetado y codificación al contenedor de destino, aplicando restricciones de bitrate y muestreo para evitar sobremuestreo artificial.
   - `VideoAudioExtractor.kt`: Módulo de demuxing para aislar y extraer pistas de audio desde contenedores de video (MP4, MKV, WebM, MOV, AVI) con soporte para copia de stream directa o recodificación acústica.
   - `NativeAudioBridge.kt` / `app/src/main/cpp/`: Puente JNI hacia C++ y FFmpeg Core configurado mediante `CMakeLists.txt` en `app/build.gradle.kts`.
   - `RustAudioBridge.kt` / `rust/`: Módulo en Rust para procesamiento DSP y algoritmos de remuestreo de audio de alta fidelidad.

3. **Capa de Gestión de Archivos y Metadatos**:
   - `AudioMetadataReader.kt` & `VideoMetadataReader.kt`: Extracción robusta de metadatos de audio y video (formato, bitrate, sample rate, resolución, fotograma miniatura, duración) para acotar las opciones de exportación al máximo físico soportado por el archivo original.
   - Lectura de URIs mediante `ContentResolver` y almacenamiento de archivos de salida en el directorio de caché/archivos de la app con opción de exportación a `Environment.DIRECTORY_MUSIC`.

4. **Reglas de Calidad y Experiencia de Usuario**:
   - Bloqueo preventivo de bitrates y sample rates mayores al origen (Anti-bloat).
   - Aviso visual y educativo en la interfaz cuando el usuario selecciona formatos sin pérdida (WAV, FLAC, AIFF) a partir de audios de entrada ya comprimidos (MP3, AAC, OGG, etc.).

---

## ⚠️ Reglas y Directrices Críticas

- **Idioma**: Toda la interfaz de usuario, cadenas en `strings.xml`, mensajes de commit y documentación deben mantenerse en **Español**.
- **Distribución**: El APK está concebido para distribución directa o plataformas de terceros (como Uptodown); no se deben imponer limitaciones artificiales de peso del APK a expensas de la funcionalidad.
- **Inclusión Nativa**: Las configuraciones de C++ / NDK y Rust deben estar siempre contempladas en los scripts de compilación de Gradle sin omitirlas.
- **Nombres de Marcas**: Evitar el uso en nombres de paquetes o archivos de marcas registradas que puedan ocasionar problemas de derechos de autor.
- **Entorno del Usuario**: El usuario interactúa desde un dispositivo móvil; el código generado debe ser robusto y estar completamente verificado mediante `compile_applet`.
