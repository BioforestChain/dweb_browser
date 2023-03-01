package info.bagen.rust.plaoc.broadcast

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.impl.utils.ForceStopRunnable.BroadcastReceiver
import info.bagen.libappmgr.ui.app.AppViewIntent
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.util.DwebBrowserUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

enum class BFSBroadcastAction(val action: String) {
    BFSInstallApp("info.bagen.rust.plaoc.BFSInstallApp"),
    DownLoadStatusChanged("action.download.status.changed"),
}

@SuppressLint("RestrictedApi")
class BFSBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        GlobalScope.launch(Dispatchers.IO) {
            intent?.let {
                when (it.action) {
                    BFSBroadcastAction.BFSInstallApp.action -> {
                        App.browserActivity?.let { mainActivity ->
                            val path = it.getStringExtra("path") ?: ""
                            mainActivity.getAppViewModel()
                                .handleIntent(AppViewIntent.BFSInstallApp(path))
                        }
                    }
                    BFSBroadcastAction.DownLoadStatusChanged.action -> {
                        it.getStringExtra("url")?.let { url ->
                            DwebBrowserUtil.INSTANCE.mBinderService?.invokeDownloadStatusChange(url)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
