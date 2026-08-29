package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Gestor centralizado del almacenamiento accesible para el usuario.
 *
 * Estructura de carpetas en el almacenamiento:
 * 📁 AudioConverter (Carpeta principal con el nombre provisional de la app)
 *   ├── 📁 Convertir (Audios convertidos entre diferentes formatos)
 *   ├── 📁 Video a Audio (Audios extraídos de videos MP4/MKV/WebM)
 *   ├── 📁 Recortar (Segmentos de audio cortados)
 *   ├── 📁 Fusionar (Audios unidos / mezclados)
 *   └── 📁 Grabaciones (Notas de voz y muestras)
 */
enum class AppAudioFolder(val folderName: String, val displayName: String, val description: String) {
  CONVERTIR("Convertir", "Convertir", "Audios convertidos a otros formatos (MP3, WAV, FLAC, etc.)"),
  VIDEO_A_AUDIO("Video a Audio", "Video a Audio", "Pistas de audio extraídas de videos"),
  AUDIO_8D("Audio 8D", "Audio 8D", "Audios espaciales 3D / 8D inmersivos con rotación binaural"),
  FUSIONAR("Fusionar", "Fusionar", "Pistas de audio combinadas"),
  RECORTAR("Recortar", "Recortar", "Segmentos recortados y tonos de llamada"),
  GRABACIONES("Grabaciones", "Grabaciones", "Audios grabados y muestras de prueba")
}

object AppStorageManager {

  const val APP_ROOT_FOLDER_NAME = "AudioConverter"

  /**
   * Retorna el directorio público primario en el almacenamiento accesible (Música/AudioConverter),
   * visible directamente en exploradores de archivos como Google Files, Solid Explorer o conexión USB a PC.
   */
  fun getBaseAppStorageDirectory(context: Context): File {
    // 1. Carpeta pública de Música (Estándar Android para archivos de audio accesibles al usuario)
    val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    if (publicMusicDir != null) {
      val appMusicDir = File(publicMusicDir, APP_ROOT_FOLDER_NAME)
      if (appMusicDir.exists() || appMusicDir.mkdirs()) {
        return appMusicDir
      }
    }

    // 2. Directorio de la app en caso de restricciones de permisos del sistema
    val externalDir = context.getExternalFilesDir(null)
    val fallbackDir = if (externalDir != null) {
      File(externalDir, APP_ROOT_FOLDER_NAME)
    } else {
      File(context.filesDir, APP_ROOT_FOLDER_NAME)
    }
    if (!fallbackDir.exists()) {
      fallbackDir.mkdirs()
    }
    return fallbackDir
  }

  /**
   * Crea e inicializa toda la jerarquía de subcarpetas en el almacenamiento accesible del dispositivo.
   */
  fun ensureAllFoldersExist(context: Context) {
    try {
      val baseAppDir = getBaseAppStorageDirectory(context)
      for (folder in AppAudioFolder.values()) {
        val subDir = File(baseAppDir, folder.folderName)
        if (!subDir.exists()) {
          subDir.mkdirs()
        }
      }

      // También asegurar en almacenamiento externo directo si está disponible
      val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
      if (musicDir != null) {
        val publicAppDir = File(musicDir, APP_ROOT_FOLDER_NAME)
        publicAppDir.mkdirs()
        for (folder in AppAudioFolder.values()) {
          val subDir = File(publicAppDir, folder.folderName)
          if (!subDir.exists()) {
            subDir.mkdirs()
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Retorna la subcarpeta específica para el tipo de audio indicado (asegurando su creación en almacenamiento público).
   */
  fun getFolder(context: Context, folderType: AppAudioFolder): File {
    val baseDir = getBaseAppStorageDirectory(context)
    val targetFolder = File(baseDir, folderType.folderName)
    if (!targetFolder.exists()) {
      targetFolder.mkdirs()
    }
    return targetFolder
  }

  /**
   * Obtiene todos los archivos existentes en la subcarpeta, buscando tanto en la ubicación pública
   * como en ubicaciones de fallback previas para no perder ningún archivo convertido anteriormente.
   */
  fun listFilesForFolder(context: Context, folderType: AppAudioFolder): List<File> {
    val result = mutableListOf<File>()
    val seenPaths = mutableSetOf<String>()

    // 1. Buscar en directorio público primario
    try {
      val primaryFolder = getFolder(context, folderType)
      primaryFolder.listFiles()?.filter { it.isFile && it.length() > 0 }?.forEach {
        if (seenPaths.add(it.name)) {
          result.add(it)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    // 2. Buscar en carpeta pública directa de Música (por si acaso difiere)
    try {
      val musicFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "$APP_ROOT_FOLDER_NAME/${folderType.folderName}")
      if (musicFolder.exists()) {
        musicFolder.listFiles()?.filter { it.isFile && it.length() > 0 }?.forEach {
          if (seenPaths.add(it.name)) {
            result.add(it)
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    // 3. Buscar en directorio de app de versiones previas para retrocompatibilidad
    try {
      val externalDir = context.getExternalFilesDir(null)
      if (externalDir != null) {
        val legacyFolder = File(externalDir, "$APP_ROOT_FOLDER_NAME/${folderType.folderName}")
        if (legacyFolder.exists()) {
          legacyFolder.listFiles()?.filter { it.isFile && it.length() > 0 }?.forEach {
            if (seenPaths.add(it.name)) {
              result.add(it)
            }
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    return result.sortedByDescending { it.lastModified() }
  }

  /**
   * Retorna una ruta amigable y descriptiva para mostrar en la interfaz de usuario.
   */
  fun getDisplayPath(context: Context, folderType: AppAudioFolder): String {
    return "Almacenamiento / Música / $APP_ROOT_FOLDER_NAME / ${folderType.folderName}"
  }

  /**
   * Copia e indexa el archivo de audio en la biblioteca pública de Música bajo la subcarpeta correspondiente,
   * haciéndolo visible inmediatamente en exploradores de archivos y reproductores de música.
   */
  fun exportToPublicMusicFolder(
    context: Context,
    sourceFile: File,
    mimeType: String,
    folderType: AppAudioFolder,
    customDisplayName: String? = null
  ): Boolean {
    val displayName = customDisplayName ?: sourceFile.name
    val relativePath = "${Environment.DIRECTORY_MUSIC}/$APP_ROOT_FOLDER_NAME/${folderType.folderName}"

    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
          put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
          put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
          put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
          put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(sourceFile).use { input ->
              input.copyTo(out)
            }
          }
          values.clear()
          values.put(MediaStore.Audio.Media.IS_PENDING, 0)
          context.contentResolver.update(uri, values, null, null)
          true
        } else {
          false
        }
      } else {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val targetFolder = File(musicDir, "$APP_ROOT_FOLDER_NAME/${folderType.folderName}").apply { mkdirs() }
        val destFile = File(targetFolder, displayName)
        FileInputStream(sourceFile).use { input ->
          FileOutputStream(destFile).use { out ->
            input.copyTo(out)
          }
        }
        true
      }
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Abre la subcarpeta en el explorador de archivos del sistema o muestra un selector.
   */
  fun openFolderInFileManager(context: Context, folderType: AppAudioFolder) {
    try {
      val folder = getFolder(context, folderType)
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        folder
      )

      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "resource/folder")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      context.startActivity(Intent.createChooser(intent, "Abrir carpeta ${folderType.displayName}"))
    } catch (e: Exception) {
      try {
        // Intento de fallback abriendo el selector de archivos del sistema
        val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
          type = "audio/*"
          addCategory(Intent.CATEGORY_OPENABLE)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(fallbackIntent, "Explorar audios"))
      } catch (e2: Exception) {
        Toast.makeText(context, "Ruta: ${getDisplayPath(context, folderType)}", Toast.LENGTH_LONG).show()
      }
    }
  }
}
