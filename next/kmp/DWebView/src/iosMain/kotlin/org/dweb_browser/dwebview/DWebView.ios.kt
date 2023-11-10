package org.dweb_browser.dwebview

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSArray
import platform.Foundation.NSString
import platform.Foundation.create
import platform.WebKit.WKContentWorld
import platform.WebKit.WKFrameInfo
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual suspend fun IDWebView.Companion.create(
  mm: MicroModule,
  options: DWebViewOptions
): IDWebView =
  create(CGRectMake(0.0, 0.0, 0.0, 0.0), mm, options, WKWebViewConfiguration())

@OptIn(ExperimentalForeignApi::class)
suspend fun IDWebView.Companion.create(
  frame: CValue<CGRect>,
  remoteMM: MicroModule,
  options: DWebViewOptions = DWebViewOptions(),
  configuration: WKWebViewConfiguration,
) = withMainContext { DWebView(DWebViewEngine(frame, remoteMM, options, configuration)) }

class DWebView(
  private val engine: DWebViewEngine,
) : IDWebView() {
  override suspend fun startLoadUrl(url: String) = withMainContext {
    engine.loadUrl(url)
  }


  override suspend fun getTitle(): String {
    return engine.title ?: ""
  }


  override suspend fun getIcon() = withMainContext {
    engine.evaluateAsyncJavascriptCode(
      """
function getIosIcon(preference_size = 64) {
  const iconLinks = [
    ...document.head.querySelectorAll(`link[rel*="icon"]`).values(),
  ]
    .map((ele) => {
      return {
        ele,
        rel: ele.getAttribute("rel"),
      };
    })
    .filter((link) => {
      return (
        link.rel === "icon" ||
        link.rel === "shortcut icon" ||
        link.rel === "apple-touch-icon" ||
        link.rel === "apple-touch-icon-precomposed"
      );
    })
    .map((link, index) => {
      const sizes = parseInt(link.ele.getAttribute("sizes")) || 32;
      return {
        ...link,
        // 上古时代的图标默认大小是32
        sizes,
        weight: sizes * 100 + index,
      };
    })
    .sort((a, b) => {
      const a_diff = Math.abs(a.sizes - preference_size);
      const b_diff = Math.abs(b.sizes - preference_size);
      /// 和预期大小接近的排前面
      if (a_diff !== b_diff) {
        return a_diff - b_diff;
      }
      /// 权重大的排前面
      return b.weight - a.weight;
    });

  const href =
    (
      iconLinks
        /// 优先获取 ios 的指定图标
        .filter((link) => {
          return (
            link.rel === "apple-touch-icon" ||
            link.rel === "apple-touch-icon-precomposed"
          );
        })[0] ??
      /// 获取标准网页图标
      iconLinks[0]
    )?.ele.href ?? "favicon.ico";

  const iconUrl = new URL(href, document.baseURI);
  return iconUrl.href;
}
function watchIosIcon(preference_size = 64, message_hanlder_name = "favicons") {
  let preIcon = "";
  const getAndPost = () => {
    const curIcon = getIosIcon(preference_size);
    if (curIcon && preIcon !== curIcon) {
      preIcon = curIcon;
      if (typeof webkit !== "undefined") {
        webkit.messageHandlers[message_hanlder_name].postMessage(curIcon);
      } else {
        console.log("favicon:", curIcon);
      }
      return true;
    }
    return false;
  };

  getAndPost();
  const config = { attributes: true, childList: true, subtree: true };
  const observer = new MutationObserver(getAndPost);
  observer.observe(document.head, config);

  return () => observer.disconnect();
}
"""
    )
  }


  private var _destroyed = false
  private var _destroySignal = SimpleSignal();
  private val onLoad = Signal<String>()

  override val onDestroy by lazy { _destroySignal.toListener() }
  override val onStateChange by lazy { engine.onStateChangeSignal.toListener() }

  override val onReady get() = engine.onReady

  override suspend fun destroy() {
    if (_destroyed) {
      return
    }
    _destroyed = true
    debugDWebView("DESTROY")
    loadUrl("about:blank", true)
    runBlockingCatching {
      _destroySignal.emitAndClear(Unit)
    }.getOrNull()
    engine.mainScope.cancel(null)
    engine.removeFromSuperview()
  }

  override suspend fun canGoBack(): Boolean {
    return engine.canGoBack
  }

  override suspend fun canGoForward(): Boolean {
    return engine.canGoForward
  }

  override suspend fun goBack(): Boolean {
    return if (engine.canGoBack) {
      engine.goBack()
      true
    } else {
      false
    }
  }

  override suspend fun goForward(): Boolean {
    return if (engine.canGoForward) {
      engine.goForward()
      true
    } else {
      false
    }
  }

  override suspend fun createMessageChannel(): IWebMessageChannel {
    val deferred = evalAsyncJavascript<NSArray>(
      "nativeCreateMessageChannel()", null,
      DWebViewWebMessage.webMessagePortContentWorld
    )
    val ports_id = deferred.await()
    val port1_id = ports_id.objectAtIndex(0u) as Double
    val port2_id = ports_id.objectAtIndex(1u) as Double

    val port1 = DWebMessagePort(port1_id.toInt(), this)
    val port2 = DWebMessagePort(port2_id.toInt(), this)

    return DWebMessageChannel(port1, port2)
  }

  override suspend fun setContentScale(scale: Float) {
    engine.setContentScaleFactor(scale.toDouble())
  }

  override suspend fun evaluateAsyncJavascriptCode(
    script: String,
    afterEval: suspend () -> Unit
  ) = engine.evaluateAsyncJavascriptCode(script, afterEval)

  @OptIn(BetaInteropApi::class)
  override suspend fun postMessage(data: String, ports: List<IWebMessagePort>) {
    val portIdList = ports.map {
      require(it is DWebMessagePort)
      it.portId
    }
    withMainContext {
      val arguments = mutableMapOf<NSString, NSObject>().apply {
        put(NSString.create(string = "data"), NSString.create(string = data))
        put(NSString.create(string = "ports"), NSArray.create(portIdList))
      }
      callAsyncJavaScript<Unit>(
        "nativeWindowPostMessage(data,ports)",
        arguments.toMap(),
        null,
        DWebViewWebMessage.webMessagePortContentWorld
      ).await()
    }
  }

  fun evalAsyncJavascript(code: String): Deferred<String> = engine.evalAsyncJavascript(code)

  fun <T> evalAsyncJavascript(
    code: String,
    wkFrameInfo: WKFrameInfo?,
    wkContentWorld: WKContentWorld
  ): Deferred<T> = engine.evalAsyncJavascript(code, wkFrameInfo, wkContentWorld)

  fun <T> callAsyncJavaScript(
    functionBody: String,
    arguments: Map<Any?, *>?,
    inFrame: WKFrameInfo?,
    inContentWorld: WKContentWorld,
  ): Deferred<T> = engine.callAsyncJavaScript(functionBody, arguments, inFrame, inContentWorld)
}

