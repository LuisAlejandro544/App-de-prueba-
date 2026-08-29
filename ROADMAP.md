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
- [x] Integración de llamadas nativas C++ y compilación obligatoria de Rust (`cargo-ndk`) para 4 arquitecturas ABI.
- [x] **Sistema de Carpetas Accesibles en Almacenamiento**:
  - Estructura automática `AudioConverter/` con subcarpetas `Convertir/`, `Video a Audio/`, `Fusionar/`, `Recortar/`, `Grabaciones/`.
  - Integración transparente con `MediaStore` (Scoped Storage) para visibilidad inmediata en exploradores de archivos y reproductores.
  - Accesos directos y botones de exploración de carpetas desde la interfaz.

---

## 📍 Fase 3: Unión Acústica, Compresión Inteligente, Audio 8D y Herramientas Avanzadas (En Progreso 🚧)
- [x] **Herramienta: Dividir por Silencios (Smart Audio Splitter / Auto-Chunker)**:
  - Detección automática de cortes de pistas a partir del análisis espectral de energía RMS (dB).
  - Umbral de decibelios y tiempo de pausa mínimo configurables para música, conciertos o grabaciones académicas.
  - Previsualización interactiva con reproductor de muestra por segmento, selección individual y renombrado de pistas.
  - Exportación individual de pistas a `Música/AudioConverter/Dividir/` y empaquetado opcional en archivo ZIP.
- [x] **Herramienta: Comprimir Audio (Smart Size Shrinker)**:
  - Estimación en tiempo real del tamaño de salida en MB y porcentaje de ahorro previo a la compresión.
  - Ajustes de compresión para WhatsApp/Discord (<16MB), Email (<25MB), Ahorro Extremo, Equilibrado y Tamaño Específico en MB.
  - Perfiles acústicos inteligentes (Voz/Podcasts, Música/Canciones, Máximo Ahorro) con remuestreo y control de canales.
  - Guardado directo en la subcarpeta pública `Música/AudioConverter/Comprimir/`.
- [x] **Herramienta: Unir Audios / Fusión de Pistas (Hasta 6 Pistas)**:
  - Concatenación interactiva con reordenación de pistas y preescucha individual.
  - Normalización DSP y remuestreo lineal ante diferencias de sample rate o canales.
  - Micro-fundido suave (15ms crossfade anti-clics) en los puntos de empalme.
  - Exportación automática a `Música/AudioConverter/Fusionar/` con registro en MediaStore.
- [x] **Herramienta: Audio 8D Espacial (Holofónico 360° Binaural)**:
  - Paneo orbital continuo con modulación LFO y trayectorias seleccionables (Circular 360°, Péndulo Infinito en 8, Expansión 3D).
  - Simulación acústica binaural con cálculo de retardo interaural (ITD), efecto de sombra craneal (filtro paso bajo dependiente de azimut) y reverberación Schroeder.
  - Radar orbital 360° interactivo para previsualizar visualmente la órbita del sonido alrededor de la cabeza.
  - Aceleración nativa en C++ y Rust para procesamiento ultrarrápido sin sobrecalentamiento.
  - Guardado directo en almacenamiento público visible (`Música/AudioConverter/Audio 8D/`).
- [ ] Recortador visual de audio con selección de puntos de inicio y fin sobre la onda sonora y creador de tonos (carpeta `Recortar/`).
- [ ] Ecualizador gráfico paramétrico de 10 bandas y refuerzo de graves (*Bass Boost*).
- [ ] Editor de etiquetas ID3 / Metadatos (Título, Artista, Álbum, Año, Portada del álbum embebida).
- [ ] Conversión en segundo plano mediante `WorkManager` y notificación persistente en la barra de estado.

---

## 📍 Fase 4: Optimización y Distribución Externa 📲
- [x] Pipeline CI automatizado con GitHub Actions para compilación obligatoria de Rust (`cargo-ndk`) y C++ (`CMake`) con verificación estricta de librerías nativas `.so`.
- [ ] Preparación y firma de APK optimizado para distribución directa en plataformas de terceros (Uptodown, descarga directa APK).
- [ ] Modo oscuro / claro adaptativo automático según la configuración del sistema.
