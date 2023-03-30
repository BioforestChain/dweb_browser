package info.bagen.rust.plaoc.ui.browser

import androidx.compose.runtime.Composable
import info.bagen.rust.plaoc.ui.app.AppViewModel
import info.bagen.rust.plaoc.ui.main.Home
import info.bagen.rust.plaoc.ui.main.MainViewModel
import info.bagen.rust.plaoc.ui.main.SearchAction

@Composable
fun BrowserMainView() {
  Home(
    mainViewModel = MainViewModel(),
    appViewModel = AppViewModel(),
    onSearchAction = { action, data ->
      when (action) {
        SearchAction.Search -> {}
        SearchAction.OpenCamera -> {}
      }
    }, onOpenDWebview = { appId, dAppInfo ->
      // TODO 这里是点击桌面app触发的事件
    }
  )
}