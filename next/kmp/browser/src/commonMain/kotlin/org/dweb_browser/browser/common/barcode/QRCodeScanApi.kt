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
 * 扫码时，打开相机内容
 * @param openAlarmResult 打开相册返回图片的操作
 * @param onBarcodeDetected 扫码后直接将图片处理的结果
 * @param maskView 上面遮罩层的内容，包括二维码的扫码区间和蓝色移动的横条，以及下面的图标按钮
 */
@Composable
expect fun CameraPreviewRender(
  openAlarmResult: (ImageBitmap) -> Unit,
  onBarcodeDetected: (QRCodeDecoderResult) -> Unit,
  maskView: @Composable (FlashLightSwitch, OpenAlbum) -> Unit,
  onCancel: (String) -> Unit, // 增加该字段，主要是因为 desktop 可能不选择照片，直接返回，需要增加该功能响应事件
)

/**
 * 振动，响铃
 */
expect fun beepAudio()

/**
 * 解析图片中的二维码
 */
expect fun decoderImage(
  imageBitmap: ImageBitmap,
  onSuccess: (QRCodeDecoderResult) -> Unit,
  onFailure: (Exception) -> Unit,
)

/**
 * 计算二维码的位置
 */
expect fun transformPoint(
  x: Int, y: Int, srcWidth: Int, srcHeight: Int, destWidth: Int, destHeight: Int, isAlarm: Boolean,
): QRCodeDecoderResult.Point

/**
 * 打开扫码的结果
 */
expect fun openDeepLink(data: String, showBackground: Boolean = false): Boolean