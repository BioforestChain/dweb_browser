package org.dweb_browser.browser.common.barcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 传入关闭或者打开，返回结果是否成功
 */
typealias FlashLightSwitch = (Boolean) -> Boolean

/**
 * 打开系统相册，选择照片
 */
typealias OpenAlbum = () -> Unit

/**
 * 相机功能
 */
@Composable
expect fun CameraPreviewView(
  openAlarmResult: (ImageBitmap) -> Unit,
  onBarcodeDetected: (QRCodeDecoderResult) -> Unit,
  maskView: @Composable (FlashLightSwitch, OpenAlbum) -> Unit
)

/**
 * 振动，响铃
 */
expect fun beepAudio()

/**
 * 解析图片中的二维码
 */
expect fun decoderImage(
  imageBitmap: ImageBitmap, onSuccess: (QRCodeDecoderResult) -> Unit, onFailure: (Exception) -> Unit
)

/**
 * 计算二维码的位置
 */
expect fun transformPoint(
  x: Int,
  y: Int,
  srcWidth: Int,
  srcHeight: Int,
  destWidth: Int,
  destHeight: Int,
  isFit: Boolean = false
): QRCodeDecoderResult.Point

/**
 * 打开扫码的结果
 */
expect fun openDeepLink(data: String)