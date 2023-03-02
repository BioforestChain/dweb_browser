package info.bagen.rust.plaoc.microService.browser

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Resources.NotFoundException
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
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
import com.google.mlkit.vision.barcode.Barcode
import com.king.app.dialog.AppDialog
import com.king.app.dialog.AppDialogConfig
import com.king.mlkit.vision.barcode.BarcodeDecoder
import com.king.mlkit.vision.camera.CameraScan
import com.king.mlkit.vision.camera.analyze.Analyzer.OnAnalyzeListener
import com.king.mlkit.vision.camera.util.LogUtils
import com.king.mlkit.vision.camera.util.PermissionUtils
import info.bagen.libappmgr.system.permission.EPermission
import info.bagen.libappmgr.system.permission.PermissionUtil
import info.bagen.libappmgr.ui.app.AppViewModel
import info.bagen.libappmgr.ui.camera.QRCodeIntent
import info.bagen.libappmgr.ui.camera.QRCodeScanning
import info.bagen.libappmgr.ui.camera.QRCodeScanningView
import info.bagen.libappmgr.ui.camera.QRCodeViewModel
import info.bagen.libappmgr.ui.main.Home
import info.bagen.libappmgr.ui.main.MainViewModel
import info.bagen.libappmgr.ui.main.SearchAction
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.R
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.BLUETOOTH_CAN_BE_FOUND
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.BLUETOOTH_REQUEST
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.bluetoothOp
import info.bagen.rust.plaoc.microService.sys.plugin.device.BluetoothNMM.Companion.bluetooth_found
import info.bagen.rust.plaoc.microService.sys.plugin.permission.PermissionManager
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import info.bagen.rust.plaoc.util.lib.drawRect
import info.bagen.rust.plaoc.webView.network.dWebView_host
import info.bagen.rust.plaoc.webView.openDWebWindow
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class BrowserActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_PHOTO = 1
        const val REQUEST_CODE_REQUEST_EXTERNAL_STORAGE = 2
        const val REQUEST_CODE_SCAN_CODE = 3
    }

    var isQRCode = false //是否是识别二维码
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
                !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
            RustApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.primary)
                ) {
                    Home(mainViewModel, appViewModel, onSearchAction = { action, data ->
                        LogUtils.d("搜索框内容响应：$action--$data")
                        when (action) {
                            SearchAction.Search -> {
                                //openDWebWindow(this@MainActivity, data)
                                dWebBrowserModel.openDWebBrowser(data)
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
                        /// TODO 这里是点击桌面app触发的事件
//                        println("kotlin#onCreate 启动了DwebView ：$dWebView_host,worker_id：$workerResponse")
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
        // 相册返回
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_PHOTO -> processPhoto(data)
                REQUEST_CODE_SCAN_CODE -> processScanResult(data)
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == info.bagen.libappmgr.system.permission.PermissionManager.MY_PERMISSIONS) {
            info.bagen.libappmgr.system.permission.PermissionManager(this@BrowserActivity)
                .onRequestPermissionsResult(requestCode,
                    permissions,
                    grantResults,
                    object :
                        info.bagen.libappmgr.system.permission.PermissionManager.PermissionCallback {
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
        // 退出APP关闭服务
        super.onDestroy()
        App.browserActivity = null
        dWebBrowserModel.handleIntent(DWebBrowserIntent.RemoveALL)
        unregisterReceiver(receiver)
    }

    // 扫码后显示一下Toast
    private fun processScanResult(data: Intent?) {
        val text = CameraScan.parseScanResult(data)
        // Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        if (text?.startsWith("http") == true) {
            dWebView_host = "http"
            openDWebViewActivity(text)
        }
    }

    // 显示扫码的结果，是显示一张图片
    private fun processPhoto(data: Intent?) {
        data?.let {
            try {
                val src = MediaStore.Images.Media.getBitmap(contentResolver, it.data)
                BarcodeDecoder.process(src, object : OnAnalyzeListener<List<Barcode>?> {
                    override fun onSuccess(result: List<Barcode>) {
                        if (result.isNotEmpty()) {
                            val buffer = StringBuilder()
                            val bitmap = src.drawRect { canvas, paint ->
                                for ((index, data) in result.withIndex()) {
                                    buffer.append("[$index] ").append(data.displayValue)
                                        .append("\n")
                                    data.boundingBox?.let { it1 -> canvas.drawRect(it1, paint) }
                                }
                            }

                            val config =
                                AppDialogConfig(getContext(), R.layout.barcode_result_dialog)
                            config.setContent(buffer).setHideCancel(true).setOnClickOk {
                                AppDialog.INSTANCE.dismissDialog()
                            }
                            val imageView = config.getView<ImageView>(R.id.ivDialogContent)
                            imageView.setImageBitmap(bitmap)
                            AppDialog.INSTANCE.showDialog(config)
                        } else {
                            LogUtils.d("result is null")
                            Toast.makeText(getContext(), "result is null", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    override fun onFailure() {
                        LogUtils.d("onFailure")
                        Toast.makeText(getContext(), "onFailure", Toast.LENGTH_SHORT).show()
                    }
                    //如果指定具体的识别条码类型，速度会更快
                }, if (isQRCode) Barcode.FORMAT_QR_CODE else Barcode.FORMAT_ALL_FORMATS)
            } catch (e: Throwable) {
                e.printStackTrace()
                Toast.makeText(getContext(), e.message, Toast.LENGTH_SHORT).show()
            }

        }
    }

    // 相册的二维码
    private fun pickPhotoClicked(isQRCode: Boolean) {
        this.isQRCode = isQRCode
        if (PermissionUtils.checkPermission(
                getContext(), Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            startPickPhoto()
        } else {
            PermissionUtils.requestPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                REQUEST_CODE_REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    // 打开相册
    private fun startPickPhoto() {
        val pickIntent = Intent(
            Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(pickIntent, REQUEST_CODE_PHOTO)
    }


    fun openDWebViewActivity(path: String) {
        // 存储一下host，用来判断是远程的还是本地的
        if (dWebView_host == "") {
            throw NotFoundException("app host not found!")
        }
        LogUtils.d("启动了DWebView:url=$path")
        openDWebWindow(
            activity = getContext(), url = path // url
        )
    }

    // 创建查找对象
    val receiver = object : BroadcastReceiver() {
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
