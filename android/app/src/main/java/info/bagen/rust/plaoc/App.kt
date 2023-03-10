package info.bagen.rust.plaoc

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import info.bagen.rust.plaoc.di.repositoryModule
import info.bagen.rust.plaoc.di.appModules
import info.bagen.rust.plaoc.microService.browser.BrowserActivity
import info.bagen.rust.plaoc.util.DwebBrowserUtil
import info.bagen.rust.plaoc.util.PlaocUtil
import info.bagen.rust.plaoc.webView.DWebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class App : Application() {
    companion object {
        lateinit var appContext: Context

        var browserActivity: BrowserActivity? = null
        var dwebViewActivity: DWebViewActivity? = null

        fun <T> startActivity(cls: Class<T>, onIntent: (intent: Intent) -> Unit) {
            GlobalScope.launch(Dispatchers.IO) {
                val intent = Intent(appContext, cls)
                onIntent(intent)
                appContext.startActivity(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@App)
            androidFileProperties()
            modules(appModules, repositoryModule)
        }
        PlaocUtil.addShortcut(this) // 添加桌面快捷方式
        // startService(Intent(this@App, DwebBrowserService::class.java))
        DwebBrowserUtil.INSTANCE.bindDwebBrowserService()
    }

    private class ActivityLifecycleCallbacksImp : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        }

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
            // android10中规定, 只有默认输入法(IME)或者是目前处于焦点的应用, 才能访问到剪贴板数据，所以要延迟到聚焦后
            /*activity.window.decorView.post {
                GlobalScope.launch { ClipboardUtil.readAndParsingClipboard(activity) }
            }*/
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
        }
    }
}

