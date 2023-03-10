package info.bagen.rust.plaoc.ui.dcim

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
import info.bagen.rust.plaoc.ui.entity.MediaInfo
import info.bagen.rust.plaoc.ui.theme.ColorBackgroundBar
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import info.bagen.rust.plaoc.util.createMediaInfo
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
          Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
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
    // window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN) // ?????????????????????
    window.statusBarColor = ColorBackgroundBar.toArgb() // // ?????????????????????
    dcimViewModel.setCallback(callback = object : DCIMViewModel.CallBack {
      override fun send(fileList: ArrayList<String>) {
        // ?????????????????????????????????
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
        // ?????????????????????????????????activity
        this@DCIMActivity.finish()
      }
    })
    setContent {
      RustApplicationTheme {
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
