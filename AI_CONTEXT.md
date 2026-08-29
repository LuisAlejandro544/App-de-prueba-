# Contexto para Asistentes de IA (AI Context)

Este archivo proporciona contexto técnico integral sobre el propósito, la arquitectura y las directrices de codificación de **Audio Converter** para cualquier modelo o agente de IA que trabaje sobre este repositorio.

---

## 🎯 Propósito del Proyecto
Construir una aplicación móvil de conversión y procesamiento de audio para Android que combine la flexibilidad de **Jetpack Compose** en la interfaz con la potencia de bajo nivel de **C++ (NDK / CMake)** y **Rust (DSP / Symphonia)** en el procesamiento de señales de audio, con almacenamiento 100% accesible y organizado para el usuario.

---

## 🧱 Pilares de la Arquitectura

1. **Capa de Presentación (UI)**:
   - Construida exclusivamente con Jetpack Compose y Material Design 3.
   - Menú Principal (`MainHubScreen.kt`) con el catálogo de herramientas y explorador de carpetas.
   - Pantallas dedicadas:
     - `AudioConverterScreen.kt`: Conversión multiformato con protección de fidelidad.
     - `VideoToAudioScreen.kt`: Extracción y transcodificación de audio de videos.
     - `Spatial8DScreen.kt`: Creación de Audio 8D Espacial y Holofónico con radar orbital 360°.
     - `AudioMergerScreen.kt`: Unión y concatenación de hasta 6 pistas con normalización DSP y micro-fundido.
   - Estado reactivo mediante `ViewModel` y `StateFlow`.
   - Se utilizan `testTag` con formato `snake_case` en todos los componentes interactivos.

2. **Capa de Gestión de Almacenamiento y Carpetas Accesibles (`AppStorageManager.kt`)**:
   - Estructura de carpetas en el almacenamiento accesible del dispositivo (público en `Environment.DIRECTORY_MUSIC` bajo `Música/AudioConverter` para compatibilidad total con Android 11+ y exploradores de archivos externos):
     - Raíz pública: `AudioConverter`
     - Subcarpeta `Convertir`: Audios convertidos entre formatos.
     - Subcarpeta `Video a Audio`: Pistas de audio extraídas de videos.
     - Subcarpeta `Audio 8D`: Audios espaciales y binaurales 360°.
     - Subcarpeta `Fusionar`: Pistas de audio combinadas y unificadas.
     - Subcarpetas preparadas: `Recortar`, `Grabaciones`.
   - Compatibilidad con **Scoped Storage** y registro inmediato en `MediaStore API`.
   - Apertura directa de carpetas en el explorador de archivos del sistema mediante `FileProvider`.

3. **Capa de Motores de Audio y Procesamiento DSP**:
   - `AudioTranscoder.kt`: Controla el flujo de decodificación a buffers PCM lineales y codificación final.
   - `VideoAudioExtractor.kt`: Módulo de demuxing para aislar y extraer audio desde contenedores de video.
   - `Spatial8DAudioProcessor.kt`: Motor de audio 8D que aplica paneo orbital con LFO, cálculo de retardo temporal interaural (ITD), efecto de sombra craneal y reverberación espacial Schroeder.
   - `AudioMerger.kt`: Motor de unión de hasta 6 archivos de audio. Decodifica a PCM, normaliza sample rates y canales mediante DSP (C++/Rust/Kotlin), aplica micro-fundido suave (15ms anti-clic) y empaqueta en el formato de salida elegido.
   - `NativeAudioBridge.kt` / `app/src/main/cpp/`: Puente JNI hacia C++ configurado mediante `CMakeLists.txt`.
   - `RustAudioBridge.kt` / `rust/`: Módulo en Rust para procesamiento DSP y algoritmos de remuestreo audiófilo.

4. **Capa de Metadatos y Reglas Acústicas**:
   - `AudioMetadataReader.kt` & `VideoMetadataReader.kt`: Extracción robusta de metadatos de audio y video.
   - Bloqueo preventivo de bitrates y sample rates mayores al origen (Anti-bloat).
   - Aviso pedagógico al elegir formatos sin pérdida sobre fuentes comprimidas.

---

## ⚠️ Reglas y Directrices Críticas

- **Idioma**: Toda la interfaz de usuario, cadenas en `strings.xml`, mensajes de commit y documentación deben mantenerse en **Español**.
- **Distribución**: El APK está concebido para distribución directa o plataformas de terceros (como Uptodown); priorizar dependencias 100% funcionales.
- **Inclusión Nativa**: Las configuraciones de C++ / NDK y Rust deben estar siempre compiladas y empaquetadas sin omitirse.
- **Nombres de Marcas**: Evitar el uso en nombres de paquetes o archivos de marcas registradas.
