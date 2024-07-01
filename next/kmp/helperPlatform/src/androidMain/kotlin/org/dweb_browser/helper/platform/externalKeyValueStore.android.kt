package org.dweb_browser.helper.platform

import android.os.Environment
import io.ktor.util.decodeBase64Bytes
import org.dweb_browser.helper.base64
import org.dweb_browser.pure.crypto.hash.jvmSha256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 一个不依赖外部存储权限，能在外部存储写入数据的工具类
 *
 * 使用文件夹名称来保存数据，而不是文件本身
 */
@OptIn(ExperimentalEncodingApi::class)
class ExternalKeyValueStore(
  val storeName: String,
  /**
   * 压缩算法
   */
  val compressAlgorithm: String? = null,
) {
  companion object {
    val externalDir by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    }
  }

  init {

    if (compressAlgorithm != null) {
      throw Exception("no yet support compress algorithm: '$compressAlgorithm'")
    }
  }

  val storeDir = externalDir.resolve("dweb/$storeName")

  private fun parseData(data: ByteArray): Pair<String, List<String>> {
    val str = Base64.encode(data)
    if (str.length > 128) {
      val hashId = jvmSha256(data).base64.substring(0, 128)
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
    setItem(key.encodeToByteArray(), value.encodeToByteArray())

  fun setItem(key: ByteArray, value: ByteArray) {
    val (keyId, keyDetail) = parseData(key)
    val (valueId, valueDetail) = parseData(value)
    val keyDir = storeDir.resolve("k/$keyId")
    val valueDir = keyDir.resolve("v/$valueId")
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

  fun getItem(key: String) = getItem(key.encodeToByteArray())

  fun getItem(key: ByteArray): ByteArray? {
    val (keyId, _) = parseData(key)
    val keyDir = storeDir.resolve("k/$keyId")
    return runCatching {
      val valueId = keyDir.resolve("v").list()!!.first()
      if (valueId.startsWith("A")) {
        valueId.substring(1).decodeBase64Bytes()
      } else if (valueId.startsWith("B")) {
        val valueDir = keyDir.resolve("v/$valueId")
        val value = valueDir.list()!!.map {
          val (index, s) = it.split("-")
          index to s
        }.sortedBy { it.first }.joinToString("") { it.second }.decodeBase64Bytes()

        value
      } else null
    }.getOrNull()
  }
}