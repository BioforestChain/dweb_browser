package org.dweb_browser.shared.microService.sys.deepLink


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.dweb_browser.helper.APP_DIR_TYPE
import org.dweb_browser.helper.FilesUtil
import android.widget.Toast
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
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
                    "${NativeMicroModule.getAppContext().dataDir.absolutePath}/${APP_DIR_TYPE.SystemApp}/$appName/${FilesUtil.DIR_HOME}/$url"
                if (File(callUrl).exists()) {
//                    createWorker(WorkerNative.valueOf("DenoRuntime"), callUrl)
                } else {
                    //context?.let { cc -> cc.startActivity(Intent(cc, BrowserActivity::class.java)) }
                    Toast.makeText(context, "请安装应用<$appName>", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}