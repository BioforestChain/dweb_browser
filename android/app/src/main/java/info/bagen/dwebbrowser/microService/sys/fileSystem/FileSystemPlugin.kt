package info.bagen.dwebbrowser.microService.sys.fileSystem

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Base64
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.sys.fileSystem.exeprions.CopyFailedException
import info.bagen.dwebbrowser.microService.sys.fileSystem.exeprions.DirectoryExistsException
import info.bagen.dwebbrowser.microService.sys.fileSystem.exeprions.DirectoryNotFoundException
import kotlinx.coroutines.*
import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

typealias path = String

class FileSystemPlugin {

  private var context: Context? = App.appContext

  fun Filesystem(context: Context?) {
    this.context = context
  }

  @Throws(IOException::class)
  fun readFile(path: String?, directory: String?, charset: Charset?): String? {
    val `is` = getInputStream(path, directory)
    val dataStr: String
    if (charset != null) {
      dataStr = readFileAsString(`is`, charset.name())
    } else {
      dataStr = readFileAsBase64EncodedData(`is`)
    }
    return dataStr
  }

  @Throws(IOException::class)
  fun saveFile(file: File?, data: InputStream, append: Boolean = false) {
    FileOutputStream(file, append).use { outputStream ->
      var read: Int
      val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
      while (data.read(bytes).also { read = it } != -1) {
        outputStream.write(bytes, 0, read)
      }
    }
  }

  @Throws(FileNotFoundException::class)
  fun deleteFile(file: String?, directory: String?): Boolean {
    val fileObject = getFileObject(file, directory)
    if (!fileObject.exists()) {
      throw FileNotFoundException("File does not exist")
    }
    return fileObject.delete()
  }

  @Throws(DirectoryExistsException::class)
  fun mkdir(path: String?, directory: String?, recursive: Boolean): Boolean {
    val fileObject: File = getFileObject(path, directory)
    if (fileObject.exists()) {
      throw DirectoryExistsException("Directory exists")
    }
    var created = false
    created = if (recursive) {
      fileObject.mkdirs()
    } else {
      fileObject.mkdir()
    }
    return created
  }

  @Throws(DirectoryNotFoundException::class)
  fun readdir(path: String?, directory: String?): Array<File?>? {
    var files: Array<File?>? = null
    val fileObject: File = getFileObject(path, directory)
    files = if (fileObject != null && fileObject.exists()) {
      fileObject.listFiles()
    } else {
      throw DirectoryNotFoundException("Directory does not exist")
    }
    return files
  }

  @Throws(IOException::class, CopyFailedException::class)
  fun copy(
    from: String?,
    directory: String?,
    to: String?,
    toDirectory: String?,
    doRename: Boolean
  ): File {
    val fromObject: File = getFileObject(from, directory)
    val toObject: File = getFileObject(to, toDirectory)
    if (toObject == fromObject) {
      return toObject
    }
    if (!fromObject.exists()) {
      throw CopyFailedException("The source object does not exist")
    }
    if (toObject.parentFile.isFile) {
      throw CopyFailedException("The parent object of the destination is a file")
    }
    if (!toObject.parentFile.exists()) {
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

  @Throws(IOException::class)
  fun readFileAsString(`is`: InputStream, encoding: String): String {
    val outputStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var length = 0
    while (`is`.read(buffer).also { length = it } != -1) {
      outputStream.write(buffer, 0, length)
    }
    return outputStream.toString(encoding)
  }

  @Throws(IOException::class)
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


  @Throws(IOException::class)
  fun getInputStream(path: String?, directory: String?): InputStream {
    if (directory == null) {
      val u: Uri = Uri.parse(path)
      return if (u.getScheme().equals("content")) {
        context?.contentResolver?.openInputStream(u)!!
      } else {
        FileInputStream(File(u.getPath()))
      }
    }
    val androidDirectory: File = this.getDirectory(directory)
      ?: throw IOException("Directory not found")
    return FileInputStream(File(androidDirectory, path))
  }

  fun getDirectory(directory: String?): File? {
    val c: Context = context!!
    when (directory) {
      EFileDirectory.Documents.location -> return Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOCUMENTS
      )

      EFileDirectory.Data.location, EFileDirectory.Library.location -> return c.getFilesDir()
      EFileDirectory.Cache.location -> return c.getCacheDir()
      EFileDirectory.External.location -> return c.getExternalFilesDir(null)
      EFileDirectory.ExternalStorage.location -> return Environment.getExternalStorageDirectory()
    }
    return null
  }

  fun getFileObject(path: String?, directory: String?): File {
    if (directory == null) {
      val u = Uri.parse(path)
      if (u.scheme == null || u.scheme == "file") {
        return File(u.path)
      }
    }
    val androidDirectory = getDirectory(directory)
    if (androidDirectory == null) {
      throw Throwable("androidDirectory is null")
    } else {
      if (!androidDirectory.exists()) {
        androidDirectory.mkdir()
      }
    }
    return File(androidDirectory, path)
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
  @Throws(IOException::class)
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
  @Throws(IOException::class)
  fun copyRecursively(src: File, dst: File) {
    if (src.isDirectory) {
      dst.mkdir()
      for (file in src.list()!!) {
        copyRecursively(File(src, file), File(dst, file))
      }
      return
    }
    if (!dst.parentFile.exists()) {
      dst.parentFile.mkdirs()
    }
    if (!dst.exists()) {
      dst.createNewFile()
    }
    FileInputStream(src).channel.use { source ->
      FileOutputStream(dst).channel.use { destination ->
        destination.transferFrom(
          source,
          0,
          source.size()
        )
      }
    }
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

enum class EFileDirectory(val location: String) {
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