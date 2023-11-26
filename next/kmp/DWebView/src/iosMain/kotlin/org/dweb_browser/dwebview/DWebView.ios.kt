package org.dweb_browser.dwebview

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.asSharedFlow
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSArray
import platform.Foundation.NSString
import platform.Foundation.create
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual suspend fun IDWebView.Companion.create(
  mm: MicroModule,
  options: DWebViewOptions
): IDWebView =
  create(
    CGRectMake(0.0, 0.0, 100.0, 100.0),
    mm,
    options,
    withMainContext { WKWebViewConfiguration() })

@OptIn(ExperimentalForeignApi::class)
suspend fun IDWebView.Companion.create(
  frame: CValue<CGRect>,
  remoteMM: MicroModule,
  options: DWebViewOptions = DWebViewOptions(),
  configuration: WKWebViewConfiguration,
) = withMainContext { create(DWebViewEngine(frame, remoteMM, options, configuration), options.url) }

@OptIn(ExperimentalForeignApi::class)
internal fun IDWebView.Companion.create(
  engine: DWebViewEngine,
  initUrl: String? = null,
) = DWebView(engine, initUrl)

class DWebView(
  internal val engine: DWebViewEngine,
  initUrl: String? = null
) : IDWebView(initUrl ?: engine.options.url) {
  override val scope get() = engine.ioScope

  override suspend fun startLoadUrl(url: String) = withMainContext {
    engine.loadUrl(url)
  }

  override suspend fun resolveUrl(url: String) = engine.resolveUrl(url)


  override suspend fun getTitle(): String {
    return engine.title ?: ""
  }


  override suspend fun getIcon() = withMainContext {
    evaluateAsyncJavascriptCode(
      // TODO fix this
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

  override val onDestroy by lazy { _destroySignal.toListener() }
  override val onLoadStateChange by lazy { engine.loadStateChangeSignal.toListener() }
  override val onReady get() = engine.onReady
  override val onBeforeUnload by lazy { engine.beforeUnloadSignal.toListener() }
  override val loadingProgressFlow by lazy { engine.loadingProgressSharedFlow.asSharedFlow() }
  override val closeWatcher: ICloseWatcher
    get() = engine.closeWatcher
  override val onCreateWindow by lazy { engine.createWindowSignal.toListener() }

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

  override suspend fun canGoBack() = withMainContext { engine.canGoBack }

  override suspend fun canGoForward() = withMainContext { engine.canGoForward }

  override suspend fun goBack() = withMainContext {
    engine.canGoBack.trueAlso {
      engine.goBack()
    }
  }

  override suspend fun goForward() = withMainContext {
    engine.canGoForward.trueAlso {
      engine.goForward()
    }
  }

  override suspend fun createMessageChannel() = withMainContext {
    val deferred = engine.evalAsyncJavascript<NSArray>(
      "nativeCreateMessageChannel()", null,
      DWebViewWebMessage.webMessagePortContentWorld
    )
    val ports_id = deferred.await()
    val port1_id = ports_id.objectAtIndex(0u) as Double
    val port2_id = ports_id.objectAtIndex(1u) as Double

    val port1 = DWebMessagePort(port1_id.toInt(), this)
    val port2 = DWebMessagePort(port2_id.toInt(), this)

    DWebMessageChannel(port1, port2)
  }

  override suspend fun setContentScale(scale: Float) {
    engine.setContentScaleFactor(scale.toDouble())
  }

  override suspend fun setPrefersColorScheme(colorScheme: WebColorScheme) {
    WARNING("Not yet implemented setPrefersColorScheme")
  }

  override suspend fun setVerticalScrollBarVisible(visible: Boolean) = withMainContext {
    engine.scrollView.showsVerticalScrollIndicator = visible
  }

  override suspend fun setHorizontalScrollBarVisible(visible: Boolean) = withMainContext {
    engine.scrollView.showsHorizontalScrollIndicator = visible
  }

  override suspend fun evaluateAsyncJavascriptCode(
    script: String,
    afterEval: suspend () -> Unit
  ) = withMainContext {
    engine.callAsyncJavaScript<String>(
      "return JSON.stringify(await($script))??'undefined'",
      afterEval = afterEval
    )
  }

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
      engine.callAsyncJavaScript<Unit>(
        "nativeWindowPostMessage(data,ports)",
        arguments.toMap(),
        null,
        DWebViewWebMessage.webMessagePortContentWorld
      )
    }
  }

  override fun setOnTouchListener(onTouch: (IDWebView, MotionEventAction) -> Boolean) {
    WARNING("Not yet implemented setOnTouchListener")
  }

  override fun setOnScrollChangeListener(onScrollChange: (IDWebView, Int, Int, Int, Int) -> Unit) {
    WARNING("Not yet implemented setOnScrollChangeListener")
  }

  override suspend fun getFavoriteIcon(): ImageBitmap? = withMainContext {
    WARNING("Not yet implemented")
    null
  }
}

fun IDWebView.asIosWebView(): DWebViewEngine {
  require(this is DWebView)
  return engine
}