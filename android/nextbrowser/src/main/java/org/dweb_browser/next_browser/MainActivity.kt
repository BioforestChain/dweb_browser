package org.dweb_browser.next_browser

import org.dweb_browser.helper.PromiseOut
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.dweb_browser.next_browser.ui.theme.PlaocTheme
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalComposeUiApi::class)
  @SuppressLint("JavascriptInterface")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      PlaocTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          var window_id_acc by remember { mutableIntStateOf(0) }
          val corScope = rememberCoroutineScope()
          val screenWidth = LocalConfiguration.current.screenWidthDp
          val screenHeight = LocalConfiguration.current.screenHeightDp

          Box(Modifier.fillMaxSize()) {
            val windows = remember { mutableStateMapOf<Int, Window>() };

            // url = "file:///android_asset/index.html"
            // url = "http://172.30.90.240:5173/"
            // url = "file:///android_asset/browser/dist/index.html"
            val browserState = rememberWebViewState(url = "https:///browser.dweb/index.html")
            val browserNavigator = rememberWebViewNavigator()
            val browserWebView = remember {
              val context = this@MainActivity;

              val view = object : WebView(context) {}
              view.settings.allowFileAccess = true
              view.addJavascriptInterface(object {
                @JavascriptInterface
                fun getAllWindow() = windows.keys.joinToString(",")

                @JavascriptInterface
                fun createWindow(url: String): Int {
                  val id = window_id_acc++;
                  val window = Window(context, url, corScope)
                  windows[id] = window
                  return id
                }

                @JavascriptInterface
                fun closeWindow(id: Int) = windows.remove(id)?.let { window ->
                  window.webView.destroy()
                  true
                } ?: false

                @JavascriptInterface
                fun getFrame(id: Int) = windows[id]?.let { window ->
                  listOf(
                    window.x.floatValue,
                    window.y.floatValue,
                    window.width.floatValue,
                    window.height.floatValue,
                    window.round.floatValue,
                  ).joinToString(",")
                }

                @JavascriptInterface
                fun setFrame(
                  id: Int,
                  x: Float,
                  y: Float,
                  width: Float,
                  height: Float,
                  round: Float
                ) {
                  windows[id]?.also { window ->
                    window.x.floatValue = x;
                    window.y.floatValue = y;
                    window.width.floatValue = width;
                    window.height.floatValue = height;
                    window.round.floatValue = round;
                  }
                }

                @JavascriptInterface
                fun setFramesBatch(ops: String) {
                  for (argsStr in ops.split("\n")) {
                    val args = argsStr.split(",")
                    setFrame(
                      id = args[0].toInt(),
                      x = args[1].toFloat(),
                      y = args[2].toFloat(),
                      width = args[3].toFloat(),
                      height = args[4].toFloat(),
                      round = args[5].toFloat()
                    )
                  }
                }

                @JavascriptInterface
                fun getVisible(id: Int) = windows[id]?.visible?.value

                @JavascriptInterface
                fun setVisible(id: Int, visible: Boolean) {
                  windows[id]?.visible?.value = visible
                }


                @JavascriptInterface
                fun getZIndex(id: Int) = windows[id]?.zIndex?.intValue

                @JavascriptInterface
                fun setZIndex(id: Int, zIndex: Int) {
                  windows[id]?.zIndex?.intValue = zIndex
                }

                @JavascriptInterface
                fun getTitle(id: Int) = windows[id]?.state?.pageTitle ?: ""

                @JavascriptInterface
                fun getIcon(id: Int) = windows[id]?.pageIconHref?.value ?: ""

                @JavascriptInterface
                fun getUrl(id: Int) = windows[id]?.state?.lastLoadedUrl ?: ""

                @JavascriptInterface
                fun setUrl(id: Int, url: String) {
                  windows[id]?.navigator?.loadUrl(url)
                }
              }, "__web_browser_api__")
              return@remember view
            }

            WebView(
              modifier = Modifier.fillMaxSize(),
              state = browserState,
              navigator = browserNavigator,
              factory = { browserWebView },
              client = remember {
                val mimeTypeMap by lazy { MimeTypeMap.getSingleton() }
                object : AccompanistWebViewClient() {
                  override fun shouldInterceptRequest(
                    view: WebView?, request: WebResourceRequest?
                  ): WebResourceResponse? {
                    if (request != null && request.url.scheme == "https" && request.url.host == "web.browser.dweb") {
                      val path = request.url.path!!
                      val src = "browser/dist$path"
                      lateinit var dirname: String
                      lateinit var filename: String

                      src.lastIndexOf('/').also { it ->
                        when (it) {
                          -1 -> {
                            filename = src
                            dirname = ""
                          }

                          else -> {
                            filename = src.substring(it + 1)
                            dirname = src.substring(0..it)
                          }
                        }
                        src.substring(0..it)
                      }


                      val androidAssets = this@MainActivity.assets

                      val filenameList = androidAssets.list(dirname) ?: emptyArray()
                      val mimeType =
                        mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(src))
                      if (!filenameList.contains(filename)) {
                        return WebResourceResponse(
                          mimeType,
                          null,
                          ByteArrayInputStream(byteArrayOf())
                        )
                      }
                      return WebResourceResponse(mimeType, null, androidAssets.open(src))
                    }
                    return null
                  }
                }
              }
            )

            for ((id, window) in windows) {
              key(id) {
                val x by window.x;
                val y by window.y;
                val width by window.width;
                val height by window.height;
                val visible by window.visible;

//                val ani_x by animateFloatAsState(targetValue = x, animationSpec = spring(stiffness=3000f))
//                val ani_y by animateFloatAsState(targetValue = y, animationSpec = spring(stiffness=3000f))


                val webViewState = window.state;
                LaunchedEffect(browserWebView, webViewState) {
                  launch(Dispatchers.Main) {
                    snapshotFlow { webViewState.pageTitle }.collect { title ->
                      val safeTitle = URLEncoder.encode(title ?: "", "UTF-8")
                      browserWebView.evaluateJavascript("void __web_browser_api__.onTitleChange?.($id,\"$safeTitle\")") {}
                    }
                  }
                  launch(Dispatchers.Main) {
                    snapshotFlow { webViewState.pageIcon }.collect {
                      val pageIconHref = window.webView.getIconHref();
                      window.pageIconHref.value = pageIconHref
                      val safeHref = URLEncoder.encode(pageIconHref, "UTF-8")
                      browserWebView.evaluateJavascript("void __web_browser_api__.onIconChange?.($id,\"$safeHref\")") {}
                    }
                  }
                  launch(Dispatchers.Main) {
                    snapshotFlow { webViewState.lastLoadedUrl }.collect { url ->
                      val safeUrl = URLEncoder.encode(url ?: "", "UTF-8")
                      browserWebView.evaluateJavascript("void __web_browser_api__.onUrlChange?.($id,\"$safeUrl\")") {}
                    }
                  }
                }



                if (visible
//                  &&
//                  /// 横向溢出 2 屏幕
//                  ((x + width) > 0 && x <= screenWidth)
//                  &&
//                  /// 纵向溢出 2 屏幕
//                  ((y + height) > 0 && y <= screenHeight)
                ) {
                  val zIndex by window.zIndex;
                  val round by window.round;
                  WebView(
                    modifier = Modifier
                      .offset(x = x.dp, y = y.dp)
                      .size(width = width.dp, height = height.dp)
                      .clip(RoundedCornerShape(round))
                      .zIndex(zIndex.toFloat()),
                    state = window.state,
                    navigator = window.navigator,
                    factory = {
                      val parentView =
                        window.webView.parent
                      if (parentView is ViewGroup) {
                        parentView.removeView(window.webView)
                      }
                      window.webView
                    },
                    chromeClient = window.webChromeClient,
                    client = window.webViewClient,
                  )
                }

              }
            }
          }
        }
      }
    }
  }
}

suspend fun WebView.getIconHref() =
  PromiseOut<String>().also {
    evaluateJavascript(
      """
      [...document.querySelectorAll('link[rel~="icon"]').values()].findLast(link=>['icon','shortcut icon'].includes(link.getAttribute("rel")))?.href || ""
    """.trimIndent()
    ) { src ->
      it.resolve(src)
    }
  }.waitPromise()


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(
    text = "Hello $name!", modifier = modifier
  )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  PlaocTheme {
    Greeting("Android")
  }
}