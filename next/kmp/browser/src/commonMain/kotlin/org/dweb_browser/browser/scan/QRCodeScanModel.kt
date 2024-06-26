package org.dweb_browser.browser.scan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.PureRect
import org.dweb_browser.helper.Signal

/**
 * @property Hide 隐藏
 * @property Scanning 扫码中
 * @property AnalyzePhoto 分析图片
 * @property CameraCheck 相册实时识别后，跳转到该状态，如果存在多个二维码，需要用户点击
 * @property AlarmCheck 相册识别后，跳转到该状态，如果存在多个二维码，需要用户点击
 */
enum class QRCodeState(val type: Int) {
  Hide(0), Scanning(1), AnalyzePhoto(2), CameraCheck(3), AlarmCheck(4);
}

/**
 * 用于扫码解析完成后，确定坐标信息
 */
@Serializable
data class QRCodeDecoderResult(
  var preBitmap: ImageBitmap?,
  var lastBitmap: ImageBitmap?,
  var listQRCode: List<QRCode>,
) {
  @Serializable
  data class Point(val x: Float, val y: Float)

  @Serializable
  data class QRCode(val rect: PureRect, val displayName: String?)
}

class QRCodeScanModel {
  var state by mutableStateOf(QRCodeState.Hide)
  fun updateQRCodeStateUI(qrCodeState: QRCodeState) {
    state = qrCodeState
  }

  var imageBitmap: ImageBitmap? = null
  var qrCodeDecoderResult: QRCodeDecoderResult? = null

  /**
   * 监听状态的变化
   */
  internal val stateChange = Signal<QRCodeState>()
  val onStateChange = stateChange.toListener()
}