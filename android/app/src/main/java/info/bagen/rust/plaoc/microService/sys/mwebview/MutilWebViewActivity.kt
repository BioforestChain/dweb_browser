package info.bagen.rust.plaoc.microService.sys.mwebview

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme


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
                if (viewItem != null) key(viewItem.webviewId) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        AndroidView(
                            factory = { ctx ->
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