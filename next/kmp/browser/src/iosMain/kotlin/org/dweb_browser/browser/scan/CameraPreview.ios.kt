package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.text.style.TextAlign
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.dwebview.UnScaleBox
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.compose.toUIColor
import org.dweb_browser.helper.platform.NSDataHelper.toByteArray
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.render.LocalWindowContentStyle
import org.dweb_browser.sys.window.render.UIKitViewInWindow
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession.Companion.discoverySessionWithDeviceTypes
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDeviceInput.Companion.deviceInputWithDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDualCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDualWideCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInDuoCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInUltraWideCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeJPEG
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.hasTorch
import platform.AVFoundation.torchMode
import platform.CoreGraphics.CGRect
import platform.Foundation.NSError
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject


@Composable
actual fun CameraPreviewRender(
  modifier: Modifier,
  controller: SmartScanController
) {

  // 判断是否在模拟器执行
  if (PureViewController.isSimulator) {
    Text(
      """
      Camera is not available on simulator.
      Please try to run on a real iOS device.
      """.trimIndent(),
      color = Color.White,
      modifier = modifier,
      textAlign = TextAlign.Center
    )
  } else {
    RealDeviceCamera(modifier, controller)
  }
}


@OptIn(ExperimentalForeignApi::class)
@Composable
private fun RealDeviceCamera(
  modifier: Modifier,
  controller: SmartScanController
) {
  val uiViewController = LocalUIViewController.current
  // 创建一个 UIView 作为相机预览的容器
  val uiView = remember { UIView().apply { backgroundColor = Color.Red.toUIColor() } }
  // 创建相机控制器
  val cameraController = remember(uiViewController) {
    val c = CameraControllerImpl(controller, uiViewController, uiView)
    controller.cameraController?.stop()
    controller.cameraController = c // 赋值
    c
  }
  DisposableEffect(Unit) {
    cameraController.start()
    onDispose {
      cameraController.stop()
    }
  }
  cameraController.initCaptureDelegate()
  val scale by controller.scaleFlow.collectAsState()
  UnScaleBox(scale, modifier) {
    uiView.UIKitViewInWindow(
      modifier = Modifier.fillMaxSize(),
      style = LocalWindowContentStyle.current.frameStyle,
      onResize = { _, frame ->
        cameraController.onResize(frame)
      }
    )
  }
}


@OptIn(ExperimentalForeignApi::class)
class CameraControllerImpl(
  private val controller: SmartScanController,
  private val uiViewController: UIViewController,
  private val uiView: UIView
) : CameraController {
  //  设备类型
  private val deviceTypes = listOf(
    AVCaptureDeviceTypeBuiltInWideAngleCamera,
    AVCaptureDeviceTypeBuiltInDualWideCamera,
    AVCaptureDeviceTypeBuiltInDualCamera,
    AVCaptureDeviceTypeBuiltInUltraWideCamera,
    AVCaptureDeviceTypeBuiltInDuoCamera
  )

  // 创建输出
  private val photoOutput = AVCapturePhotoOutput()

  // 创建相机
  private val camera: AVCaptureDevice =
    discoverySessionWithDeviceTypes(
      deviceTypes = deviceTypes,
      mediaType = AVMediaTypeVideo,
      position = AVCaptureDevicePositionBack,
    ).devices.firstOrNull() as AVCaptureDevice

  // 创建一个相机会话
  private val captureSession =
    AVCaptureSession().also { captureSession ->
      // 配置相机设备
      captureSession.sessionPreset = AVCaptureSessionPresetPhoto
      val deviceInput: AVCaptureDeviceInput =
        deviceInputWithDevice(device = camera, error = null)!!
      captureSession.addInput(deviceInput)
      if (captureSession.canAddOutput(photoOutput)) {
        captureSession.addOutput(photoOutput)
      } else {
        WARNING("Could not add photo output to capture session")
      }
    }

  // 创建相机预览层
  private val cameraPreviewLayer: AVCaptureVideoPreviewLayer =
    AVCaptureVideoPreviewLayer(session = captureSession).apply {
      // 设置预览图层的视频重力属性
      videoGravity = AVLayerVideoGravityResizeAspectFill
      uiView.layer.addSublayer(this)
    }

  // 从相机流中拿内容
  private val photoCaptureDelegate: AVCapturePhotoCaptureDelegateProtocol =
    object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
      override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
      ) {
        if (error != null) {
          WARNING("Error capturing photo: ${error.localizedDescription}")
          return
        }
        val photoData = didFinishProcessingPhoto.fileDataRepresentation()
        if (photoData != null) {
          println("xxxxxx=> $photoData")
          // 回调到二维码识别
          controller.imageCaptureFlow.tryEmit(photoData)
        } else {
          WARNING("Photo data is null")
        }
      }
    }

  // 从相册拿内容
  private val galleryDelegate: PHPickerViewControllerDelegateProtocol =
    object : NSObject(), PHPickerViewControllerDelegateProtocol {
      override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>
      ) {
        val result = didFinishPicking[0] as PHPickerResult
        // 获取结果中的 itemProvider
        val itemProvider = result.itemProvider
        if (itemProvider.hasItemConformingToTypeIdentifier(UTTypeImage.toString())) {
          itemProvider.loadDataRepresentationForTypeIdentifier(UTTypeImage.toString()) { data, error ->
            if (data != null) {
              val byteArray = data.toByteArray()
              // 回调到二维码识别
              controller.imageCaptureFlow.tryEmit(byteArray)
            } else if (error != null) {
              // 处理错误
              WARNING("Error loading data: ${error.localizedDescription}")
            }
          }

          // 关闭选择器视图控制器
          picker.dismissViewControllerAnimated(true, null)
        }
      }
    }

  fun initCaptureDelegate() {
    // 创建照片设置，指定照片格式为 JPEG
    val photoSettings = AVCapturePhotoSettings.photoSettingsWithFormat(
      format = mapOf(AVVideoCodecKey to AVVideoCodecTypeJPEG)
    )
    // 捕获照片，使用指定的设置和委托
    photoOutput.capturePhotoWithSettings(
      settings = photoSettings,
      delegate = photoCaptureDelegate
    )
  }

  fun start() {
    captureSession.startRunning()
  }

  internal fun onResize(rect: CValue<CGRect>) {
    CATransaction.begin()
    CATransaction.setValue(true, kCATransactionDisableActions)
    uiView.layer.setFrame(rect)
    cameraPreviewLayer.setFrame(rect)
    CATransaction.commit()
  }


  @OptIn(ExperimentalForeignApi::class)
  override fun toggleTorch() {
    try {
      if (camera.hasTorch) {
        try {
          camera.lockForConfiguration(null)
          if (camera.torchMode == AVCaptureTorchModeOn) {
            camera.torchMode = AVCaptureTorchModeOff
          } else {
            camera.torchMode = AVCaptureTorchModeOn
          }
          camera.unlockForConfiguration()
        } catch (e: Throwable) {
          WARNING("Error toggling torch: $e")
        }
      }
    } catch (e: Exception) {
      WARNING("Torch not available: $e")
    }
  }

  /**实现打开相册*/
  override fun openAlbum() {
    val configuration = PHPickerConfiguration()
    val pickerController = PHPickerViewController(configuration)
    pickerController.setDelegate(galleryDelegate)
    uiViewController.presentViewController(pickerController, animated = true, completion = null)
  }

  override fun stop() {
    captureSession.stopRunning()
  }
}