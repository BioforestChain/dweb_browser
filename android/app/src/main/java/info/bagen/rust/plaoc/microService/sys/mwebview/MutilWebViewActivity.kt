package info.bagen.rust.plaoc.microService.sys.mwebview

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.browser.*
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.SystemUIState
import info.bagen.rust.plaoc.microService.sys.plugin.systemui.SystemUiPlugin
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.min


open class PermissionActivity : AppCompatActivity() {

    companion object {
        private val requestPermissionsResultMap = mutableMapOf<Int, RequestPermissionsResult>()
        private var requestPermissionsCodeAcc = 1;
    }

    class RequestPermissionsResult(val code: Int) {
        val grants = mutableListOf<String>()
        val denied = mutableListOf<String>()
        private val task = PromiseOut<Unit>()
        fun done() {
            task.resolve(Unit)
        }

        val isGranted get() = denied.size == 0

        suspend fun waitPromise() = task.waitPromise()
    }

    suspend fun requestPermissions(permissions: Array<String>): RequestPermissionsResult {
        val result = RequestPermissionsResult(requestPermissionsCodeAcc++)

        val shouldRequestPermissions = permissions
//            permissions.filter {
//                ActivityCompat.shouldShowRequestPermissionRationale(this, it).also { isDenied ->
//                    if (isDenied) {
//                        result.denied.add(it)
//                    }
//                }
//            }.toTypedArray()
        if (shouldRequestPermissions.isNotEmpty()) {
            requestPermissionsResultMap[result.code] = result
            runOnUiThread {
                ActivityCompat.requestPermissions(
                    this,
                    shouldRequestPermissions,
                    result.code
                )
            }
        } else {
            result.done()
        }

        result.waitPromise()
        return result
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        requestPermissionsResultMap.remove(requestCode)?.also { result ->
            grantResults.forEachIndexed { index, p ->
                if (p == PackageManager.PERMISSION_GRANTED) {
                    result.grants.add(permissions[index])
                } else {
                    result.denied.add(permissions[index])
                }
            }
            result.done()
        }
    }
}

open class MutilWebViewActivity : PermissionActivity() {


    private var remoteMmid by mutableStateOf("")
    private var controller: MutilWebViewController? = null

    var systemUiPlugin: SystemUiPlugin? = null
    private fun upsetRemoteMmid() {
        remoteMmid = intent.getStringExtra("mmid")
            ?: return finish()
        controller?.activity = null

        controller = MultiWebViewNMM.controllerMap[remoteMmid]?.also { it.activity = this }
            ?: throw Exception("no found controller by mmid:$remoteMmid")
    }

    override fun onResume() {
        super.onResume()
        upsetRemoteMmid()
    }

    override fun onRestart() {
        super.onRestart()
        upsetRemoteMmid()

    }


    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upsetRemoteMmid()


        // This will lay out our app behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {

            val systemUiController = rememberSystemUiController()
            systemUiController.setSystemBarsColor(Color.Transparent)

            RustApplicationTheme {
                val wc by remember(remoteMmid) { mutableStateOf(controller) }

                val viewItem = wc?.webViewList?.lastOrNull()

                // if (controller == null) return@RustApplicationTheme
                // TODO !!
                val systemUIState = SystemUIState.Default(activity = controller?.activity!!)

                var overlayOffset = IntOffset(0, 0)
                val overlayPadding = WindowInsets(0).let {
                    var res = it
                    if (!systemUIState.statusBar.overlay.value) {
                        res = res.add(WindowInsets.statusBars)
                    }
                    if (!systemUIState.virtualKeyboard.overlay.value && WindowInsets.isImeVisible) {
                        // it.add(WindowInsets.ime) // ime本身就包含了navigationBars的高度
                        overlayOffset =
                            IntOffset(
                                0, min(
                                    0, -WindowInsets.ime.getBottom(LocalDensity.current)
                                            + WindowInsets.navigationBars.getBottom(LocalDensity.current)
                                )
                            )
                    } else if (!systemUIState.navigationBar.overlay.value) {
                        res = res.add(WindowInsets.navigationBars)
                    }
                    res
                }.asPaddingValues()

                if (viewItem != null) key(viewItem.webviewId) {
                    Box(
                        modifier = Modifier
                            .padding(overlayPadding)
                            .offset { overlayOffset }
                            .fillMaxSize()
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                if (controller != null) {
                                    val currentView =
                                        controller!!.getCurrentWebView().dWebView.rootView
                                    systemUiPlugin = SystemUiPlugin(currentView, systemUIState)
                                }
                                viewItem.dWebView
                            },
                            modifier = Modifier
                                .fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}


class MutilWebViewPlaceholder1Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder2Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder3Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder4Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder5Activity : MutilWebViewActivity()