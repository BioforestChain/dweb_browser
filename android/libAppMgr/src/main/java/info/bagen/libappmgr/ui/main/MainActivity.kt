package info.bagen.libappmgr.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import info.bagen.libappmgr.data.PreferencesHelper
import info.bagen.libappmgr.entity.DAppInfoUI
import info.bagen.libappmgr.schedule.CoroutineUpdateTask
import info.bagen.libappmgr.ui.app.AppViewIntent
import info.bagen.libappmgr.ui.app.AppViewModel
import info.bagen.libappmgr.ui.theme.AppMgrTheme
import info.bagen.libappmgr.utils.FilesUtil

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppMgrTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.primary)
                ) {
                    Home(
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
    mainViewModel: MainViewModel = viewModel() as MainViewModel,
    appViewModel: AppViewModel = viewModel() as AppViewModel,
    onSearchAction: ((SearchAction, String) -> Unit)? = null,
    onOpenDWebview: ((appId: String, dAppInfo: DAppInfoUI?) -> Unit)? = null
) {
    LaunchedEffect(Unit) {
        if (PreferencesHelper.isFirstIn()) {
            FilesUtil.copyAssetsToRecommendAppDir()
            PreferencesHelper.saveFirstState(false)
        }
        // ????????????????????????app????????????remember-app???system-app????????????????????????
        // AppContextUtil.appInfoList.clear()
        // AppContextUtil.appInfoList.addAll(FilesUtil.getAppInfoList())
        appViewModel.handleIntent(AppViewIntent.LoadAppInfoList) // ???????????????
//        CoroutineUpdateTask().scheduleUpdate(1000 * 60) // ????????????
    }
    //AppInfoGridView(appInfoList = AppContextUtil.appInfoList, downModeDialog = true, onOpenApp = onOpenDWebview)
    //AppInfoGridView(appViewModel, onOpenApp = onOpenDWebview)
    MainView(mainViewModel, appViewModel, onSearchAction, onOpenDWebview)
}
