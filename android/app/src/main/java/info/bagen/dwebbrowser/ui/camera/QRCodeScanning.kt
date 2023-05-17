package info.bagen.dwebbrowser.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Point
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.barcode.Barcode
import com.king.mlkit.vision.barcode.ViewfinderView
import com.king.mlkit.vision.barcode.analyze.BarcodeScanningAnalyzer
import com.king.mlkit.vision.barcode.utils.PointUtils
import com.king.mlkit.vision.camera.BaseCameraScan
import com.king.mlkit.vision.camera.CameraScan
import com.king.mlkit.vision.camera.analyze.Analyzer
import com.king.mlkit.vision.camera.config.ResolutionCameraConfig
import com.king.mlkit.vision.camera.util.PermissionUtils
import info.bagen.dwebbrowser.R

private val enterAnimator = slideInHorizontally(animationSpec = tween(500),//动画时长1s
  initialOffsetX = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  })
private val exitAnimator = slideOutHorizontally(animationSpec = tween(500),//动画时长1s
  targetOffsetX = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  })

@Composable
fun QRCodeScanningView(
  activity: FragmentActivity, qrCodeViewModel: QRCodeViewModel
) {
  AnimatedVisibility(
    visible = qrCodeViewModel.uiState.show.value, enter = enterAnimator, exit = exitAnimator
  ) {
    BackHandler { qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(false)) }
    DisposableEffect(Box(modifier = Modifier.fillMaxSize()) {
      AndroidView(factory = {
        QRCodeScanning(activity, qrCodeViewModel.uiState.scanType).apply {
          qrCodeViewModel.handleIntent(QRCodeIntent.SetQRCodeScanning(this))
        }
      })
    }) {
      onDispose {
        qrCodeViewModel.handleIntent(QRCodeIntent.ReleaseCamera)
      }
    }
  }
}

private typealias ScanCallback = List<Barcode>

@SuppressLint("ViewConstructor")
class QRCodeScanning(val activity: FragmentActivity, val scanType: ScanType) :
  FrameLayout(activity) {

  companion object {
    const val CAMERA_PERMISSION_REQUEST_CODE = 0x88
  }

  private lateinit var previewView: PreviewView // 显示照相机拍摄内容
  private lateinit var ivFlashlight: ImageView // 显示照相机拍摄内容
  private lateinit var ivResult: ImageView // 用于显示定格的照片
  private lateinit var viewfinderView: ViewfinderView
  lateinit var mCameraScan: CameraScan<ScanCallback> // 拍摄内容返回
  private var onScanCallBack: OnScanCallBack? = null

  init {
    inflate(activity, R.layout.qrcode_scan_activity, this)
    initViews()
    initEvents()
    initCameraScan()
    startCamera()
  }

  private fun initViews() {
    // 初始化 PreviewView 相机预览
    previewView = findViewById(R.id.previewView)
    ivFlashlight = findViewById(R.id.ivFlashlight)
    viewfinderView = findViewById(R.id.viewfinderView)
    ivResult = findViewById(R.id.ivResult)
  }

  private fun initEvents() {
    findViewById<LinearLayout>(R.id.btn_scan_light).setOnClickListener { toggleTorchState() }
  }

  private fun initCameraScan() {
    mCameraScan = createCameraScan(previewView) as CameraScan<ScanCallback>
    mCameraScan.setAnalyzer(createAnalyzer())
    mCameraScan.setPlayBeep(true).setVibrate(true)
    if (scanType == ScanType.QRCODE) {
      mCameraScan.bindFlashlightView(ivFlashlight)
    } else {
      mCameraScan.setCameraConfig(ResolutionCameraConfig(activity))//设置CameraConfig
    }
    mCameraScan.setOnScanResultCallback { result ->
      mCameraScan.setAnalyzeImage(false)
      val results = result.result

      //取预览当前帧图片并显示，为结果点提供参照
      ivResult.setImageBitmap(previewView.bitmap)
      val points = ArrayList<Point>()
      results.forEachIndexed { _, barcode ->
        //将实际的结果中心点坐标转换成界面预览的坐标
        barcode.boundingBox?.let { rect ->
          PointUtils.transform(
            rect.centerX(),
            rect.centerY(),
            result.bitmap.width,
            result.bitmap.height,
            viewfinderView.width,
            viewfinderView.height
          )
        }?.also {
          points.add(it)
        }
      }
      //设置Item点击监听
      viewfinderView.setOnItemClickListener {
        // 显示点击Item将所在位置扫码识别的结果返回
        // val intent = Intent()
        // val data = results[it].displayValue
        // intent.putExtra(CameraScan.SCAN_RESULT, data)
        // 将值返回，并关闭扫描界面
        results[it].displayValue?.let { data ->
          onScanCallBack?.scanCallBack(data)
        }
      }
      //显示结果点信息
      viewfinderView.showResultPoints(points)
    }
  }

  fun setCallBack(onScanCallBack: OnScanCallBack) {
    this.onScanCallBack = onScanCallBack
  }

  /**
   * 启动相机预览
   */
  private fun startCamera() {
    if (PermissionUtils.checkPermission(activity, Manifest.permission.CAMERA)) {
      mCameraScan.startCamera()
    } else {
      Log.d("QRCodeScanning", "checkPermissionResult != PERMISSION_GRANTED")
      PermissionUtils.requestPermission(
        activity, Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE
      )
    }
  }

  /**
   * 释放相机
   */
  fun releaseCamera() {
    mCameraScan.release()
  }

  /**
   * 创建[CameraScan]
   * @param previewView
   * @return
   */
  private fun createCameraScan(previewView: PreviewView): CameraScan<ScanCallback?> {
    return BaseCameraScan<ScanCallback>(activity, previewView)
  }

  /**
   * 创建分析器
   * @return
   */
  private fun createAnalyzer(): Analyzer<ScanCallback?> {
    return when (scanType) {
      ScanType.QRCODE -> BarcodeScanningAnalyzer(Barcode.FORMAT_QR_CODE)
      else -> BarcodeScanningAnalyzer(Barcode.FORMAT_ALL_FORMATS)
    }
  }

  fun toggleTorch(state: Boolean) {
    if (mCameraScan.isTorchEnabled != state) toggleTorchState()
  }

  /**
   * 切换闪光灯状态（开启/关闭）
   */
  private fun toggleTorchState() {
    val isTorch: Boolean = mCameraScan.isTorchEnabled
    mCameraScan.enableTorch(!isTorch)
    ivFlashlight.isSelected = !isTorch
  }
}

