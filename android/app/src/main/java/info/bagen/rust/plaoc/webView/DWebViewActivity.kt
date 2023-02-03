package info.bagen.rust.plaoc.webView


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import info.bagen.libappmgr.network.ApiService
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.App.Companion.dwebViewActivity
import info.bagen.rust.plaoc.TASK
import info.bagen.rust.plaoc.system.permission.PermissionManager
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import info.bagen.rust.plaoc.webView.jsutil.emitListenBackButton
import info.bagen.rust.plaoc.webView.urlscheme.CustomUrlScheme
import info.bagen.rust.plaoc.webView.urlscheme.requestHandlerFromAssets
import info.bagen.rust.plaoc.webkit.AdAndroidWebView
import info.bagen.rust.plaoc.webkit.rememberAdWebViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import kotlin.io.path.Path


private const val TAG = "DWebViewActivity"
var dWebView: AdAndroidWebView? = null

class DWebViewActivity : AppCompatActivity() {

    override fun onBackPressed() {
        Log.i(TAG, "parentActivityIntent:${this.parentActivityIntent}")
        // 触发回调监听事件
        emitListenBackButton()

        if (this.parentActivityIntent == null) {
            println("shutdownNow:${TASK?.isAlive}") // 结束线程任务
            super.onBackPressed()
        } else {
            this.startActivity(this.parentActivityIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dwebViewActivity = null
    }

    // 权限回调
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)// 开启调试
        // 设置装饰视图是否应适合WindowInsetsCompat(Describes a set of insets for window content.)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        dwebViewActivity = this
        setContent {
            ViewCompat.getWindowInsetsController(LocalView.current)?.isAppearanceLightStatusBars =
                !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
            RustApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background,
                ) {
                    NavFun(this)
                }
            }

        }
    }
}

@OptIn(
    ExperimentalMaterialNavigationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
private fun NavFun(activity: ComponentActivity) {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)

    ModalBottomSheetLayout(bottomSheetNavigator) {
        NavHost(navController = navController, startDestination = "https://{url}") {
            composable(
                "https://{url}",
                arguments = listOf(
                    navArgument("url") {
                        type = NavType.StringType
                    }
                ),
                deepLinks = listOf(navDeepLink {
                    uriPattern = "https://{url}"
                })
            ) { entry ->
                val url = entry.arguments?.getString("url")
                Log.d(TAG, "NavFun entry : $url")
                checkNotNull(url)
                // 请求文件路径
                val urlStr = URLDecoder.decode(url, "UTF-8")

                val host = Path(urlStr).getName(1).toString()
                val assetBasePath = "./"
                println("kotlin#DwebViewActivity NavFun host=$host, urlStr=$urlStr")
                // 设置规则
                val customUrlScheme = CustomUrlScheme(
                    "https", host,
                    requestHandlerFromAssets(LocalContext.current.assets, assetBasePath)
                )
                DWebView(
                    state = rememberAdWebViewState(urlStr),
                    navController = navController,
                    activity = activity,
                    modifier = Modifier.background(Color.Unspecified),
                    customUrlScheme = customUrlScheme,
                ) { webView ->
                    dWebView = webView
                    val workerHandle = "worker${Date().time}"
                    println("kotlin#JsMicroModule workerHandle==> $workerHandle")
                    val inputStream = App.appContext.assets.open("injectWorkerJs/injectWorker.js")
                    val byteArray = inputStream.readBytes()
                    val injectJs = String(byteArray)
                    val channel = webView.createWebMessageChannel()
                    channel[0].setWebMessageCallback(object :
                        WebMessagePort.WebMessageCallback() {
                        override fun onMessage(port: WebMessagePort, message: WebMessage) {
                            println("kotlin#JsMicroModuleport🍟message: ${message.data}")
                        }
                    })
                    // 构建注入的代码
                    val workerCode =
                        "data:utf-8,((module,exports=module.exports)=>{$injectJs;return module.exports})({exports:{}}).installEnv();console.log(\"ookkkkk, i'm in worker\");\n" +
                                "(async () => {\n" +
                                "    const view_id = await fetch(\"file://KEJPMHLA.mwebview.sys.dweb/open?origin=https://objectjson.waterbang.top/index.html\").then((res) => res.text());\n" +
                                "})()"
                    webView.evaluateJavascript("const $workerHandle = new Worker(`$workerCode`); \n") {
                        println("worker创建完成")
                    }
                    webView.evaluateJavascript(
                        "onmessage = function (e) {\n" +
                                "console.log(\"kotlin#DwebViewActivity port1111\", e.data, e.ports[0]); \n" +
                                "$workerHandle.postMessage([\"ipc-channel\", e.ports[0]], [e.ports[0]])\n" +
                                "}\n"
                    ) {
                        println("worker监听注册完成")
                    }
                    webView.postWebMessage(
                        WebMessage("fetch-ipc-channel", arrayOf(channel[1])),
                        Uri.EMPTY
                    )
                    channel[0].postMessage(WebMessage("xxx"))
                }
            }
        }
    }
}


/** 打开DWebview*/
fun openDWebWindow(activity: ComponentActivity, url: String) {
    val intent = Intent(activity.applicationContext, DWebViewActivity::class.java).also {
        println(
            "kotlin#DwebViewActivity openDWebWindow url:$url,${
                Uri.parse(
                    "https://" + URLEncoder.encode(
                        url,
                        "UTF-8"
                    )
                )
            }"
        )
        it.data = Uri.parse("https://" + URLEncoder.encode(url, "UTF-8"))
    }
    activity.startActivity(intent)
}




