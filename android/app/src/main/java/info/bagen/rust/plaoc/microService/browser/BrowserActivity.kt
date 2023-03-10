package info.bagen.rust.plaoc.microService.browser

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import com.king.mlkit.vision.camera.util.LogUtils
import com.king.mlkit.vision.camera.util.PermissionUtils
import info.bagen.rust.plaoc.util.permission.EPermission
import info.bagen.rust.plaoc.util.permission.PermissionUtil
import info.bagen.rust.plaoc.ui.app.AppViewModel
import info.bagen.rust.plaoc.ui.camera.QRCodeIntent
import info.bagen.rust.plaoc.ui.camera.QRCodeScanning
import info.bagen.rust.plaoc.ui.camera.QRCodeScanningView
import info.bagen.rust.plaoc.ui.camera.QRCodeViewModel
import info.bagen.rust.plaoc.ui.main.Home
import info.bagen.rust.plaoc.ui.main.MainViewModel
import info.bagen.rust.plaoc.ui.main.SearchAction
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.BLUETOOTH_CAN_BE_FOUND
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.BLUETOOTH_REQUEST
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.bluetoothOp
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.bluetooth_found
import info.bagen.rust.plaoc.microService.sys.plugin.permission.PermissionManager
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import info.bagen.rust.plaoc.webView.network.dWebView_host
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class BrowserActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_PHOTO = 1
        const val REQUEST_CODE_REQUEST_EXTERNAL_STORAGE = 2
    }

    var blueToothReceiver : BlueToothReceiver? = null
    fun getContext() = this
    val dWebBrowserModel: DWebBrowserModel by viewModel()
    val qrCodeViewModel: QRCodeViewModel by viewModel()
    private val appViewModel: AppViewModel by viewModel()
    private val mainViewModel: MainViewModel by viewModel()

    @JvmName("getAppViewModel1")
    fun getAppViewModel(): AppViewModel {
        // tansocc.com
        // ua dweb-host/browser.android.dweb
        // file://dns.sys.dweb/install?url=.zip
        return appViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrowserNMM.activityPo?.resolve(this)
        App.browserActivity = this
        setContent {
            ViewCompat.getWindowInsetsController(LocalView.current)?.isAppearanceLightStatusBars =
                !isSystemInDarkTheme() // ????????????????????????????????????
            RustApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.primary)
                ) {
                    Home(mainViewModel, appViewModel, onSearchAction = { action, data ->
                        LogUtils.d("????????????????????????$action--$data")
                        when (action) {
                            SearchAction.Search -> {
                                dWebBrowserModel.handleIntent(DWebBrowserIntent.OpenDWebBrowser(data))
                                //dWebBrowserModel.openDWebBrowser("https://shop.plaoc.com/bfs-metadata.json")
                            }
                            SearchAction.OpenCamera -> {
                                if (PermissionUtil.isPermissionsGranted(EPermission.PERMISSION_CAMERA.type)) {
                                    //openScannerActivity()
                                    qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true))
                                } else {
                                    PermissionManager.requestPermissions(
                                        this@BrowserActivity, EPermission.PERMISSION_CAMERA.type
                                    )
                                }
                            }
                        }
                    }, onOpenDWebview = { appId, dAppInfo ->
                        dWebView_host = appId
                        /// TODO ?????????????????????app???????????????
                        JmmNMM.nativeFetch(appId)
                    })
                    MultiDWebBrowserView(dWebBrowserModel = dWebBrowserModel)
                    QRCodeScanningView(this@BrowserActivity, qrCodeViewModel)
                }
            }
        }
    }


    // ???????????????????????????
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // ???????????????????????????
        if (requestCode == BLUETOOTH_REQUEST) {
            when (resultCode) {
                RESULT_OK -> bluetoothOp.resolve("success")
                RESULT_CANCELED -> bluetoothOp.resolve("Application for bluetooth rejected")
                else -> bluetoothOp.resolve("Application for bluetooth rejected")
            }
        }
        // ?????????????????????????????????
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
        if (requestCode == info.bagen.rust.plaoc.util.permission.PermissionManager.MY_PERMISSIONS) {
            info.bagen.rust.plaoc.util.permission.PermissionManager(this@BrowserActivity)
                .onRequestPermissionsResult(requestCode,
                    permissions,
                    grantResults,
                    object :
                        info.bagen.rust.plaoc.util.permission.PermissionManager.PermissionCallback {
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
        } else if (requestCode == REQUEST_CODE_REQUEST_EXTERNAL_STORAGE && PermissionUtils.requestPermissionsResult(
                Manifest.permission.READ_EXTERNAL_STORAGE, permissions, grantResults
            )
        ) {
            startPickPhoto()
        } else if (requestCode == QRCodeScanning.CAMERA_PERMISSION_REQUEST_CODE) {
            grantResults.forEach {
                if (it != PackageManager.PERMISSION_GRANTED) return
            }
            qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true))
        }
    }


    override fun onDestroy() {
        // ??????APP????????????
        super.onDestroy()
        App.browserActivity = null
        dWebBrowserModel.handleIntent(DWebBrowserIntent.RemoveALL)
        blueToothReceiver?.let { unregisterReceiver(it) }
        blueToothReceiver = null
    }

    // ????????????
    private fun startPickPhoto() {
        val pickIntent = Intent(
            Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(pickIntent, REQUEST_CODE_PHOTO)
    }

    // ??????????????????
    class BlueToothReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val result = mutableListOf<BluetoothNMM.BluetoothTargets>()
            when (intent.action) {
                // ????????????
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    // ????????????
                    if (ActivityCompat.checkSelfPermission(
                            App.appContext,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: ???????????????????????????
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
