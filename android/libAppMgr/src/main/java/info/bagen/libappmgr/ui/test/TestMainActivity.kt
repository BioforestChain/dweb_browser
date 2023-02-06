package info.bagen.libappmgr.ui.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import info.bagen.libappmgr.ui.theme.AppMgrTheme
import info.bagen.libappmgr.utils.AppContextUtil
import info.bagen.libappmgr.utils.JsonUtil
import info.bagen.libappmgr.utils.saveString

class TestMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.primary)
            ) {
                AppMgrTheme {
                    var vm = viewModel() as TestViewModel
                    TestMainView(vm) {
                        vm.navUIType.value = NavUIType.WEBVIEW
                        vm.url.value = it.path
                        vm.showWebView.value = true
                        // 保存历史记录
                        vm.historyList.add(it)
                        AppContextUtil.sInstance!!.saveString(
                            key = "historyList",
                            JsonUtil.toJson(vm.historyList)
                        )
                    }
                }
            }
        }
    }
}
