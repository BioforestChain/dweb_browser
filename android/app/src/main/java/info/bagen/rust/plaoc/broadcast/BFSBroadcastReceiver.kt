package info.bagen.rust.plaoc.broadcast

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import info.bagen.rust.plaoc.ui.app.AppViewIntent
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.browser.BrowserNMM.Companion.browserController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

enum class BFSBroadcastAction(val action: String) {
    BFSInstallApp("info.bagen.rust.plaoc.BFSInstallApp"),
}

@SuppressLint("RestrictedApi")
class BFSBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        GlobalScope.launch(Dispatchers.IO) {
            intent?.let {
                when (it.action) {
                    BFSBroadcastAction.BFSInstallApp.action -> {
                        browserController.activity?.let { mainActivity ->
                            val path = it.getStringExtra("path") ?: ""
                            mainActivity.getAppViewModel()
                                .handleIntent(AppViewIntent.BFSInstallApp(path))
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
