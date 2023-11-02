package org.dweb_browser.dwebview

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.withMainContext
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaType
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIViewController
import platform.WebKit.WKFrameInfo
import platform.WebKit.WKMediaCaptureType
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKPermissionDecision
import platform.WebKit.WKSecurityOrigin
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWindowFeatures
import platform.darwin.NSObject

class DUIDelegateProtocol(private val engine: DWebViewEngine) : NSObject(), WKUIDelegateProtocol {
  private val closeSignal = Signal<WKWebView>()
  private val jsAlertSignal = Signal<Pair<JsParams, SignalResult<Unit>>>()
  private val jsConfirmSignal = Signal<Pair<JsParams, SignalResult<Boolean>>>()
  private val jsPromptSignal = Signal<Pair<JsPromptParams, SignalResult<String?>>>()
  private val createWebviewSignal = Signal<CreateWebViewParams>()
  val createWebViewListener = createWebviewSignal.toListener()

  internal data class JsParams(
    val webView: WKWebView,
    val message: String,
    val wkFrameInfo: WKFrameInfo
  )

  internal data class JsPromptParams(
    val webView: WKWebView,
    val prompt: String,
    val defaultText: String?,
    val wkFrameInfo: WKFrameInfo
  )

  data class CreateWebViewParams(
    val webView: WKWebView,
    val configuration: WKWebViewConfiguration,
    val navigationAction: WKNavigationAction,
    val windowFeatures: WKWindowFeatures,
    val completionHandler: (WKWebView?) -> Unit
  )

  override fun webView(
    webView: WKWebView,
    createWebViewWithConfiguration: WKWebViewConfiguration,
    forNavigationAction: WKNavigationAction,
    windowFeatures: WKWindowFeatures
  ): WKWebView? {
    var result: WKWebView? = null
    val completionHandler: (WKWebView?) -> Unit = {
      result = it
    }
    val args = CreateWebViewParams(webView, createWebViewWithConfiguration, forNavigationAction, windowFeatures, completionHandler)
    var url = forNavigationAction.request.URL?.absoluteString
    val createAction: () -> Unit = {
      if(url != null && engine.closeWatcher.consuming.remove(url)) {
        val consumeToken = url!!
        val isUserGesture = forNavigationAction.targetFrame == null || !forNavigationAction.targetFrame!!.mainFrame
        val watcher = engine.closeWatcher.apply(isUserGesture)

        engine.mainScope.launch {
          withMainContext {
            engine.closeWatcher.resolveToken(consumeToken, watcher)
          }
        }
      } else {
        engine.mainScope.launch {
          createWebviewSignal.emit(args)
        }
      }
    }

    if(url == null) {
      engine.onReadySignal.listen {
        url = forNavigationAction.request.URL?.absoluteString
        createAction()
      }
    } else {
      createAction()
    }

    return result
  }

  override fun webViewDidClose(webView: WKWebView) {
    engine.mainScope.launch {
      closeSignal.emit(webView)
    }
  }

  override fun webView(
    webView: WKWebView,
    runJavaScriptAlertPanelWithMessage: String,
    initiatedByFrame: WKFrameInfo,
    completionHandler: () -> Unit
  ) {
    val finalNext = Signal<Pair<JsParams, SignalResult<Unit>>>()

    finalNext.listen {
      val (params, ctx) = it
      val vc = webView.getUIViewController()
      if (vc != null) {
        val alertController = UIAlertController.alertControllerWithTitle(
          params.webView.title,
          params.message,
          UIAlertControllerStyleAlert
        )
        alertController.addAction(UIAlertAction.actionWithTitle("Ok", UIAlertActionStyleDefault) {
          ctx.complete(Unit)
        })
        vc.presentViewController(alertController, true, null)
      } else {
        ctx.complete(Unit)
      }
    }

    engine.mainScope.launch {
      jsAlertSignal.emitForResult(
        JsParams(
          webView,
          runJavaScriptAlertPanelWithMessage,
          initiatedByFrame
        ), finalNext
      )
      completionHandler()
    }
  }

  override fun webView(
    webView: WKWebView,
    runJavaScriptConfirmPanelWithMessage: String,
    initiatedByFrame: WKFrameInfo,
    completionHandler: (Boolean) -> Unit
  ) {
    val finalNext = Signal<Pair<JsParams, SignalResult<Boolean>>>()

    finalNext.listen {
      val (params, ctx) = it
      val vc = webView.getUIViewController()
      if (vc != null) {
        val confirmController = UIAlertController.alertControllerWithTitle(
          params.webView.title,
          params.message,
          UIAlertControllerStyleAlert
        )
        confirmController.addAction(
          UIAlertAction.actionWithTitle(
            "Cancel",
            UIAlertActionStyleCancel
          ) {
            ctx.complete(false)
          })
        confirmController.addAction(UIAlertAction.actionWithTitle("Ok", UIAlertActionStyleDefault) {
          ctx.complete(true)
        })
        vc.presentViewController(confirmController, true, null)
      } else {
        ctx.complete(false)
      }
    }

    engine.mainScope.launch {
      val (confirm, _) = jsConfirmSignal.emitForResult(
        JsParams(
          webView,
          runJavaScriptConfirmPanelWithMessage,
          initiatedByFrame
        ), finalNext
      )
      completionHandler(confirm!!)
    }
  }

  override fun webView(
    webView: WKWebView,
    runJavaScriptTextInputPanelWithPrompt: String,
    defaultText: String?,
    initiatedByFrame: WKFrameInfo,
    completionHandler: (String?) -> Unit
  ) {
    val finalNext = Signal<Pair<JsPromptParams, SignalResult<String?>>>()

    finalNext.listen {
      val (params, ctx) = it
      val vc = webView.getUIViewController()
      if (vc != null) {
        val promptController = UIAlertController.alertControllerWithTitle(
          params.webView.title,
          params.prompt,
          UIAlertControllerStyleAlert
        )
        promptController.addTextFieldWithConfigurationHandler { textField ->
          textField?.text = params.defaultText
          textField?.selectAll(null)
          promptController.addAction(
            UIAlertAction.actionWithTitle(
              "Cancel",
              UIAlertActionStyleCancel
            ) {
              ctx.complete(null)
            })
          promptController.addAction(
            UIAlertAction.actionWithTitle(
              "Ok",
              UIAlertActionStyleDefault
            ) {
              ctx.complete(textField?.text)
            })
        }

        vc.presentViewController(promptController, true, null)
      } else {
        ctx.complete(null)
      }
    }

    engine.mainScope.launch {
      val (promptText, _) = jsPromptSignal.emitForResult(
        JsPromptParams(
          webView,
          runJavaScriptTextInputPanelWithPrompt,
          defaultText,
          initiatedByFrame
        ), finalNext
      )
      completionHandler(promptText ?: "")
    }
  }

  override fun webView(
    webView: WKWebView,
    requestMediaCapturePermissionForOrigin: WKSecurityOrigin,
    initiatedByFrame: WKFrameInfo,
    type: WKMediaCaptureType,
    decisionHandler: (WKPermissionDecision) -> Unit
  ) {
    val mediaTypes = when (type) {
      WKMediaCaptureType.WKMediaCaptureTypeCamera -> listOf(AVMediaTypeVideo)
      WKMediaCaptureType.WKMediaCaptureTypeMicrophone -> listOf(AVMediaTypeAudio)
      WKMediaCaptureType.WKMediaCaptureTypeCameraAndMicrophone -> listOf(
        AVMediaTypeVideo,
        AVMediaTypeAudio
      )

      else -> emptyList()
    }

    if (mediaTypes.isEmpty()) {
      decisionHandler(WKPermissionDecision.WKPermissionDecisionPrompt)
      return
    }

    engine.mainScope.launch {
      mediaTypes.forEach {
        val isAuthorized = when (AVCaptureDevice.authorizationStatusForMediaType(it)) {
          AVAuthorizationStatusNotDetermined -> AVCaptureDevice.requestAccessForMediaType(it)
            .await()

          AVAuthorizationStatusAuthorized -> true
          AVAuthorizationStatusDenied -> false
          /*
           * 1. 家长控制功能启用,限制了应用访问摄像头或麦克风
           * 2. 机构部署的设备,限制了应用访问硬件功能
           * 3. 用户在 iCloud 中的"隐私"设置中针对应用禁用了访问权限
           */
          AVAuthorizationStatusRestricted -> null
          else -> null
        }

        /// 认证失败
        if (isAuthorized == false) {
          decisionHandler(WKPermissionDecision.WKPermissionDecisionDeny)
          return@launch
        }

        /// 受限 或者 未知？
        /// TODO 用额外的提示框提示用户
        if (isAuthorized == null) {
          decisionHandler(WKPermissionDecision.WKPermissionDecisionPrompt)
          return@launch
        }
      }

      /// 所有的权限都验证通过
      decisionHandler(WKPermissionDecision.WKPermissionDecisionGrant)
    }
  }

  fun AVCaptureDevice.Companion.requestAccessForMediaType(mediaType: AVMediaType?): Deferred<Boolean> {
    val deferred = CompletableDeferred<Boolean>()

    AVCaptureDevice.requestAccessForMediaType(mediaType) {
      deferred.complete(it)
    }

    return deferred
  }

  internal class SignalResult<R> {
    var result: R? = null
    var hasResult: Boolean = false

    /**
     * 写入结果
     */
    fun complete(result: R) {
      if (hasResult) return

      this.result = result
      hasResult = true
      next()
    }

    /**
     * 跳过处置，由下一个处理者接管
     */
    fun next() {
      waiter.complete(Unit)
    }

    val waiter = CompletableDeferred<Unit>()
  }

  private suspend fun <T, R> Signal<Pair<T, SignalResult<R>>>.emitForResult(
    args: T,
    finallyNext: Signal<Pair<T, SignalResult<R>>>
  ): Pair<R?, Boolean> {
    try {
      val ctx = SignalResult<R>()
      this@emitForResult.emit(Pair(args, ctx))
      finallyNext.emit(Pair(args, ctx))

      ctx.waiter.await()
      if (ctx.hasResult) {
        return Pair(ctx.result, true)
      }
    } catch (e: Throwable) {
      debugDWebView("DUIDelegateProtocol", e.message ?: e.stackTraceToString())
    }

    return Pair(null, false)
  }

  private fun WKWebView.getUIViewController(): UIViewController? {
    var responder = nextResponder
    while (responder != null) {
      if (responder is UIViewController) {
        return responder
      }
      responder = nextResponder
    }

    return null
  }
}