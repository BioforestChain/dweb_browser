package info.bagen.rust.plaoc.microService.browser

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.google.gson.JsonSyntaxException
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.BLUETOOTH_CAN_BE_FOUND
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.BLUETOOTH_REQUEST
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.bluetoothOp
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.bluetooth_found
import info.bagen.rust.plaoc.microService.sys.plugin.permission.PermissionManager
import info.bagen.rust.plaoc.network.HttpClient
import info.bagen.rust.plaoc.network.base.byteBufferToString
import info.bagen.rust.plaoc.ui.app.AppViewModel
import info.bagen.rust.plaoc.ui.camera.QRCodeIntent
import info.bagen.rust.plaoc.ui.camera.QRCodeScanning
import info.bagen.rust.plaoc.ui.camera.QRCodeScanningView
import info.bagen.rust.plaoc.ui.camera.QRCodeViewModel
import info.bagen.rust.plaoc.ui.main.Home
import info.bagen.rust.plaoc.ui.main.MainViewModel
import info.bagen.rust.plaoc.ui.main.SearchAction
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import info.bagen.rust.plaoc.util.permission.EPermission
import info.bagen.rust.plaoc.util.permission.PermissionUtil
import kotlinx.coroutines.launch
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
        // tansocc.com
        // ua dweb-host/browser.android.dweb
        // file://dns.sys.dweb/install?url=.zip
        return appViewModel
    }

    private var remoteMmid by mutableStateOf("")
    private var controller: BrowserController? = BrowserNMM.browserController

    private fun upsetRemoteMmid() {
        remoteMmid = intent.getStringExtra("mmid") ?: return finish()
        controller?.activity = this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upsetRemoteMmid()
        App.browserActivity = this
        setContent {
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
                !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
            RustApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.primary)
                ) {
                    val coroutineScope = rememberCoroutineScope()
                    Home(mainViewModel, appViewModel, onSearchAction = { action, data ->
                        when (action) {
                            SearchAction.Search -> {
                                if (!checkJmmMetadataJson(data) { jmmMetadata, url ->
                                        // 先判断下是否是json结尾，如果是并获取解析json为jmmMetadata，失败就照常打开网页，成功打开下载界面
                                        coroutineScope.launch {
                                            BrowserNMM.browserController?.installJMM(jmmMetadata, url)
                                        }
                                    }) {
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
                                    PermissionManager.requestPermissions(
                                        this@BrowserActivity, EPermission.PERMISSION_CAMERA.type
                                    )
                                }
                            }
                        }
                    }, onOpenDWebview = { appId, dAppInfo ->
                        /// TODO 这里是点击桌面app触发的事件
                        coroutineScope.launch {
                            BrowserNMM.browserController.openApp(appId)
                        }
                    })
                    MultiDWebBrowserView(dWebBrowserModel = dWebBrowserModel)
                    QRCodeScanningView(this@BrowserActivity, qrCodeViewModel)
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
        } else if (requestCode == QRCodeScanning.CAMERA_PERMISSION_REQUEST_CODE) {
            grantResults.forEach {
                if (it != PackageManager.PERMISSION_GRANTED) return
            }
            qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true))
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true)
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        // 退出APP关闭服务
        super.onDestroy()
        App.browserActivity = null
        dWebBrowserModel.handleIntent(DWebBrowserIntent.RemoveALL)
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

    private fun checkJmmMetadataJson(
        url: String, openJmmActivity: (JmmMetadata, String) -> Unit
    ): Boolean {
        Uri.parse(url).lastPathSegment?.let { lastPathSegment ->
            if (lastPathSegment.endsWith(".json")) { // 如果是json，进行请求判断并解析jmmMetadata
                try {
                    gson.fromJson(
                        byteBufferToString(HttpClient().requestPath(url).body.payload),
                        JmmMetadata::class.java
                    ).apply { openJmmActivity(this, url) }

                    return true
                } catch (e: JsonSyntaxException) {
                    Log.e("DWebBrowserModel", "checkJmmMetadataJson fail -> ${e.message}")
                }
            }
        }
        return false
    }
}
