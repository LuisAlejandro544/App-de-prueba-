# Directrices para Agentes de Desarrollo (AGENTS.md)

Este archivo define las reglas de comportamiento, estándares de código y prioridades que deben seguir los agentes de IA que colaboren en este repositorio.

---

## 📋 Reglas Obligatorias de Operación

1. **Razonamiento Previo Obligatorio**:
   - Antes de realizar cualquier cambio o ejecutar herramientas de edición, se debe razonar cuidadosamente qué herramientas se utilizarán, qué cambios se introducirán y cómo afectará a la compilación.

2. **Idioma del Proyecto**:
   - Todo el contenido generado para el usuario (mensajes, textos de la UI, comentarios relevantes de documentación y cualquier archivo de commits) debe estar redactado en **Español**.

3. **Inclusión de Módulos C++ y Rust**:
   - Si se añade código en C++, Rust o dependencias nativas, deben mantenerse siempre enlazados e incluidos en la configuración de Gradle (`build.gradle.kts` y `CMakeLists.txt`), asegurando que compilen correctamente.
   - No se deben sustituir funcionalidades nativas solicitadas por alternativas incompletas de Kotlin si la funcionalidad fue diseñada para ejecutarse en el motor nativo.

4. **Tamaño del APK y Uso de Dependencias**:
   - No limitar la funcionalidad por el peso final del archivo APK. Se deben priorizar dependencias 100% funcionales y completas sobre soluciones sin dependencias que carezcan de capacidades reales.

5. **Protección de Propiedad Intelectual**:
   - Evitar el uso en nombres de archivos o paquetes de marcas protegidas por derechos de autor que puedan comprometer la seguridad legal de la aplicación.

6. **Target de Distribución**:
   - La aplicación está orientada a distribución en plataformas de terceros (como Uptodown o distribución directa de APK), por lo que debe funcionar de manera autónoma sin servicios cautivos de tiendas.

7. **Prohibición en Comandos de Sistema**:
   - En caso de implementar utilidades de optimización del dispositivo, nunca utilizar `persist.sys.*`.

---

## 🛠️ Buenas Prácticas de Código

- **Jetpack Compose**: Mantener componentes desacoplados, utilizar `Modifier.testTag("...")` en elementos interactivos y respetar la escala de espaciado de Material 3.
- **Manejo de Errores**: Todo proceso de transcodificación de audio debe gestionar de forma controlada excepciones como archivos corruptos, falta de espacio en disco o cancelaciones del usuario.
- **Verificación**: Siempre verificar los cambios ejecutando la compilación del proyecto para asegurar que el código compile de forma exitosa.
