package org.dweb_browser.helper.platform

import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.helper.platform.NSDataHelper.toByteArray
import org.dweb_browser.helper.platform.NSDataHelper.toNSData
import org.dweb_browser.helper.readLittleEndianInt
import org.dweb_browser.helper.toKString
import org.dweb_browser.helper.toLittleEndianByteArray
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.helper.utf8String
import org.dweb_browser.platform.ios.DwebKeyChainGenericStore
import org.dweb_browser.pure.crypto.hash.ccSha256
import platform.Foundation.NSString
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 一个不依赖外部存储权限，能在外部存储写入数据的工具类
 *
 * 使用文件夹名称来保存数据，而不是文件本身
 */
@OptIn(ExperimentalEncodingApi::class, ExperimentalForeignApi::class)
actual class DeviceKeyValueStore actual constructor(
  storeName: String,
  /**
   * 压缩算法
   */
  compressAlgorithm: String?,
) {
  init {
    if (compressAlgorithm != null) {
      throw Exception("no yet support compress algorithm: '$compressAlgorithm'")
    }
  }

  actual val supportEnumKeys = true

  private val store = DwebKeyChainGenericStore(service = storeName)

  private fun parseKey(key: ByteArray): Pair<String, Boolean> {
    val str = Base64.UrlSafe.encode(key)
    if (str.length > 128) {
      val hashId = Base64.UrlSafe.encode(ccSha256(key)) // ~= 48
      return "B$hashId" to false
    }
    return "A$str" to true
  }

  fun setRawItem(key: ByteArray, value: ByteArray) {
    val (account, isKey) = parseKey(key)
    store.saveItemWithAccount(
      account = account, (when {
        isKey -> value
        else -> key.size.toLittleEndianByteArray() + key + value
      }).toNSData()
    )
  }

  actual fun setItem(key: String, value: ByteArray): Boolean {
    setRawItem(key.utf8Binary, value)
    return true
  }

  private fun getRawItem(key: ByteArray): ByteArray? {
    val (account, isKey) = parseKey(key)
    val rawValue = store.loadItemWithAccount(account = account)?.toByteArray() ?: return null
    return when {
      isKey -> rawValue
      else -> rawValue.copyOfRange(rawValue.readLittleEndianInt() + 4, rawValue.size)
    }
  }

  actual fun getItem(key: String) = getRawItem(key.utf8Binary)

  private fun hasRawItem(key: ByteArray): Boolean {
    val (account, _) = parseKey(key)
    return store.hasItemWithAccount(account = account)
  }

  actual fun hasItem(key: String) = hasRawItem(key.utf8Binary)

  private fun getRawKeys() = store.getAllAccounts().mapNotNull { account ->
    val key = (account as NSString).toKString()
    when {
      key.startsWith("A") -> Base64.UrlSafe.decode(key.substring(1))
      key.startsWith("B") -> store.loadItemWithAccount(key)?.toByteArray()?.let { rawValue ->
        val keySize = rawValue.readLittleEndianInt()
        rawValue.copyOfRange(4, 4 + keySize)
      }

      else -> null
    }
  }

  actual fun keys() = getRawKeys().map { it.utf8String }

  private fun deleteRawItem(key: ByteArray): Boolean {
    val (account, _) = parseKey(key)
    return store.deleteItemWithAccount(account)
  }

  actual fun deleteItem(key: String) = deleteRawItem(key.utf8Binary)
}