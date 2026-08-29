# Roadmap de Desarrollo - Audio Converter

Este documento describe las fases de evolución técnica y funcional del proyecto **Audio Converter**.

---

## 📍 Fase 1: Fundaciones y Arquitectura (Completada ✅)
- [x] Interfaz moderna basada en Jetpack Compose con soporte completo Material Design 3.
- [x] Menú Principal / Hub centralizado de herramientas de audio con navegación modular.
- [x] Selector de archivos multimedia y generador de audio de muestra para pruebas.
- [x] Soporte para 12 formatos de salida (MP3, WAV, M4A, FLAC, OGG, OPUS, WMA, AIFF, AMR, M4R, AC3, MP2).
- [x] Sistema de auto-detección y bloqueo de sobremuestreo (*Anti-Bloat* / Protección de Fidelidad).
- [x] Banner de advertencia contextual para formatos sin pérdida (*Lossless*) ante fuentes comprimidas.
- [x] Motor de decodificación y transcodificación multinivel en dispositivo.
- [x] Reproductor integrado con visualizador interactivo de forma de onda.
- [x] Gestión de biblioteca de archivos convertidos con opciones de compartir y exportar.
- [x] Configuración de compilación nativa C++ (CMake / NDK) y estructura base Rust.

---

## 📍 Fase 2: Extractor de Video, Motor Nativo y Almacenamiento Organizado (Completada ✅)
- [x] **Herramienta: Extractor de Audio desde Video** (MP4, MKV, WebM, MOV, AVI a MP3/AAC/WAV/FLAC/OGG/OPUS).
- [x] **Modo Extracción Directa (Ultra Rápida / Passthrough)** sin recodificación para contenedores compatibles.
- [x] **Modo Conversión de Alta Fidelidad** con control de bitrate (hasta 320 kbps), calidad y ganancia de volumen.
- [x] Análisis automático de metadatos de video (resolución, duración, miniatura de fotograma, códec de audio interno).
- [x] Integración de llamadas nativas C++ (FFmpeg Core JNI) y Rust Bridge para demuxing y extracción de audio.
- [x] **Sistema de Carpetas Accesibles en Almacenamiento**:
  - Estructura automática `AudioConverter/` con subcarpetas `Convertir/`, `Video a Audio/`, `Recortar/`, `Fusionar/`, `Grabaciones/`.
  - Integración transparente con `MediaStore` (Scoped Storage) para visibilidad inmediata en exploradores de archivos y reproductores.
  - Accesos directos y botones de exploración de carpetas desde la interfaz.

---

## 📍 Fase 3: Edición y Herramientas Acústicas Avanzadas (En Progreso 🚧)
- [ ] Recortador visual de audio con selección de puntos de inicio y fin sobre la onda sonora y creador de tonos (carpeta `Recortar/`).
- [ ] Unión / Fusión de múltiples pistas de audio en un único archivo (carpeta `Fusionar/`).
- [ ] Ecualizador gráfico paramétrico de 10 bandas y refuerzo de graves (*Bass Boost*).
- [ ] Editor de etiquetas ID3 / Metadatos (Título, Artista, Álbum, Año, Portada del álbum embebida).
- [ ] Conversión en segundo plano mediante `WorkManager` y notificación persistente en la barra de estado.

---

## 📍 Fase 4: Optimización y Distribución Externa 📲
- [ ] Optimización de binarios nativos para arquitecturas `arm64-v8a`, `armeabi-v7a` y `x86_64`.
- [ ] Preparación y firma de APK optimizado para distribución directa en plataformas de terceros (Uptodown, descarga directa APK).
- [ ] Modo oscuro / claro adaptativo automático según la configuración del sistema.
