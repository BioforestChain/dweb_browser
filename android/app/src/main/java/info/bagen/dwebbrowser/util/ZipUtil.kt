package info.bagen.dwebbrowser.util

import android.util.Log
import info.bagen.dwebbrowser.ui.entity.AppInfo
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ZipUtil {
  private val TAG = ZipUtil::class.simpleName

  private val BUFFER_SIZE = 1024 * 100

  /**
   * 私有函数将文件集合压缩成tar包后返回
   *
   * @param files  要压缩的文件集合
   * @param target tar 输出流的目标文件
   * @return File  指定返回的目标文件
   */
  @Throws(IOException::class)
  private fun pack(files: List<File>, target: File): File? {
    FileOutputStream(target).use { fos ->
      BufferedOutputStream(fos, BUFFER_SIZE).use { bos ->
        TarArchiveOutputStream(bos).use { taos ->
          //解决文件名过长问题
          taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
          for (file in files) {
            taos.putArchiveEntry(TarArchiveEntry(file))
            FileInputStream(file).use { fis ->
              IOUtils.copy(fis, taos)
              taos.closeArchiveEntry()
            }
          }
        }
      }
    }
    return target
  }

  /**
   * 将tar压缩成tar.gz文件
   *
   * @param list
   * @param outPutPath
   * @param fileName
   */
  @Throws(IOException::class)
  fun compress(list: List<File>, outPutPath: String, fileName: String): File? {
    val outPutFile = File(outPutPath + File.separator.toString() + fileName + ".tar.gz")
    val tempTar = File("temp.tar")
    FileInputStream(pack(list, tempTar)).use { fis ->
      BufferedInputStream(fis, BUFFER_SIZE).use { bis ->
        FileOutputStream(outPutFile).use { fos ->
          GZIPOutputStream(fos).use { gzp ->
            var count: Int
            val data = ByteArray(BUFFER_SIZE)
            while (bis.read(data, 0, BUFFER_SIZE).also { count = it } != -1) {
              gzp.write(data, 0, count)
            }
          }
        }
      }
    }
    try {
      Files.deleteIfExists(tempTar.toPath())
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return outPutFile
  }

  fun ergodicDecompress(
    filePath: String, outputDir: String, isDeleted: Boolean = true, mmid: String? = null
  ): Boolean {
    Log.w(TAG, "ergodicDecompress->$filePath, $outputDir, $isDeleted, $mmid")
    val file = File(filePath)
    if (!file.exists()) {
      Log.e(TAG, "ergodicDecompress file not exist.")
      return false
    }
    var ret = false
    for (index in arrayListOf("tar", "tar.gz", "tar.bz2", "zip")) {
      try {
        val dirName = when (index) {
          "tar" -> {
            decompressTar(file, outputDir, mmid)
          }
          "tar.gz" -> {
            decompressTarGz(file, outputDir, mmid)
          }
          "tar.bz2" -> {
            decompressTarBz2(file, outputDir, mmid)
          }
          "zip" -> {
            unZip(file, outputDir, mmid)
          }
          else -> "NULL"
        }
        filterFile(File(outputDir))
        ret = true
        Log.e(TAG, "ergodicDecompress:: fileType is $index.$dirName")
        break
      } catch (e: IOException) {
        Log.e(TAG, "ergodicDecompress:: decompress occur error{$index}. ${e.message}")
      }
    }
    if (isDeleted && ret) {
      FilesUtil.deleteQuietly(file)
    }
    return ret
  }

  private fun getEntryName(
    entryName: String,
    mmid: String?,
    dirName: ((String) -> String)
  ): String {
    return when (entryName) {
      ".", "..", "__MACOSX" -> entryName
      else -> {
        mmid?.let { mmid ->
          val sss = dirName(entryName)
          entryName.replaceFirst(sss, "$mmid/")// 由于entryName如果是目录最后一个字符是 /
        } ?: entryName
      }
    }
  }

  private fun decompress(filePath: String, outputDir: String, isDeleted: Boolean = true): Boolean {
    Log.d(TAG, "decompress->$filePath, $outputDir, $isDeleted")
    val file = File(filePath)
    if (!file.exists()) {
      Log.e(TAG, "decompress file not exist.")
      return false
    }
    var unzip = false
    try {
      if (filePath.endsWith(".zip")) {
        unZip(file, outputDir)
      } else if (filePath.endsWith(".tar")) {
        decompressTar(file, outputDir)
      } else if (filePath.endsWith(".bfsa") ||
        filePath.endsWith(".tar.gz") || filePath.endsWith(".tgz")
      ) {
        decompressTarGz(file, outputDir)
      } else if (filePath.endsWith(".tar.bz2")) {
        decompressTarBz2(file, outputDir)
      }
      filterFile(File(outputDir))
      unzip = true
    } catch (e: IOException) {
      Log.e(TAG, "decompress:: decompress occur error.")
    } finally {
      if (isDeleted) {
        FilesUtil.deleteQuietly(file)
      }
    }
    return unzip
  }

  /**
   * 解压 .zip 文件
   *
   * @param file      要解压的zip文件对象
   * @param outputDir 要解压到某个指定的目录下
   * @throws IOException
   */
  @Throws(IOException::class)
  private fun unZip(file: File?, outputDir: String, mmid: String? = null): String {
    Log.d(TAG, "unZip->${file?.absolutePath}, $outputDir")
    val dirName = "$outputDir/$mmid"
    ZipFile(file, StandardCharsets.UTF_8).use { zipFile ->
      //创建输出目录
      createDirectory(outputDir, null)
      val enums: Enumeration<*> = zipFile.entries()
      while (enums.hasMoreElements()) {
        val entry: ZipEntry = enums.nextElement() as ZipEntry
        val name = entry.name // getEntryName(entry.name, mmid) { if (dirName.isEmpty()) dirName = it; dirName }
        if (entry.isDirectory) {
          //创建空目录
          createDirectory(outputDir, name)
        } else { //是文件
          createDirectory(outputDir, name.substring(0, name.lastIndexOf("/"))) // 需要确保父级目录存在
          zipFile.getInputStream(entry).use { inputStream ->
            FileOutputStream(
              File(outputDir + File.separator + name)
            ).use { out -> writeFile(inputStream, out) }
          }
        }
      }
    }
    return dirName
  }

  @Throws(IOException::class)
  private fun decompressTar(file: File?, outputDir: String, mmid: String? = null): String {
    val dirName = "$outputDir/$mmid"
    TarArchiveInputStream(FileInputStream(file)).use { tarIn ->
      //创建输出目录
      createDirectory(outputDir, null)
      var entry: TarArchiveEntry? = null
      while (tarIn.nextTarEntry.also { entry = it } != null) {
        val name = entry!!.name
        //val name = getEntryName(entry!!.name, mmid) { if (dirName.isEmpty()) dirName = it; dirName }
        //是目录
        if (entry!!.isDirectory) {
          //创建空目录
          createDirectory(outputDir, name)
        } else { //是文件
          createDirectory(outputDir, name.substring(0, name.lastIndexOf("/"))) // 需要确保父级目录存在
          FileOutputStream(
            File(outputDir + File.separator.toString() + name)
          ).use { out -> writeFile(tarIn, out) }
        }
      }
    }
    return dirName
  }

  @Throws(IOException::class)
  private fun decompressTarGz(file: File?, outputDir: String, mmid: String? = null): String {
    Log.d(TAG, "decompressTarGz->${file?.absolutePath}, $outputDir")
    val dirName = "$outputDir/$mmid"
    TarArchiveInputStream(
      GzipCompressorInputStream(BufferedInputStream(FileInputStream(file)))
    ).use { tarIn ->
      //创建输出目录
      createDirectory(outputDir, null)
      var entry: TarArchiveEntry? = null
      while (tarIn.nextTarEntry.also { entry = it } != null) {
        val name = entry!!.name // getEntryName(entry!!.name, mmid) { if (dirName.isEmpty()) dirName = it; dirName }
        //是目录
        if (entry!!.isDirectory) {
          //创建空目录
          createDirectory(outputDir, name)
        } else { //是文件
          createDirectory(outputDir, name.substring(0, name.lastIndexOf("/"))) // 需要确保父级目录存在
          FileOutputStream(
            File(outputDir + File.separator + name)
          ).use { out -> writeFile(tarIn, out) }
        }
      }
    }
    return dirName
  }

  /**
   * 解压缩tar.bz2文件
   *
   * @param file      压缩包文件
   * @param outputDir 目标文件夹
   */
  @Throws(IOException::class)
  private fun decompressTarBz2(file: File?, outputDir: String, mmid: String? = null): String {
    Log.d(TAG, "decompressTarBz2->${file?.absolutePath}, $outputDir")
    val dirName = "$outputDir/$mmid"
    TarArchiveInputStream(
      BZip2CompressorInputStream(FileInputStream(file))
    ).use { tarIn ->
      createDirectory(outputDir, null)
      var entry: TarArchiveEntry
      while (tarIn.nextTarEntry.also { entry = it } != null) {
        val name = entry.name // getEntryName(entry.name, mmid) { if (dirName.isEmpty()) dirName = it; dirName }
        if (entry.isDirectory) { // 是目录
          createDirectory(outputDir, name)
        } else { // 是文件
          createDirectory(outputDir, name.substring(0, name.lastIndexOf("/"))) // 需要确保父级目录存在
          FileOutputStream(
            File(outputDir + File.separator.toString() + name)
          ).use { out -> writeFile(tarIn, out) }
        }
      }
    }
    return dirName
  }

  /**
   * 写文件
   *
   * @param in
   * @param out
   * @throws IOException
   */
  @Throws(IOException::class)
  private fun writeFile(inputStream: InputStream, out: OutputStream) {
    var length: Int
    val b = ByteArray(BUFFER_SIZE)
    while (inputStream.read(b).also { length = it } != -1) {
      out.write(b, 0, length)
    }
  }

  /**
   * 创建目录
   *
   * @param outputDir
   * @param subDir
   */
  private fun createDirectory(outputDir: String, subDir: String?) {
    var file = File(outputDir)
    //子目录不为空
    if (!(subDir == null || subDir.trim { it <= ' ' } == "")) {
      file = File(outputDir + File.separator.toString() + subDir)
    }
    if (!file.exists()) {
      if (file.parentFile?.exists() == false) {
        file.parentFile?.mkdirs()
      }
      file.mkdirs()
    }
  }

  /**
   * 删除Mac压缩再解压产生的 __MACOSX 文件夹和 .开头的其他文件
   *
   * @param filteredFile
   */
  private fun filterFile(filteredFile: File?) {
    if (filteredFile != null) {
      filteredFile.listFiles()?.let { files ->
        for (file in files) {
          if (file.name.startsWith(".") ||
            file.isDirectory && file.name.equals("__MACOSX")
          ) {
            FilesUtil.deleteQuietly(file)
          }
        }
      }
    }
  }

  /**
   * 预解压，同时获取app信息，如bfsId, icon等
   */
  fun getAppInfoByPreDecompressTarGz(filePath: String): AppInfo? {
    Log.d(TAG, "getAppInfoByPreDecompressTarGz->$filePath")
    val file = File(filePath)
    if (!file.exists()) {
      Log.e(TAG, "getAppInfoByPreDecompressTarGz file not exist.")
      return null
    }
    try {
      TarArchiveInputStream(
        GzipCompressorInputStream(BufferedInputStream(FileInputStream(file)))
      ).use { tarIn ->
        //创建输出目录
        //createDirectory(outputDir, null)
        var entry: TarArchiveEntry? = null
        while (tarIn.nextTarEntry.also { entry = it } != null) {
          //是文件
          if (entry!!.isFile && entry!!.name!!.contains("boot/link.json")) {
            val content = String(tarIn.readBytes())
            return JsonUtil.getAppInfoFromLinkJson(content, APP_DIR_TYPE.SystemApp)
          }
        }
      }
    } catch (e: IOException) {
      Log.e(TAG, "getAppInfoByPreDecompressTarGz:: decompress occur error.")
    }
    return null
  }
}
