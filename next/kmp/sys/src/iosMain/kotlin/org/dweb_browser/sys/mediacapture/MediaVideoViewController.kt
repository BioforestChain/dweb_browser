package org.dweb_browser.sys.mediacapture

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.toRect
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureFileOutput
import platform.AVFoundation.AVCaptureFileOutputRecordingDelegateProtocol
import platform.AVFoundation.AVCaptureMovieFileOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSTimer
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UILabel
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIViewController
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_SEC
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

class MediaVideoViewController : UIViewController(nibName = null, bundle = null) {

  //视频捕获会话。它是input和output的桥梁。它协调着intput到output的数据传输
  private val captureSession = AVCaptureSession()

  //视频输入设备
  private val videoDevice = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)

  //音频输入设备
  private val audioDevice = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeAudio)

  //将捕获到的视频输出到文件
  private val fileOutput = AVCaptureMovieFileOutput()

  //是否在录频
  private var isRecording = false

  //录制按钮
  private var videoButton: VideoButton? = null

  //定时器
  private var timer: NSTimer? = null

  //显示录制时长
  private var timeLabel: UILabel? = null

  //默认最大录制时长: 秒
  var maxDuration: Int = 60

  //界面离开方式
  private var isClickVideoButton: Boolean = false

  var videoPathBlock: (path: String) -> Unit = {}

  override fun viewDidLoad() {
    this.view.backgroundColor = UIColor.blackColor

    initSessionInfo()
    initTopView()
  }

  override fun viewDidDisappear(animated: Boolean) {
    videoPathBlock(delegate.filePath)
    stopRecording()
    this.captureSession.stopRunning()
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun initSessionInfo() {
    val isSuccess = initCaptureSession()
    if (!isSuccess) {
      return
    }

    initVideoLayer()
    initVideoButton()

    this.captureSession.startRunning()
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun initCaptureSession(): Boolean {
    val videoInput = this.videoDevice?.let { AVCaptureDeviceInput(it, null) } ?: return false
    this.captureSession.addInput(videoInput)
    val audioInput = this.audioDevice?.let { AVCaptureDeviceInput(it, null) } ?: return false
    this.captureSession.addInput(audioInput)

    this.captureSession.addOutput(this.fileOutput)
    return true
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun initVideoLayer() {
    val (_, bottom) = safeAreaInsets()
    val naviHeight: CGFloat = 44.0
    val videoLayer = AVCaptureVideoPreviewLayer(session = this.captureSession)
    videoLayer.setFrame(
      CGRectMake(
        0.0,
        naviHeight,
        this.view.frame.toRect().width.toDouble(),
        this.view.frame.toRect().height.toDouble() - bottom - naviHeight,
      )
    )
    videoLayer.setVideoGravity(AVLayerVideoGravityResizeAspectFill)
    this.view.layer.addSublayer(videoLayer)
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun safeAreaInsets(): Pair<CGFloat, CGFloat> {
    val window = UIApplication.sharedApplication.keyWindow ?: return Pair(0.0, 0.0)
    return window.safeAreaInsets.useContents {
      Pair(this.top, this.bottom)
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun initTopView() {
    val height: CGFloat = 44.0
    val backTitle =
      SimpleI18nResource(Language.ZH to "返回", Language.EN to "Back")
    val button = UIButton(CGRectMake(16.0, 0.0, 60.0, height))
    button.setTitle(backTitle.text, UIControlStateNormal)
    button.setTitleColor(UIColor.whiteColor, UIControlStateNormal)
    button.addTarget(this, NSSelectorFromString("backAction"), UIControlEventTouchUpInside)
    this.view.addSubview(button)

    val timeLabelWidth: CGFloat = 100.0
    timeLabel = UILabel(
      CGRectMake(
        (this.view.frame.toRect().width.toDouble() - timeLabelWidth) * 0.5,
        0.0,
        timeLabelWidth,
        height
      )
    )
    timeLabel!!.textAlignment = NSTextAlignmentCenter
    timeLabel!!.textColor = UIColor.whiteColor
    timeLabel!!.text = "00:00:00"
    this.view.addSubview(timeLabel!!)
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun initVideoButton() {
    val button_width: CGFloat = 56.0
    val (_, bottom) = safeAreaInsets()

    val frame = CGRectMake(
      (this.view.frame.toRect().width.toDouble() - button_width) * 0.5,
      this.view.frame.toRect().height.toDouble() - bottom - button_width - 56,
      button_width,
      button_width
    )
    videoButton = VideoButton(frame = frame)
    videoButton!!.setUserInteractionEnabled(true)

    val onTap = UITapGestureRecognizer(
      target = this,
      action = NSSelectorFromString("clickVideoButton")
    )

    videoButton!!.addGestureRecognizer(onTap)
    this.view.addSubview(videoButton!!)
  }

  private fun initTimer() {
    var count = 0
    var minuteString: String
    var secondString: String
    timer = NSTimer.scheduledTimerWithTimeInterval(1.0, true) {
      count += 1
      if (count > this.maxDuration) {
        stopRecording()
      } else {
        val (minutes, seconds) = changeCountToTime(count)
        minuteString = if (minutes > 9) {
          "$minutes"
        } else {
          "0$minutes"
        }

        secondString = if (seconds > 9) {
          "$seconds"
        } else {
          "0$seconds"
        }
        this.timeLabel?.text = "00:$minuteString:$secondString"
      }
    }
  }

  private fun changeCountToTime(count: Int): Pair<Int, Int> {

    val minutes = count / 60
    val seconds = count % 60
    return Pair(minutes, seconds)
  }

  @OptIn(BetaInteropApi::class)
  @ObjCAction
  private fun backAction() {
    this@MediaVideoViewController.dismissViewControllerAnimated(true, null)
  }

  @OptIn(BetaInteropApi::class)
  @ObjCAction
  fun clickVideoButton() {
    isRecording = !isRecording
    videoButton?.updateInternalView(isRecording)
    if (isRecording) {
      initTimer()
      startRecording()
    } else {
      isClickVideoButton = true
      stopRecording()
    }
  }

  //开始录视频
  private fun startRecording() {
    val path = NSSearchPathForDirectoriesInDomains(
      NSDocumentDirectory,
      NSUserDomainMask,
      true
    ).first() as? String ?: ""

    val nowTime = datetimeNow()
    val filePath = "$path/$nowTime.mp4"
    val fileURL = NSURL(fileURLWithPath = filePath)
    this.fileOutput.startRecordingToOutputFileURL(fileURL, delegate)
  }

  //停止录视频
  private fun stopRecording() {
    fileOutput.stopRecording()
    this.isRecording = false
    this.timeLabel?.text = "00:00:00"
    this.timer?.invalidate()
    this.timer = null
  }

  inner class CaptureDelegate : NSObject(), AVCaptureFileOutputRecordingDelegateProtocol {

    var filePath = ""

    override fun captureOutput(
      output: AVCaptureFileOutput,
      didFinishRecordingToOutputFileAtURL: NSURL,
      fromConnections: List<*>,
      error: NSError?
    ) {
      var message = ""

      PHPhotoLibrary.sharedPhotoLibrary().performChanges({
        PHAssetChangeRequest.creationRequestForAssetFromVideoAtFileURL(
          didFinishRecordingToOutputFileAtURL
        )
      }) { isSuccess, err ->
        if (isSuccess) {
          message = "保存成功"
        } else {
          if (err != null) {
            message = "保存失败: ${err.localizedDescription}"
          }
        }
        if (!isClickVideoButton) {
          return@performChanges
        }
        filePath = didFinishRecordingToOutputFileAtURL.absoluteString.toString()
        dispatch_async(dispatch_get_main_queue()) {
          val alertController = UIAlertController.alertControllerWithTitle(
            message,
            null,
            UIAlertControllerStyleAlert
          )
          this@MediaVideoViewController.presentViewController(
            alertController,
            true,
            null
          )

          val delayTime: Double = 0.5 * NSEC_PER_SEC.toDouble()
          val after_time = dispatch_time(DISPATCH_TIME_NOW, delayTime.toLong())
          dispatch_after(after_time, dispatch_get_main_queue()) {
            alertController.dismissViewControllerAnimated(true, null)
          }
        }
      }
    }

  }

  private val delegate = CaptureDelegate()
}

