package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import org.dweb_browser.helper.compose.NativeBackHandler
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.isDesktop
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

@OptIn(ExperimentalResourceApi::class)
@Composable
fun WindowContentRenderScope.RenderBarcodeScanning(
  modifier: Modifier, controller: SmartScanController
) {
  // 全局返回操作，只关闭扫码
  NativeBackHandler {
    controller.onCancel("NativeBackHandler")
  }
//  // 本来考虑这边可以监听activity如果是onPause的话，也关闭的，但是发现这个触发时机不过及时
//  LocalWindowController.current.pureViewControllerState.value?.onPause {
//    controller.onCancel("onPause")
//  }

  val selectImg by controller.albumImageFlow.collectAsState()
  // 当用户选中文件的时候切换到Album模式
  selectImg?.let { controller.updatePreviewType(SmartModuleTypes.Album) }
  Box(modifier) {
    when (controller.previewTypes) {
      // 视图切换,如果扫描到了二维码
      SmartModuleTypes.Scanning -> {
        // 渲染相机内容
        CameraPreviewRender(modifier = Modifier.fillMaxSize(), controller = controller)
        // 扫描线和打开相册，暂时不再桌面端支持
        // TODO 根据设备是否支持摄像头来做这个事情
        controller.DefaultScanningView(
          modifier = Modifier.fillMaxSize().zIndex(2f), showLight = !IPureViewController.isDesktop
        )
        // 渲染扫码结果
        controller.RenderScanResultView(Modifier.matchParentSize().zIndex(3f))
      }
      // 相册选择
      SmartModuleTypes.Album -> {
        selectImg?.let { byteArray ->
          // 如果是选中图片，渲染选中的图片
          LaunchedEffect(byteArray) {
            controller.decodeQrCode {
              recognize(byteArray, 0)
            }
          }
          controller.RenderAlbumPreview(Modifier.fillMaxSize(), byteArray.decodeToImageBitmap())
        } ?: run {
          AlbumPreviewRender(modifier, controller) // 如果图片为空，就打开相册选择器
        }
      }
      // 内窥模式
      SmartModuleTypes.Endoscopic -> {
        controller.EndoscopicPreview(modifier)
        ScannerLine() // 添加扫描线
        // 渲染扫码结果
        controller.RenderScanResultView(
          Modifier.matchParentSize().zIndex(3f)
        )
      }
    }
  }
}

/**相机preview视图*/
@Composable
expect fun CameraPreviewRender(
  modifier: Modifier = Modifier, controller: SmartScanController
)

/**这里是文件选择视图*/
@Composable
expect fun AlbumPreviewRender(
  modifier: Modifier = Modifier, controller: SmartScanController
)