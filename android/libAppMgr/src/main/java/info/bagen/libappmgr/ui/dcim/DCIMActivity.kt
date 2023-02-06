package info.bagen.libappmgr.ui.dcim

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import info.bagen.libappmgr.system.media.MediaInfo
import info.bagen.libappmgr.ui.theme.AppMgrTheme
import info.bagen.libappmgr.ui.theme.ColorBackgroundBar
import info.bagen.libappmgr.utils.createMediaInfo
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import java.io.File

class DCIMActivity : ComponentActivity() {
    val dcimViewModel: DCIMViewModel by inject()

    companion object {
        const val REQUEST_DCIM_CODE = 168
        const val RESPONSE_DCIM_VALUE = "result"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 666
            )
        } else {
            contentLoad()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            666 -> contentLoad()
        }
    }

    private fun contentLoad() {
        // window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN) // 隐藏顶部状态栏
        window.statusBarColor = ColorBackgroundBar.toArgb() // // 修改状态栏颜色
        dcimViewModel.setCallback(callback = object : DCIMViewModel.CallBack {
            override fun send(fileList: ArrayList<String>) {
                // 接收返回的文件列表地址
                val mediaInfos = arrayListOf<MediaInfo>()
                fileList.forEach { path ->
                    File(path).createMediaInfo()?.let { mediaInfos.add(it) }
                }
                val bundle = Bundle()
                bundle.putSerializable(RESPONSE_DCIM_VALUE, mediaInfos)
                setResult(REQUEST_DCIM_CODE, Intent().putExtras(bundle))
                this@DCIMActivity.finish()
            }

            override fun cancel() {
                // 取消操作，等于关闭当前activity
                this@DCIMActivity.finish()
            }
        })
        setContent {
            AppMgrTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ColorBackgroundBar)
                ) {
                    DCIMGreeting(onGridClick = {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }, onViewerClick = {
                        showWindowStatusBar(dcimViewModel.uiState.value.showViewerBar.value)
                    }, dcimVM = dcimViewModel
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        if (dcimViewModel.uiState.value.showViewer.value) {
            dcimViewModel.handlerIntent(DCIMIntent.UpdateViewerState(false))
            showWindowStatusBar(true)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        //dcimViewModel.clearExoPlayerList()
        dcimViewModel.clearJobList()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    private fun showWindowStatusBar(show: Boolean) {
        if (show) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun DCIMGreeting(
    onGridClick: () -> Unit, onViewerClick: () -> Unit, dcimVM: DCIMViewModel = koinViewModel()
) {
    LaunchedEffect(Unit) {
        /*dcimVM.loadDCIMInfo { maps ->
          dcimVM.dcimMaps.putAll(maps)
          dcimVM.initDCIMInfoList()
        }*/
        dcimVM.handlerIntent(DCIMIntent.InitDCIMInfoList)
    }
    DCIMView(dcimVM, onGridClick, onViewerClick)
}
