package info.bagen.rust.plaoc.broadcast

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.impl.utils.ForceStopRunnable.BroadcastReceiver
import info.bagen.libappmgr.ui.app.AppViewIntent
import info.bagen.rust.plaoc.App

enum class BFSBroadcastAction(val action: String) {
    BFSInstallApp("info.bagen.rust.plaoc.BFSInstallApp")
}

@SuppressLint("RestrictedApi")
class BFSBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        intent?.let {
            Log.d("BFSBroadcastReceiver", "onReceive action=${it.action}")
            when (it.action) {
                BFSBroadcastAction.BFSInstallApp.action -> {
                    App.mainActivity?.let { mainActivity ->
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
