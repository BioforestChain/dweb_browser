package org.dweb_browser.helper.platform

import android.os.Environment
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String
import org.dweb_browser.pure.crypto.hash.jvmSha256
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 一个不依赖外部存储权限，能在外部存储写入数据的工具类
 *
 * 使用文件夹名称来保存数据，而不是文件本身
 */
@OptIn(ExperimentalEncodingApi::class)
class DeviceKeyValueStore(
  val storeName: String,
  baseDir: File = externalDir,
  /**
   * 压缩算法
   */
  val compressAlgorithm: String? = null,
) {
  companion object {
    val externalDir by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        .resolve("dweb-kv")
    }
  }

  init {

    if (compressAlgorithm != null) {
      throw Exception("no yet support compress algorithm: '$compressAlgorithm'")
    }
  }

  val storeDir = baseDir.resolve(storeName)

  private fun parseReadData(data: ByteArray): String {
    val str = Base64.UrlSafe.encode(data)
    if (str.length > 128) {
      val hashId = Base64.UrlSafe.encode(jvmSha256(data)) // ~= 48
      return "B$hashId"
    }
    return "A$str"
  }

  private fun parseWriteData(data: ByteArray): Pair<String, List<String>> {
    val str = Base64.UrlSafe.encode(data)
    if (str.length > 128) {
      val hashId = Base64.UrlSafe.encode(jvmSha256(data)) // ~= 48
      val result = mutableListOf<String>()
      var rest = str
      while (rest.length > 128) {
        result.add(rest.substring(0, 128))
        rest = rest.substring(128)
      }
      result.add(rest)
      return "B$hashId" to result//.mapIndexed { index, s -> "$index-$s" }
    }
    return "A$str" to emptyList()
  }

  fun setItem(key: String, value: String) =
    setRawItem(key.utf8Binary, value.utf8Binary)

  fun setRawItem(key: ByteArray, value: ByteArray) {
    val (keyId, keyDetail) = parseWriteData(key)
    val (valueId, valueDetail) = parseWriteData(value)
    val keyDir = storeDir.resolve("k/$keyId")
    val valueDir = keyDir.resolve("v/$valueId")
    println("QAQ keyId=$keyId, keyDetail=$keyDetail")
    println("QAQ valueId=$valueId, valueDetail=$valueDetail")
    // 删除原本存储的数据，包括如果key的hash冲突，也会删除原本的冲突的数据
    keyDir.deleteRecursively()
    // 写入数据
    valueDir.mkdirs()

    /// 将key的详细信息写入keyDir
    if (keyDetail.isNotEmpty()) {
      keyDetail.forEachIndexed { index, s ->
        keyDir.resolve("$index-$s").mkdirs()
      }
    }
    /// 将value的详细信息写入valueDir
    if (valueDetail.isNotEmpty()) {
      valueDetail.forEachIndexed { index, s ->
        valueDir.resolve("$index-$s").mkdirs()
      }
    }
  }

  fun getItem(key: String) = getRawItem(key.utf8Binary)?.utf8String

  fun getRawItem(key: ByteArray): ByteArray? {
    val keyId = parseReadData(key)
    val keyDir = storeDir.resolve("k/$keyId")
    return runCatching {
      val valueId = keyDir.resolve("v").list()!!.first()
      if (valueId.startsWith("A")) {
        Base64.UrlSafe.decode(valueId.substring(1))
      } else if (valueId.startsWith("B")) {
        val valueDir = keyDir.resolve("v/$valueId")
        val value = valueDir.list()!!.map {
          val (index, s) = it.split("-")
          index.toInt() to s
        }.sortedBy { it.first }.joinToString("") { it.second }.let { Base64.UrlSafe.decode(it) }

        value
      } else null
    }.getOrNull()
  }

  fun getRawKeys() = runCatching {
    storeDir.resolve("k").list()!!.mapNotNull { keyId ->
      when {
        keyId.startsWith("A") -> Base64.UrlSafe.decode(keyId.substring(1))
        keyId.startsWith("B") -> {
          storeDir.resolve("k/$keyId").list()!!.map {
            when (it) {
              "v" -> 0 to ""
              else -> {
                val (index, s) = it.split("-")
                index.toInt() to s
              }
            }
          }.sortedBy { it.first }.joinToString("") { it.second }.let { Base64.UrlSafe.decode(it) }
        }

        else -> null
      }
    }
  }.getOrElse { emptyList() }

  fun getKeys() = getRawKeys().map { it.utf8String }

  fun removeRawKey(key: ByteArray): Boolean {
    val keyId = parseReadData(key)
    val keyDir = storeDir.resolve("k/$keyId")
    return keyDir.deleteRecursively()
  }

  fun removeKey(key: String) = removeRawKey(key.utf8Binary)
}

