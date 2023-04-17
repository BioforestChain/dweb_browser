package info.bagen.dwebbrowser.microService.browser

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import info.bagen.dwebbrowser.ActualBranch
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.BrowserNMM.Companion.browserController
import info.bagen.dwebbrowser.microService.sys.plugin.device.BluetoothNMM
import info.bagen.dwebbrowser.microService.sys.plugin.device.BluetoothNMM.Companion.BLUETOOTH_CAN_BE_FOUND
import info.bagen.dwebbrowser.microService.sys.plugin.device.BluetoothNMM.Companion.BLUETOOTH_REQUEST
import info.bagen.dwebbrowser.microService.sys.plugin.device.BluetoothNMM.Companion.bluetoothOp
import info.bagen.dwebbrowser.microService.sys.plugin.device.BluetoothNMM.Companion.bluetooth_found
import info.bagen.dwebbrowser.ui.app.AppViewModel
import info.bagen.dwebbrowser.ui.browser.BrowserView
import info.bagen.dwebbrowser.ui.camera.QRCodeIntent
import info.bagen.dwebbrowser.ui.camera.QRCodeScanning
import info.bagen.dwebbrowser.ui.camera.QRCodeScanningView
import info.bagen.dwebbrowser.ui.camera.QRCodeViewModel
import info.bagen.dwebbrowser.ui.loading.LoadingView
import info.bagen.dwebbrowser.ui.main.Home
import info.bagen.dwebbrowser.ui.main.MainViewModel
import info.bagen.dwebbrowser.ui.main.SearchAction
import info.bagen.dwebbrowser.ui.theme.RustApplicationTheme
import info.bagen.dwebbrowser.util.permission.EPermission
import info.bagen.dwebbrowser.util.permission.PermissionManager
import info.bagen.dwebbrowser.util.permission.PermissionUtil
import java.util.*

class BrowserActivity : AppCompatActivity() {
  var blueToothReceiver: BlueToothReceiver? = null
  fun getContext() = this
  val qrCodeViewModel: QRCodeViewModel = QRCodeViewModel()
  private val dWebBrowserModel: DWebBrowserModel = DWebBrowserModel()
  private val appViewModel: AppViewModel = AppViewModel()
  private val mainViewModel: MainViewModel = MainViewModel()

  @JvmName("getAppViewModel1")
  fun getAppViewModel(): AppViewModel {
    return appViewModel
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    browserController.activity = this
    setContent {
      WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
        !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
      RustApplicationTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.primary)
        ) {
          if (ActualBranch) {
            BrowserView(viewModel = browserController.browserViewModel)
          } else {
            Home(mainViewModel, appViewModel, onSearchAction = { action, data ->
              when (action) {
                SearchAction.Search -> {
                  if (!BrowserNMM.browserController.checkJmmMetadataJson(data)) {
                    dWebBrowserModel.handleIntent(
                      DWebBrowserIntent.OpenDWebBrowser(data)
                    )
                  }
                }
                SearchAction.OpenCamera -> {
                  if (PermissionUtil.isPermissionsGranted(EPermission.PERMISSION_CAMERA.type)) {
                    //openScannerActivity()
                    qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true))
                  } else {
                    info.bagen.dwebbrowser.microService.sys.plugin.permission.PermissionManager.requestPermissions(
                      this@BrowserActivity, EPermission.PERMISSION_CAMERA.type
                    )
                  }
                }
              }
            }, onOpenDWebview = { appId, dAppInfo ->
              /// TODO 这里是点击桌面app触发的事件
              dWebBrowserModel.handleIntent(DWebBrowserIntent.OpenDWebBrowser(appId))
              //coroutineScope.launch {
              //    BrowserNMM.browserController.showLoading.value = true
              //    BrowserNMM.browserController.openApp(appId)
              //}
            })
            MultiDWebBrowserView(dWebBrowserModel = dWebBrowserModel)
          }
          QRCodeScanningView(this@BrowserActivity, qrCodeViewModel)
          LoadingView(BrowserNMM.browserController.showLoading)
        }
      }
    }
  }


  // 选择图片后回调到这
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    // 申请蓝牙启动的返回
    if (requestCode == BLUETOOTH_REQUEST) {
      when (resultCode) {
        RESULT_OK -> bluetoothOp.resolve("success")
        RESULT_CANCELED -> bluetoothOp.resolve("Application for bluetooth rejected")
        else -> bluetoothOp.resolve("Application for bluetooth rejected")
      }
    }
    // 启动蓝牙可以发现的返回
    if (requestCode == BLUETOOTH_CAN_BE_FOUND) {
      when (resultCode) {
        RESULT_OK -> bluetooth_found.resolve("success")
        RESULT_CANCELED -> bluetooth_found.resolve("rejected")
        else -> bluetooth_found.resolve("rejected")
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<out String>, grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == PermissionManager.MY_PERMISSIONS) {
      PermissionManager(this@BrowserActivity)
        .onRequestPermissionsResult(requestCode,
          permissions,
          grantResults,
          object :
            PermissionManager.PermissionCallback {
            override fun onPermissionGranted(
              permissions: Array<out String>, grantResults: IntArray
            ) {
              // openScannerActivity()
              qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true))
            }

            override fun onPermissionDismissed(permission: String) {
            }

            override fun onNegativeButtonClicked(dialog: DialogInterface, which: Int) {
            }

            override fun onPositiveButtonClicked(dialog: DialogInterface, which: Int) {
              PermissionUtil.openAppSettings()
            }
          })
    } else if (requestCode == QRCodeScanning.CAMERA_PERMISSION_REQUEST_CODE) {
      grantResults.forEach {
        if (it != PackageManager.PERMISSION_GRANTED) return
      }
      qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true))
    }
  }

/*    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.repeatCount == 0) {
            if (BrowserNMM.browserController.hasDwebView) {
                if (event.action == KeyEvent.ACTION_DOWN) BrowserNMM.browserController.removeLastView()
                return true
            } else if (!BrowserNMM.browserController.showLoading.value) {
                moveTaskToBack(true)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }*/

  override fun onStop() {
    super.onStop()
    if (BrowserNMM.browserController.showLoading.value) { // 如果已经跳转了，这边直接改为隐藏
      BrowserNMM.browserController.showLoading.value = false
    }
  }

  override fun onDestroy() {
    // 退出APP关闭服务
    super.onDestroy()
    browserController.activity = null
    if (ActualBranch) {
      dWebBrowserModel.handleIntent(DWebBrowserIntent.RemoveDWebBrowser)
    } else {
      dWebBrowserModel.handleIntent(DWebBrowserIntent.RemoveALL)
    }
    blueToothReceiver?.let { unregisterReceiver(it) }
    blueToothReceiver = null
  }

  // 创建查找对象
  class BlueToothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val result = mutableListOf<BluetoothNMM.BluetoothTargets>()
      when (intent.action) {
        // 查找设备
        BluetoothDevice.ACTION_FOUND -> {
          val device: BluetoothDevice? =
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
          // 权限判断
          if (ActivityCompat.checkSelfPermission(
              App.appContext,
              Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
          ) {
            // TODO: 处理没有权限的情况
            return
          }
          device?.let {
            result.add(
              BluetoothNMM.BluetoothTargets(
                it.name,
                it.address,
                it.uuids[0].uuid
              )
            )
          }
        }
      }
      BluetoothNMM.findBluetoothResult.resolve(result)
    }
  }

}
