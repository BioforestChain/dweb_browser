package info.bagen.dwebbrowser.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.ui.app.AppViewIntent
import info.bagen.dwebbrowser.ui.app.AppViewModel
import info.bagen.dwebbrowser.ui.entity.DAppInfoUI
import info.bagen.dwebbrowser.ui.theme.RustApplicationTheme
import info.bagen.dwebbrowser.util.FilesUtil
import info.bagen.dwebbrowser.util.KEY_FIRST_LAUNCH
import info.bagen.dwebbrowser.util.getBoolean
import info.bagen.dwebbrowser.util.saveBoolean

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      RustApplicationTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
        ) {
          Home(
            mainViewModel = MainViewModel(),
            appViewModel = AppViewModel(),
            onSearchAction = { action, data ->
              Toast.makeText(
                this@MainActivity,
                "onSearchAction($action->$data)",
                Toast.LENGTH_SHORT
              ).show()
            },
            onOpenDWebview = { appId, dAppInfo ->
              Toast.makeText(
                this@MainActivity,
                "onOpenDWebview($appId->$dAppInfo)",
                Toast.LENGTH_SHORT
              ).show()
            }
          )
        }
      }
    }
  }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun Home(
  mainViewModel: MainViewModel,
  appViewModel: AppViewModel,
  onSearchAction: ((SearchAction, String) -> Unit)? = null,
  onOpenDWebview: ((appId: String, dAppInfo: DAppInfoUI?) -> Unit)? = null
) {
  LaunchedEffect(Unit) {
    println(
      "App.appContext.getBoolean(KEY_FIRST_LAUNCH, true): ${
        App.appContext.getBoolean(
          KEY_FIRST_LAUNCH, true
        )
      }"
    )
    if (App.appContext.getBoolean(KEY_FIRST_LAUNCH, true)) {
      FilesUtil.copyAssetsToRecommendAppDir()
      App.appContext.saveBoolean(KEY_FIRST_LAUNCH, false)
    }
    // 拷贝完成后，通过app目录下的remember-app和system-app获取最新列表数据
    // AppContextUtil.appInfoList.clear()
    // AppContextUtil.appInfoList.addAll(FilesUtil.getAppInfoList())
    appViewModel.handleIntent(AppViewIntent.LoadAppInfoList) // 新增的加载
//        CoroutineUpdateTask().scheduleUpdate(1000 * 60) // 轮询执行
  }
  //AppInfoGridView(appInfoList = AppContextUtil.appInfoList, downModeDialog = true, onOpenApp = onOpenDWebview)
  //AppInfoGridView(appViewModel, onOpenApp = onOpenDWebview)
  MainView(mainViewModel, appViewModel, onSearchAction, onOpenDWebview)
}
