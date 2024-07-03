package org.dweb_browser.helper.platform

class DeviceKeyValueStore(
  val storeName: String,
  /**
   * 压缩算法
   */
  val compressAlgorithm: String? = null,
) {
  init {
    if (compressAlgorithm != null) {
      throw Exception("no yet support compress algorithm: '$compressAlgorithm'")
    }
  }
}