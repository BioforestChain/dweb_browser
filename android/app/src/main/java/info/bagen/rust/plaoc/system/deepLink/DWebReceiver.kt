package info.bagen.rust.plaoc.system.deepLink

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import info.bagen.libappmgr.utils.APP_DIR_TYPE
import info.bagen.libappmgr.utils.FilesUtil
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.MainActivity
import info.bagen.rust.plaoc.WorkerNative
import info.bagen.rust.plaoc.createWorker
import java.io.File

class DWebReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_OPEN_DWEB = "action.plaoc.open_dwebview"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { it ->
            if (ACTION_OPEN_DWEB == it.action) {
                val appName = it.getStringExtra("AppName")
                val url = it.getStringExtra("URL")
                val callUrl =
                    "${App.appContext.dataDir.absolutePath}/${APP_DIR_TYPE.SystemApp}/$appName/${FilesUtil.DIR_HOME}/$url"
                if (File(callUrl).exists()) {
                    createWorker(WorkerNative.valueOf("DenoRuntime"), callUrl)
                } else {
                    context?.let { cc -> cc.startActivity(Intent(cc, MainActivity::class.java)) }
                    Toast.makeText(context, "请安装应用<$appName>", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
