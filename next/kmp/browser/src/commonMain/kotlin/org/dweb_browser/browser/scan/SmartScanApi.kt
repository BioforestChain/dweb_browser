package org.dweb_browser.browser.scan

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.PurePoint
import org.dweb_browser.helper.PureRect

/**二维码解析管理器*/
expect class ScanningController(mmScope: CoroutineScope) {
  fun stop()

  /**解析二维码*/
  suspend fun recognize(data: ByteArray, rotation: Int): List<BarcodeResult>

  /**解析二维码时候的震动效果*/
  fun decodeHaptics()
}

@Serializable
class BarcodeResult(
  val data: String,
  val boundingBox: PureRect,
  val topLeft: PurePoint,
  val topRight: PurePoint,
  val bottomLeft: PurePoint,
  val bottomRight: PurePoint,
)

/**
 * 打开扫码的结果
 */
expect fun openDeepLink(data: String, showBackground: Boolean = false): Boolean