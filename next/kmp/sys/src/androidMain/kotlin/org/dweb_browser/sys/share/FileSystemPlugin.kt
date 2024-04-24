package org.dweb_browser.sys.share

import android.annotation.SuppressLint
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.MimeTypeMap
import org.dweb_browser.helper.getAppContextUnsafe
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

@SuppressLint("StaticFieldLeak")
object FileSystemPlugin {

  private val context = getAppContextUnsafe()

  fun readFile(path: String, eFileType: EFileType?, charset: Charset?): String? {
    return getInputStream(path, eFileType)?.let { inputStream ->
      if (charset != null) {
        readFileAsString(inputStream, charset.name())
      } else {
        readFileAsBase64EncodedData(inputStream)
      }
    }
  }

  fun saveFile(file: File?, data: InputStream, append: Boolean = false) {
    FileOutputStream(file, append).use { outputStream ->
      var read: Int
      val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
      while (data.read(bytes).also { read = it } != -1) {
        outputStream.write(bytes, 0, read)
      }
    }
  }

  fun deleteFile(file: String, eFileType: EFileType?) =
    getFileObject(file, eFileType)?.delete() ?: throw FileNotFoundException("File does not exist")

  fun mkdir(path: String, eFileType: EFileType?, recursive: Boolean): Boolean {
    return getFileObject(path, eFileType)?.let { fileObject ->
      if (recursive) {
        fileObject.mkdirs()
      } else {
        fileObject.mkdir()
      }
    } ?: false
  }

  fun readDir(path: String, eFileType: EFileType?): Array<File>? {
    return getFileObject(path, eFileType)?.listFiles()
  }

  fun copy(
    from: String, fromFileType: EFileType?, to: String, toFileType: EFileType?, doRename: Boolean
  ): File {
    val fromObject = getFileObject(from, fromFileType) ?: throw Exception("source file err!!")
    val toObject = getFileObject(to, toFileType) ?: throw Exception("target file err!!")
    if (toObject == fromObject) {
      return toObject
    }
    if (!fromObject.exists()) {
      throw CopyFailedException("The source object does not exist")
    }
    if (toObject.parentFile?.isFile == true) {
      throw CopyFailedException("The parent object of the destination is a file")
    }
    if (toObject.parentFile?.exists() == false) {
      throw CopyFailedException("The parent object of the destination does not exist")
    }
    if (toObject.isDirectory) {
      throw CopyFailedException("Cannot overwrite a directory")
    }
    toObject.delete()
    if (doRename) {
      val modified = fromObject.renameTo(toObject)
      if (!modified) {
        throw CopyFailedException("Unable to rename, unknown reason")
      }
    } else {
      copyRecursively(fromObject, toObject)
    }
    return toObject
  }

  fun readFileAsString(`is`: InputStream, encoding: String): String {
    val outputStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var length = 0
    while (`is`.read(buffer).also { length = it } != -1) {
      outputStream.write(buffer, 0, length)
    }
    return outputStream.toString(encoding)
  }

  fun readFileAsBase64EncodedData(`is`: InputStream): String {
    val fileInputStreamReader = `is` as FileInputStream
    val byteStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var c: Int
    while (fileInputStreamReader.read(buffer).also { c = it } != -1) {
      byteStream.write(buffer, 0, c)
    }
    fileInputStreamReader.close()
    return Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)
  }

  fun getInputStream(path: String, eFileType: EFileType?): InputStream? {
    eFileType?.let {
      val androidDirectory: File = getDirectory(eFileType)
      return FileInputStream(File(androidDirectory, path))
    } ?: run {
      val u: Uri = Uri.parse(path)
      if (u.scheme.equals("content")) {
        return context.contentResolver?.openInputStream(u)!!
      } else if (u.path != null) {
        return FileInputStream(File(u.path!!))
      }
      return null
    }
  }

  fun getDirectory(eFileType: EFileType): File {
    return when (eFileType) {
      EFileType.Documents -> Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOCUMENTS
      )

      EFileType.Data, EFileType.Library -> context.filesDir
      EFileType.Cache -> context.cacheDir
      EFileType.External -> Environment.getExternalStorageDirectory()
      EFileType.ExternalStorage -> Environment.getExternalStorageDirectory()
    }
  }

  fun getFileObject(path: String, eFileType: EFileType?): File? {
    eFileType?.let {
      val androidDirectory = getDirectory(eFileType)
      if (!androidDirectory.exists()) {
        androidDirectory.mkdir()
      }
      return File(androidDirectory, path)
    } ?: run {
      val u = Uri.parse(path)
      if (u.scheme == null || u.scheme == "file") {
        u.path?.let { return File(it) }
      }
      return null
    }
  }

  fun getEncoding(encoding: String?): Charset? {
    if (encoding == null) {
      return null
    }
    when (encoding) {
      "utf8" -> return StandardCharsets.UTF_8
      "utf16" -> return StandardCharsets.UTF_16
      "ascii" -> return StandardCharsets.US_ASCII
    }
    return null
  }

  /**
   * Helper function to recursively delete a directory
   *
   * @param file The file or directory to recursively delete
   * @throws IOException
   */
  fun deleteRecursively(file: File) {
    if (file.isFile) {
      file.delete()
      return
    }
    for (f in file.listFiles()!!) {
      deleteRecursively(f)
    }
    file.delete()
  }

  /**
   * Helper function to recursively copy a directory structure (or just a file)
   *
   * @param src The source location
   * @param dst The destination location
   * @throws IOException
   */
  fun copyRecursively(src: File, dst: File) {
    if (src.isDirectory) {
      dst.mkdir()
      for (file in src.list()!!) {
        copyRecursively(File(src, file), File(dst, file))
      }

      return
    }
    if (dst.parentFile?.exists() == false) {
      dst.parentFile?.mkdirs()
    }
    if (!dst.exists()) {
      dst.createNewFile()
    }
    FileInputStream(src).channel.use { source ->
      FileOutputStream(dst).channel.use { destination ->
        destination.transferFrom(
          source, 0, source.size()
        )
      }
    }
  }

  fun saveToPictureDirectory(
    fileName: String, inputStream: InputStream, saveLocation: String? = "DWeb"
  ): Boolean {
    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
      put(MediaStore.MediaColumns.MIME_TYPE, "image/${getMimeType(fileName)}")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(
          MediaStore.MediaColumns.RELATIVE_PATH,
          "${Environment.DIRECTORY_PICTURES}/$saveLocation"
        )
        put(MediaStore.Video.Media.IS_PENDING, 1)
      }
    }
    val imageUri =
      context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    imageUri?.let { context.contentResolver.openOutputStream(it) }?.let { outputStream ->
      val byteArray = ByteArray(1024)
      while (inputStream.read(byteArray) != -1) {
        outputStream.write(byteArray)
      }
      outputStream.flush()
      outputStream.close()

      contentValues.clear()
      contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
      context.contentResolver.update(imageUri, contentValues, null, null)
      return true
    }
    return false
  }
}

/**
 * 权限状态
 */
enum class PermissionState(val state: Int) {
  /**无权限 */
  NULL(0),

  /**  可读*/
  READ(1 shl 1),

  /**  可写*/
  WRITE(1 shl 2),

  /** 可执行*/
  EXECUTE(1 shl 3),

  READ_WRITE(READ.state or WRITE.state),

  READ_EXECUTE(READ.state or EXECUTE.state),

  READ_WRITE_EXECUTE(READ.state or WRITE.state or EXECUTE.state);

}

enum class EFileType(val location: String) {
  /**
   * The Documents directory
   * On iOS it's the app's documents directory.
   * Use this directory to store user-generated content.
   * On Android it's the Public Documents folder, so it's accessible from other apps.
   * It's not accesible on Android 10 unless the app enables legacy External Storage
   * by adding `android:requestLegacyExternalStorage="true"` in the `application` tag
   * in the `AndroidManifest.xml`.
   * It's not accesible on Android 11 or newer.
   *
   * @since 1.0.0
   */
  Documents("DOCUMENTS"),


  /**
   * The Data directory
   * On iOS it will use the Documents directory.
   * On Android it's the directory holding application files.
   * Files will be deleted when the application is uninstalled.
   *
   * @since 1.0.0
   */
  Data("DATA"),

  /**
   * The Library directory
   * On iOS it will use the Library directory.
   * On Android it's the directory holding application files.
   * Files will be deleted when the application is uninstalled.
   *
   * @since 1.1.0
   */
  Library("LIBRARY"),

  /**
   * The Cache directory
   * Can be deleted in cases of low memory, so use this directory to write app-specific files
   * that your app can re-create easily.
   *
   * @since 1.0.0
   */
  Cache("CACHE"),

  /**
   * The external directory
   * On iOS it will use the Documents directory
   * On Android it's the directory on the primary shared/external
   * storage device where the application can place persistent files it owns.
   * These files are internal to the applications, and not typically visible
   * to the user as media.
   * Files will be deleted when the application is uninstalled.
   *
   * @since 1.0.0
   */
  External("EXTERNAL"),

  /**
   * The external storage directory
   * On iOS it will use the Documents directory
   * On Android it's the primary shared/external storage directory.
   * It's not accesible on Android 10 unless the app enables legacy External Storage
   * by adding `android:requestLegacyExternalStorage="true"` in the `application` tag
   * in the `AndroidManifest.xml`.
   * It's not accesible on Android 11 or newer.
   *
   * @since 1.0.0
   */
  ExternalStorage("EXTERNAL_STORAGE"),
}

class CopyFailedException : Exception {
  constructor(s: String?) : super(s) {}
  constructor(t: Throwable?) : super(t) {}
  constructor(s: String?, t: Throwable?) : super(s, t) {}
}

fun getMimeType(url: String): String {
  var mimeType = "*/*"
  val extensionIndex = url.lastIndexOf('.')
  if (extensionIndex > 0) {
    val extMimeType =
      MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(url.substring(extensionIndex + 1))
    if (extMimeType != null) {
      mimeType = extMimeType
    }
  }
  return mimeType
}