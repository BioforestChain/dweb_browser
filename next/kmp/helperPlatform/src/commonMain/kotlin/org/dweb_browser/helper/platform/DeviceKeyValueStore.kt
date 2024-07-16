package org.dweb_browser.helper.platform

expect class DeviceKeyValueStore(
  storeName: String,
  /**
   * 压缩算法
   */
  compressAlgorithm: String? = null,
) {
  val supportEnumKeys: Boolean
  fun keys(): List<String>
  fun getItem(key: String): ByteArray?
  fun hasItem(key: String): Boolean
  fun setItem(key: String, value: ByteArray): Boolean
  fun deleteItem(key: String): Boolean
}