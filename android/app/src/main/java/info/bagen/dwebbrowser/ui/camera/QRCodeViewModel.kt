package info.bagen.dwebbrowser.ui.camera

import android.annotation.SuppressLint
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class QRCodeUIState(
  val show: MutableState<Boolean> = mutableStateOf(false),
  var scanType: ScanType = ScanType.QRCODE
)

enum class ScanType {
  QRCODE, BARCODE
}

sealed class QRCodeIntent {
  object ReleaseCamera : QRCodeIntent()
  class OpenOrHide(val show: Boolean, val scanType: ScanType = ScanType.QRCODE) : QRCodeIntent()
  class SetQRCodeScanning(val qrCodeScanning: QRCodeScanning) : QRCodeIntent()
  open class SetOnScanCallBack(val scanCallBack: QrCodeCallBack) : QRCodeIntent()
}

interface OnScanCallBack {
  fun scanCallBack(value: String)
}

typealias QrCodeCallBack = (String) -> Unit

class QRCodeViewModel : ViewModel() {
  val uiState = QRCodeUIState()

  @SuppressLint("StaticFieldLeak")
  private var qrCodeScanning: QRCodeScanning? = null
  private var qrCodeCallBack: QrCodeCallBack? = null

  fun handleIntent(action: QRCodeIntent) {
    viewModelScope.launch(Dispatchers.IO) {
      when (action) {
        is QRCodeIntent.OpenOrHide -> {
          uiState.show.value = action.show
          uiState.scanType = action.scanType
          // 如果关闭扫码页面
          if (!action.show) {
            qrCodeCallBack?.let { it1 -> it1("") }
          }
        }
        is QRCodeIntent.SetQRCodeScanning -> {
          qrCodeScanning = action.qrCodeScanning.also {
            it.setCallBack(object : OnScanCallBack {
              override fun scanCallBack(value: String) {
                qrCodeCallBack?.let { it1 -> it1(value) }
              }
            })
          }
        }
        is QRCodeIntent.SetOnScanCallBack -> {
          qrCodeCallBack = action.scanCallBack
        }
        is QRCodeIntent.ReleaseCamera -> {
          qrCodeScanning?.releaseCamera()
          qrCodeScanning = null
        }
      }
    }
  }

  fun toggleTorch(state: Boolean) = qrCodeScanning?.toggleTorch(state)
  fun getTorchState() = qrCodeScanning?.mCameraScan?.isTorchEnabled
}