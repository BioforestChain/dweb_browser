package org.dweb_browser.dwebview.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.base.isWebUrlScheme
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SimpleCallback
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.helper.withMainContext
import java.io.File

/**
 * DWebView ,将 WebView 与 dweb 的 dwebHttpServer 设计进行兼容性绑定的模块
 * 该对象继承于WebView，所以需要在主线程去初始化它
 *
 * dwebHttpServer 底层提供了三个概念：
 * host/internal_origin/public_origin
 *
 * 其中 public_origin 是指标准的 http 协议链接，可以在标准的网络中被访问（包括本机的其它应用、以及本机所处的局域网），它的本质就是一个网关，所有的本机请求都会由它代理分发。
 * 而 host ，就是所谓网关分发的判定关键
 * 因此 internal_origin 是一个特殊的 http 链接协议，它非标准，只能在本应用（Dweb Browser）被特定的方法翻译后才能正常访问
 * 这个"翻译"方法的背后，本质上就是 host 这个值在其关键作用：
 * 1. 将 host 值放在 url 的 query.X-Dweb-Host。该方法最直接，基于兼容任何环境很高，缺点是用户构建链接的时候，这部分的信息很容易被干扰没掉
 * 2. 将 host 值放在 request 的 header 中 ("X-Dweb-Host: $HOST")。该方法有一定环境要求，需要确保自定义头部能够被设置并传递，缺点在于它很难被广泛地使用，因为自定义 header 就意味着必须基于命令编程而不是声明式的语句
 * 3. 将 host 值放在 url 的 username 中: uri.userInfo(HOST.encodeURI())。该方法相比第一个方法，优点在于不容易被干扰，而且属于声明式语句，可以对链接所处环境做影响。缺点是它属于身份验证的标准，有很多安全性限制，在一些老的API接口中会有奇怪的行为，比如现代浏览器中的iframe是不允许这种url的。
 * 4. 将 host 值 header["Host"] 中: uri.header("Host", HOST)。该方法对环境要求最大，通常用于可编程能力较高环境中，比如 electron 这种浏览器中对 https/http 域名做完全的拦截，或者说 nodejs 这类完全可空的后端环境中对 httpRequest 做完全的自定义构建。这种方案是最标准的存在，但也是最难适配到各个环境的存在。
 *
 * 以上四种方式，优先级依次降低，都可以将 dweb-host 携带给 public_origin 背后的服务让其进行网关路由
 *
 * 再有关于 internal_origin，是一种非标准的概念，它的存在目的是尽可能不要将请求走到 public_origin，因为这会导致我们的数据走了网卡，从而造成应用内数据被窃取，甚至是会被别人使用 http 请求发起恶意攻击。
 * 因此，我们就要在不同平台环境中的，尽可能让这个 internal_origin 标准能广泛地使用。
 * 具体说，在 Dweb-Browser 这个产品中，最大的问题就是浏览器的拦截问题。
 *
 * 当下，Android 想要拦截 POST 等带 body 的请求，必须用 service-worker 来做到，但是 service-worker 本身直接与原生交互，所以在 service-worker 层返回会引入新的问题，最终的结果就是导致性能下降等。同时 Android 的拦截还有一些限制，比如不允许 300～399 的响应等等。
 * IOS 虽然能拦截 body，但是不能像Android一样去拦截 http/https 链接
 * Electron 25 之后，已经能轻松拦截并构建所有的 http/https 请求的响应了
 *
 * 因此 internal_origin 的形态就千奇百怪。
 * 在 Electron 中的开发版使用的是: http://app.gaubee.com.dweb-443.localhost:22600/index.html
 *    未来正式环境版会使用完整版的形态: https://app.gaubee.com.dweb:443/index.html
 * 在 Android 中也是: https://app.gaubee.com.dweb:443/index.html，但只能处理 GET/200|400|500 这类简单的请求，其它情况下还是得使用 public_origin
 * 在 IOS 中使用的是 app.gaubee.com.dweb+443:/index.html 这样的链接
 *
 * 总而言之，如果你的 WebApp 需要很标准复杂的 http 协议的支持，那么只能选择完全使用 public_origin，它走的是标准的网络协议。
 * 否则，可以像 Plaoc 一样，专注于传统前后端分离的 WebApp，那么可以尽可能采用 internal_origin。
 *
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class DWebViewEngine(
  /**
   * 一个WebView的上下文
   */
  context: Context,
  /// 这两个参数是用来实现请求拦截与转发的
  private val remoteMM: MicroModule,
  /**
   * 一些DWebView自定义的参数
   */
  val options: DWebViewOptions,
  /**
   * 该参数的存在，是用来做一些跟交互式界面相关的行为的，交互式界面需要有一个上下文，比如文件选择、权限申请等行为。
   * 我们将这些功能都写到了BaseActivity上，如果没有提供该对象，则相关的功能将会被禁用
   */
  var activity: BaseActivity? = null
) : WebView(context) {
  init {
    if (activity == null && context is BaseActivity) {
      activity = context
    }
  }

  internal val mainScope = MainScope()
  private var readyHelper: DWebViewClient.ReadyHelper? = null

  private val readyHelperLock = Mutex()
  suspend fun onReady(cb: SimpleCallback) {
    val readyHelper = readyHelperLock.withLock {
      if (readyHelper == null) {
        DWebViewClient.ReadyHelper().also {
          readyHelper = it
          withMainContext {
            dWebViewClient.addWebViewClient(it)
          }
          it.afterReady {
            debugDWebView("READY")
          }
        }
      } else readyHelper!!
    }
    readyHelper.afterReady(cb)
  }

  suspend fun waitReady() {
    val readyPo = PromiseOut<Unit>()
    onReady { readyPo.resolve(Unit) }
    readyPo.waitPromise()
  }

  private val evaluator = WebViewEvaluator(this, mainScope)
  suspend fun getUrlInMain() = withMainContext { url }

  /**
   * 初始化设置 userAgent
   */
  private fun setUA() {
    val baseUserAgentString = settings.userAgentString
    val baseDwebHost = remoteMM.mmid
    var dwebHost = baseDwebHost

    // 初始化设置 ua，这个是无法动态修改的
    val uri = Uri.parse(options.url)
    if ((uri.scheme == "http" || uri.scheme == "https" || uri.scheme == "dweb") &&
      uri.host?.endsWith(".dweb") == true
    ) {
      dwebHost = uri.host!!
    }
    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    settings.userAgentString = "$baseUserAgentString Dweb/$versionName (Android; ${dwebHost})"
  }

  private val closeSignal = SimpleSignal()
  val onCloseWindow = closeSignal.toListener()

  val dWebViewClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    DWebViewClient().also {
      it.addWebViewClient(internalWebViewClient)
      super.setWebViewClient(it)
    }
  }
  private val internalWebViewClient = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
      if (!isWebUrlScheme(request.url.scheme ?: "http")) {
        /// TODO 显示询问对话框
        try {
          val ins = Intent(Intent.ACTION_VIEW, request.url).also {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
              it.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
            }
          }
          context.packageManager.queryIntentActivities(ins, 0)
          context.startActivity(ins)
        } catch (_: Exception) {
        }
        return true
      }
      return super.shouldOverrideUrlLoading(view, request)
    }

    override fun shouldInterceptRequest(
      view: WebView, request: WebResourceRequest
    ): WebResourceResponse? {
      // 转发请求
      if (request.method == "GET" && ((request.url.host?.endsWith(".dweb") == true) || (request.url.scheme == "dweb"))) {
        val response = runBlockingCatching(ioAsyncExceptionHandler) {
          remoteMM.nativeFetch(
            PureRequest(
              request.url.toString(), IpcMethod.GET, IpcHeaders(request.requestHeaders)
            )
          )
        }.getOrThrow()

        val contentType = (response.headers.get("Content-Type") ?: "").split(';', limit = 2)

        debugDWebView("dwebProxy end", request.url)
        val statusCode = response.status.value
        if (statusCode in 301..399) {
          return super.shouldInterceptRequest(view, request)
        }
        return WebResourceResponse(
          contentType.firstOrNull(),
          contentType.lastOrNull(),
          response.status.value,
          response.status.description,
          response.headers.toMap().let { it - "Content-Type" }, // 修复 content-type 问题
          response.body.toPureStream().getReader("DwebView shouldInterceptRequest").toInputStream(),
        )
      }
      return super.shouldInterceptRequest(view, request)
    }
  }

  fun addWebViewClient(client: WebViewClient): () -> Unit {
    dWebViewClient.addWebViewClient(client)
    return {
      dWebViewClient.removeWebViewClient(client)
    }
  }

  fun addWebChromeClient(client: WebChromeClient): () -> Unit {
    dWebChromeClient.addWebChromeClient(client)
    return {
      dWebChromeClient.removeWebChromeClient(client)
    }
  }

  override fun setWebViewClient(client: WebViewClient) {
    dWebViewClient.addWebViewClient(client)
  }

  val dWebChromeClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    DWebChromeClient().also {
      it.addWebChromeClient(internalWebChromeClient)
      super.setWebChromeClient(it)
    }
  }
  private val internalWebChromeClient = object : WebChromeClient() {

    override fun onShowFileChooser(
      webView: WebView,
      filePathCallback: ValueCallback<Array<Uri>>,
      fileChooserParams: FileChooserParams
    ) = activity?.let { context ->
      val mimeTypes = fileChooserParams.acceptTypes.joinToString(",").ifEmpty { "*/*" }
      val captureEnabled = fileChooserParams.isCaptureEnabled
      if (captureEnabled) {
        if (mimeTypes.startsWith("video/")) {
          context.lifecycleScope.launch {
            if (!context.requestSelfPermission(Manifest.permission.CAMERA)) {
              filePathCallback.onReceiveValue(null)
              return@launch
            }
            val tmpFile = File.createTempFile("temp_capture", ".mp4", context.cacheDir);
            val tmpUri = FileProvider.getUriForFile(
              context, "${context.packageName}.file.opener.provider", tmpFile
            )

            if (context.captureVideoLauncher.launch(tmpUri)) {
              filePathCallback.onReceiveValue(arrayOf(tmpUri))
            } else {
              filePathCallback.onReceiveValue(null)
            }
          }
          return true;
        } else if (mimeTypes.startsWith("image/")) {
          context.lifecycleScope.launch {
            if (!context.requestSelfPermission(Manifest.permission.CAMERA)) {
              filePathCallback.onReceiveValue(null)
              return@launch
            }

            val tmpFile = File.createTempFile("temp_capture", ".jpg", context.cacheDir);
            val tmpUri = FileProvider.getUriForFile(
              context, "${context.packageName}.file.opener.provider", tmpFile
            )

            if (context.takePictureLauncher.launch(tmpUri)) {
              filePathCallback.onReceiveValue(arrayOf(tmpUri))
            } else {
              filePathCallback.onReceiveValue(null)
            }
          }
          return true;
        } else if (mimeTypes.startsWith("audio/")) {
          context.lifecycleScope.launch {
            if (!context.requestSelfPermission(Manifest.permission.RECORD_AUDIO)) {
              filePathCallback.onReceiveValue(null)
              return@launch
            }

            val tmpFile = File.createTempFile("temp_capture", ".ogg", context.cacheDir);
            val tmpUri = FileProvider.getUriForFile(
              context, "${context.packageName}.file.opener.provider", tmpFile
            )

            if (context.recordSoundLauncher.launch(tmpUri)) {
              filePathCallback.onReceiveValue(arrayOf(tmpUri))
            } else {
              filePathCallback.onReceiveValue(null)
            }
          }
          return true;
        }
      }

      context.lifecycleScope.launch {
        try {
          if (fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
            val uris = context.getMultipleContentsLauncher.launch(mimeTypes)
            filePathCallback.onReceiveValue(uris.toTypedArray())
          } else {
            val uri = context.getContentLauncher.launch(mimeTypes)
            if (uri != null) {
              filePathCallback.onReceiveValue(arrayOf(uri))
            } else {
              filePathCallback.onReceiveValue(null)
            }
          }
        } catch (e: Exception) {
          filePathCallback.onReceiveValue(null)
        }
      }
      return true
    } ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)


    override fun onCloseWindow(window: WebView?) {
      mainScope.launch {
        closeSignal.emit()
      }
      super.onCloseWindow(window)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
      activity?.also { context ->
        debugDWebView(
          "onPermissionRequest",
          "activity:$context request.resources:${request.resources.joinToString { it }}"
        )
        context.lifecycleScope.launch {
          val requestPermissionsMap = mutableMapOf<String, String>();
          // 参考资料： https://developer.android.com/reference/android/webkit/PermissionRequest#constants.1
          for (res in request.resources) {
            when (res) {
              PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                requestPermissionsMap[Manifest.permission.CAMERA] = res
              }

              PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                requestPermissionsMap[Manifest.permission.RECORD_AUDIO] = res
              }

              PermissionRequest.RESOURCE_MIDI_SYSEX -> {
                requestPermissionsMap[Manifest.permission.BIND_MIDI_DEVICE_SERVICE] = res
              }

              PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {
                // TODO android.webkit.resource.PROTECTED_MEDIA_ID
              }
            }
          }
          if (requestPermissionsMap.isEmpty()) {
            request.grant(arrayOf());
            return@launch
          }
          val responsePermissionsMap =
            context.requestMultiplePermissionsLauncher.launch(requestPermissionsMap.keys.toTypedArray());
          val grants = responsePermissionsMap.filterValues { value -> value };
          if (grants.isEmpty()) {
            request.deny()
          } else {
            request.grant(grants.keys.map { requestPermissionsMap[it] }.toTypedArray())
          }

        }
      } ?: request.deny()
    }
  }

  override fun setWebChromeClient(client: WebChromeClient?) {
    if (client == null) {
      return
    }
    dWebChromeClient.addWebChromeClient(client)
  }

  init {
    debugDWebView("INIT", options)

    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
    )
    setUA()
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    settings.safeBrowsingEnabled = true
    settings.loadWithOverviewMode = true
    settings.loadsImagesAutomatically = true
    settings.setSupportMultipleWindows(true)
    settings.allowFileAccess = true
    settings.javaScriptCanOpenWindowsAutomatically = true
    settings.allowContentAccess = true
    settings.mediaPlaybackRequiresUserGesture = false
    setLayerType(LAYER_TYPE_HARDWARE, null) // 增加硬件加速，避免滑动时画面出现撕裂

    super.setWebViewClient(internalWebViewClient)
    super.setWebChromeClient(internalWebChromeClient)

    if (options.onJsBeforeUnloadStrategy != DWebViewOptions.JsBeforeUnloadStrategy.Default) {
      dWebChromeClient.addWebChromeClient(object : WebChromeClient() {
        override fun onJsBeforeUnload(
          view: WebView?, url: String?, message: String?, result: JsResult?
        ): Boolean {
          when (options.onJsBeforeUnloadStrategy) {
            DWebViewOptions.JsBeforeUnloadStrategy.Cancel -> result?.cancel()
            DWebViewOptions.JsBeforeUnloadStrategy.Confirm -> result?.confirm()
            DWebViewOptions.JsBeforeUnloadStrategy.Default -> return super.onJsBeforeUnload(
              view, url, message, result
            )
          }
          return true
        }
      }, Extends.Config(order = Int.MIN_VALUE))
    }
    if (options.url.isNotEmpty()) {
      /// 开始加载
      loadUrl(options.url)
    }
  }

  private var preLoadedUrlArgs: String? = null

  /**
   * 避免 com.google.accompanist.web 在切换 Compose 上下文的时候重复加载同样的URL
   */
  override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
    val curLoadUrlArgs = "$url\nHEADERS:" + additionalHttpHeaders.toList()
      .joinToString("\n") { it.first + ":" + it.second }
    if (curLoadUrlArgs == preLoadedUrlArgs) {
      return
    }
    preLoadedUrlArgs = curLoadUrlArgs
    super.loadUrl(url, additionalHttpHeaders)
  }

  /**
   * 执行同步JS代码
   */
  suspend fun evaluateSyncJavascriptCode(script: String) =
    evaluator.evaluateSyncJavascriptCode(script)

  /**
   * 执行异步JS代码，需要传入一个表达式
   */
  suspend fun evaluateAsyncJavascriptCode(script: String, afterEval: suspend () -> Unit = {}) =
    withMainContext {
      evaluator.evaluateAsyncJavascriptCode(
        script, afterEval
      )
    }

  private var _destroyed = false
  private var _destroySignal = SimpleSignal();
  fun onDestroy(cb: SimpleCallback) = _destroySignal.listen(cb)
  override fun destroy() {
    if (_destroyed) {
      return
    }
    _destroyed = true
    debugDWebView("DESTROY")
    super.destroy()
    runBlockingCatching {
      _destroySignal.emitAndClear(Unit)
    }.getOrNull()
    mainScope.cancel()
  }

  override fun onDetachedFromWindow() {
    if (options.onDetachedFromWindowStrategy == DWebViewOptions.DetachedFromWindowStrategy.Default) {
      super.onDetachedFromWindow()
    }
  }

}