package org.dweb_browser.dwebview

import androidx.webkit.WebViewFeature
import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.compose.SimpleI18nResource

class DWebViewFeature(val featureId: String, val i18n: SimpleI18nResource) {
  val name by lazy { featureId.lowercase().replace('_', ' ') }
  val enabled by lazy { WebViewFeature.isFeatureSupported(featureId) }
}

class FeatureGroups(list: List<DWebViewFeature>, val i18n: SimpleI18nResource) :
  List<DWebViewFeature> by list

class AllFeatures(val groups: List<FeatureGroups>) : List<DWebViewFeature> by groups.flatten() {
  val enabledFeatures by lazy { filter { it.enabled } }
}


private infix fun String.to(i18n: SimpleI18nResource) = DWebViewFeature(this, i18n)

private val VISUAL_STATE_CALLBACK = WebViewFeature.VISUAL_STATE_CALLBACK to SimpleI18nResource(
  Language.ZH to "启用在 WebView 的可视状态发生变化时接收回调。",
  Language.EN to "Enables receiving callbacks when the visual state of the WebView changes."
)
private val OFF_SCREEN_PRERASTER = WebViewFeature.OFF_SCREEN_PRERASTER to SimpleI18nResource(
  Language.ZH to "启用离屏预渲染网页内容以加快加载速度。",
  Language.EN to "Enables prerastering of web content off-screen for faster loading."
)
private val SAFE_BROWSING_ENABLE = WebViewFeature.SAFE_BROWSING_ENABLE to SimpleI18nResource(
  Language.ZH to "启用安全浏览功能，以保护用户免受恶意网站的侵害。",
  Language.EN to "Enables safe browsing functionality to protect users from malicious websites."
)
private val DISABLED_ACTION_MODE_MENU_ITEMS =
  WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS to SimpleI18nResource(
    Language.ZH to "禁用操作模式中的特定菜单项。",
    Language.EN to "Disables specific menu items in action mode."
  )
private val START_SAFE_BROWSING = WebViewFeature.START_SAFE_BROWSING to SimpleI18nResource(
  Language.ZH to "启动安全浏览功能。", Language.EN to "Starts the safe browsing functionality."
)
private val SAFE_BROWSING_ALLOWLIST = WebViewFeature.SAFE_BROWSING_ALLOWLIST to SimpleI18nResource(
  Language.ZH to "设置一个安全浏览允许的 URL 列表，即使它们被标记为恶意网站。",
  Language.EN to "Sets a list of URLs to be allowed by safe browsing even if they are flagged as malicious."
)
private val SAFE_BROWSING_PRIVACY_POLICY_URL =
  WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL to SimpleI18nResource(
    Language.ZH to "设置安全浏览隐私政策的 URL。",
    Language.EN to "Sets the URL of the safe browsing privacy policy."
  )
private val SERVICE_WORKER_BASIC_USAGE =
  WebViewFeature.SERVICE_WORKER_BASIC_USAGE to SimpleI18nResource(
    Language.ZH to "启用服务工作者的基本用法。",
    Language.EN to "Enables basic usage of service workers."
  )
private val SERVICE_WORKER_CACHE_MODE =
  WebViewFeature.SERVICE_WORKER_CACHE_MODE to SimpleI18nResource(
    Language.ZH to "允许控制服务工作者的缓存行为。",
    Language.EN to "Allows control over the caching behavior of service workers."
  )
private val SERVICE_WORKER_CONTENT_ACCESS =
  WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS to SimpleI18nResource(
    Language.ZH to "允许服务工作者访问网页内容。",
    Language.EN to "Allows service workers to access web content."
  )
private val SERVICE_WORKER_FILE_ACCESS =
  WebViewFeature.SERVICE_WORKER_FILE_ACCESS to SimpleI18nResource(
    Language.ZH to "允许服务工作者访问文件。",
    Language.EN to "Allows service workers to access files."
  )
private val SERVICE_WORKER_BLOCK_NETWORK_LOADS =
  WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS to SimpleI18nResource(
    Language.ZH to "允许服务工作者阻止网络加载。",
    Language.EN to "Allows service workers to block network loads."
  )
private val SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST =
  WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST to SimpleI18nResource(
    Language.ZH to "允许服务工作者拦截网络请求。",
    Language.EN to "Allows service workers to intercept network requests."
  )
private val RECEIVE_WEB_RESOURCE_ERROR =
  WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR to SimpleI18nResource(
    Language.ZH to "启用接收网页资源错误的回调。",
    Language.EN to "Enables receiving callbacks for web resource errors."
  )
private val RECEIVE_HTTP_ERROR = WebViewFeature.RECEIVE_HTTP_ERROR to SimpleI18nResource(
  Language.ZH to "启用接收 HTTP 错误的回调。",
  Language.EN to "Enables receiving callbacks for HTTP errors."
)
private val SHOULD_OVERRIDE_WITH_REDIRECTS =
  WebViewFeature.SHOULD_OVERRIDE_WITH_REDIRECTS to SimpleI18nResource(
    Language.ZH to "控制 WebView 是否自己处理重定向。",
    Language.EN to "Controls whether WebView should handle redirects itself."
  )
private val SAFE_BROWSING_HIT = WebViewFeature.SAFE_BROWSING_HIT to SimpleI18nResource(
  Language.ZH to "指示安全浏览命中，可能是一个恶意网站。",
  Language.EN to "Indicates a safe browsing hit, potentially a malicious website."
)
private val TRACING_CONTROLLER_BASIC_USAGE =
  WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE to SimpleI18nResource(
    Language.ZH to "启用跟踪控制器用于性能分析的基本用法。",
    Language.EN to "Enables basic usage of the tracing controller for performance analysis."
  )
private val WEB_RESOURCE_REQUEST_IS_REDIRECT =
  WebViewFeature.WEB_RESOURCE_REQUEST_IS_REDIRECT to SimpleI18nResource(
    Language.ZH to "确定给定的网页资源请求是否重定向。",
    Language.EN to "Determines whether a given web resource request is a redirect."
  )
private val WEB_RESOURCE_ERROR_GET_DESCRIPTION =
  WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION to SimpleI18nResource(
    Language.ZH to "检索给定网页资源错误的描述。",
    Language.EN to "Retrieves a description for a given web resource error."
  )
private val WEB_RESOURCE_ERROR_GET_CODE =
  WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE to SimpleI18nResource(
    Language.ZH to "检索给定网页资源错误的错误代码。",
    Language.EN to "Retrieves the error code for a given web resource error."
  )
private val SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY =
  WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY to SimpleI18nResource(
    Language.ZH to "指示在安全浏览命中后，用户应被引导回安全站点。",
    Language.EN to "Indicates that the user should be directed back to a safe site after a safe browsing hit."
  )
private val SAFE_BROWSING_RESPONSE_PROCEED =
  WebViewFeature.SAFE_BROWSING_RESPONSE_PROCEED to SimpleI18nResource(
    Language.ZH to "指示用户应被允许继续访问可能包含恶意的网站。",
    Language.EN to "Indicates that the user should be allowed to proceed to a potentially malicious site."
  )
private val SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL =
  WebViewFeature.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL to SimpleI18nResource(
    Language.ZH to "指示应向用户显示有关可能包含恶意的网站的插页警告。",
    Language.EN to "Indicates that an interstitial warning should be shown to the user about a potentially malicious site."
  )
private val WEB_MESSAGE_PORT_POST_MESSAGE =
  WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE to SimpleI18nResource(
    Language.ZH to "允许向网页消息端口发送消息。",
    Language.EN to "Allows sending messages to a web message port."
  )
private val WEB_MESSAGE_PORT_CLOSE = WebViewFeature.WEB_MESSAGE_PORT_CLOSE to SimpleI18nResource(
  Language.ZH to "允许关闭网页消息端口。", Language.EN to "Allows closing a web message port."
)
private val WEB_MESSAGE_ARRAY_BUFFER =
  WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER to SimpleI18nResource(
    Language.ZH to "启用通过网页消息端口发送和接收 ArrayBuffer 消息。",
    Language.EN to "Enables sending and receiving ArrayBuffer messages through web message ports."
  )
private val WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK =
  WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK to SimpleI18nResource(
    Language.ZH to "允许为网页消息端口设置消息回调。",
    Language.EN to "Allows setting a message callback for a web message port."
  )
private val CREATE_WEB_MESSAGE_CHANNEL =
  WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL to SimpleI18nResource(
    Language.ZH to "允许创建网页消息频道，用于在 WebView 和原生应用程序之间进行通信。",
    Language.EN to "Allows creating a web message channel for communication between a WebView and a native app."
  )
private val POST_WEB_MESSAGE = WebViewFeature.POST_WEB_MESSAGE to SimpleI18nResource(
  Language.ZH to "允许向网页消息频道发布消息。",
  Language.EN to "Allows posting a message to a web message channel."
)
private val WEB_MESSAGE_CALLBACK_ON_MESSAGE =
  WebViewFeature.WEB_MESSAGE_CALLBACK_ON_MESSAGE to SimpleI18nResource(
    Language.ZH to "接收发布到网页消息频道的消息。",
    Language.EN to "Receives messages posted to a web message channel."
  )
private val GET_WEB_VIEW_CLIENT = WebViewFeature.GET_WEB_VIEW_CLIENT to SimpleI18nResource(
  Language.ZH to "检索当前 WebViewClient 实例。",
  Language.EN to "Retrieves the current WebViewClient instance."
)
private val GET_WEB_CHROME_CLIENT = WebViewFeature.GET_WEB_CHROME_CLIENT to SimpleI18nResource(
  Language.ZH to "检索当前 WebChromeClient 实例。",
  Language.EN to "Retrieves the current WebChromeClient instance."
)
private val GET_WEB_VIEW_RENDERER = WebViewFeature.GET_WEB_VIEW_RENDERER to SimpleI18nResource(
  Language.ZH to "检索当前 WebViewRenderer 实例。",
  Language.EN to "Retrieves the current WebViewRenderer instance."
)
private val WEB_VIEW_RENDERER_TERMINATE =
  WebViewFeature.WEB_VIEW_RENDERER_TERMINATE to SimpleI18nResource(
    Language.ZH to "终止当前 WebViewRenderer 实例。",
    Language.EN to "Terminates the current WebViewRenderer instance."
  )
private val WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE =
  WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE to SimpleI18nResource(
    Language.ZH to "启用 WebViewRendererClient 的基本用法来处理渲染器事件。",
    Language.EN to "Enables basic usage of the WebViewRendererClient for handling renderer events."
  )
private val PROXY_OVERRIDE = WebViewFeature.PROXY_OVERRIDE to SimpleI18nResource(
  Language.ZH to "允许覆盖默认代理设置。",
  Language.EN to "Allows overriding the default proxy settings."
)
private val MULTI_PROCESS = WebViewFeature.MULTI_PROCESS to SimpleI18nResource(
  Language.ZH to "启用多进程模式来隔离网页内容。",
  Language.EN to "Enables multi-process mode for isolating web content."
)
private val FORCE_DARK = WebViewFeature.FORCE_DARK to SimpleI18nResource(
  Language.ZH to "启用强制 WebView 使用暗模式。",
  Language.EN to "Enables forcing dark mode for the WebView."
)
private val FORCE_DARK_STRATEGY = WebViewFeature.FORCE_DARK_STRATEGY to SimpleI18nResource(
  Language.ZH to "控制强制暗模式的策略。",
  Language.EN to "Controls the strategy for forcing dark mode."
)
private val WEB_MESSAGE_LISTENER = WebViewFeature.WEB_MESSAGE_LISTENER to SimpleI18nResource(
  Language.ZH to "提供网页消息事件的侦听器。",
  Language.EN to "Provides a listener for web message events."
)
private val DOCUMENT_START_SCRIPT = WebViewFeature.DOCUMENT_START_SCRIPT to SimpleI18nResource(
  Language.ZH to "允许注入一个脚本，当文档开始加载时执行。",
  Language.EN to "Allows injecting a script to be executed when a document starts loading."
)
private val PROXY_OVERRIDE_REVERSE_BYPASS =
  WebViewFeature.PROXY_OVERRIDE_REVERSE_BYPASS to SimpleI18nResource(
    Language.ZH to "允许为特定 URL 绕过代理覆盖。",
    Language.EN to "Allows bypassing the proxy override for specific URLs."
  )
private val GET_VARIATIONS_HEADER = WebViewFeature.GET_VARIATIONS_HEADER to SimpleI18nResource(
  Language.ZH to "检索当前 WebView 实例的版本头。",
  Language.EN to "Retrieves the variations header for the current WebView instance."
)
private val ALGORITHMIC_DARKENING = WebViewFeature.ALGORITHMIC_DARKENING to SimpleI18nResource(
  Language.ZH to "启用 WebView 的算法变暗。",
  Language.EN to "Enables algorithmic darkening for the WebView."
)
private val ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY =
  WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY to SimpleI18nResource(
    Language.ZH to "控制企业认证应用链接的行为。",
    Language.EN to "Controls the behavior of enterprise authentication app links."
  )
private val GET_COOKIE_INFO = WebViewFeature.GET_COOKIE_INFO to SimpleI18nResource(
  Language.ZH to "检索当前 WebView 实例的 cookie 信息。",
  Language.EN to "Retrieves cookie information for the current WebView instance."
)
private val USER_AGENT_METADATA = WebViewFeature.USER_AGENT_METADATA to SimpleI18nResource(
  Language.ZH to "设置当前 WebView 实例的用户代理元数据。",
  Language.EN to "Sets user agent metadata for the current WebView instance."
)
private val MULTI_PROFILE = WebViewFeature.MULTI_PROFILE to SimpleI18nResource(
  Language.ZH to "启用多配置文件模式来隔离网页内容。",
  Language.EN to "Enables multi-profile mode for isolating web content."
)
private val ATTRIBUTION_REGISTRATION_BEHAVIOR =
  WebViewFeature.ATTRIBUTION_REGISTRATION_BEHAVIOR to SimpleI18nResource(
    Language.ZH to "控制归因注册的行为。",
    Language.EN to "Controls the behavior of attribution registration."
  )
private val WEBVIEW_MEDIA_INTEGRITY_API_STATUS =
  WebViewFeature.WEBVIEW_MEDIA_INTEGRITY_API_STATUS to SimpleI18nResource(
    Language.ZH to "检索 WebView 媒体完整性 API 的状态。",
    Language.EN to "Retrieves the status of the WebView Media Integrity API."
  )
private val MUTE_AUDIO = WebViewFeature.MUTE_AUDIO to SimpleI18nResource(
  Language.ZH to "静音当前 WebView 实例的音频。",
  Language.EN to "Mutes the audio for the current WebView instance."
)


val DWebView.Companion.securityAndPrivacyFeatures by lazy {
  FeatureGroups(
    listOf(
      SAFE_BROWSING_ENABLE,
      START_SAFE_BROWSING,
      SAFE_BROWSING_ALLOWLIST,
      SAFE_BROWSING_PRIVACY_POLICY_URL
    ),
    SimpleI18nResource(Language.ZH to "安全与隐私", Language.EN to "Security and Privacy")
  )
}

val DWebView.Companion.renderingAndPerformanceFeatures by lazy {
  FeatureGroups(
    listOf(
      VISUAL_STATE_CALLBACK,
      OFF_SCREEN_PRERASTER,
      TRACING_CONTROLLER_BASIC_USAGE
    ),
    SimpleI18nResource(Language.ZH to "渲染与性能", Language.EN to "Rendering and Performance")
  )
}

val DWebView.Companion.serviceWorkerFeatures by lazy {
  FeatureGroups(
    listOf(
      SERVICE_WORKER_BASIC_USAGE,
      SERVICE_WORKER_CACHE_MODE,
      SERVICE_WORKER_CONTENT_ACCESS,
      SERVICE_WORKER_FILE_ACCESS,
      SERVICE_WORKER_BLOCK_NETWORK_LOADS,
      SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST
    ),
    SimpleI18nResource(Language.ZH to "服务工作者", Language.EN to "Service Workers")
  )
}

val DWebView.Companion.multiProcessAndIsolationFeatures by lazy {
  FeatureGroups(
    listOf(
      MULTI_PROCESS,
      FORCE_DARK,
      FORCE_DARK_STRATEGY,
      ALGORITHMIC_DARKENING
    ),
    SimpleI18nResource(
      Language.ZH to "多进程与隔离",
      Language.EN to "Multi-Processing and Isolation"
    )
  )
}

val DWebView.Companion.webMessagingSystemFeatures by lazy {
  FeatureGroups(
    listOf(
      WEB_MESSAGE_PORT_POST_MESSAGE,
      WEB_MESSAGE_PORT_CLOSE,
      WEB_MESSAGE_ARRAY_BUFFER,
      WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK,
      CREATE_WEB_MESSAGE_CHANNEL,
      POST_WEB_MESSAGE,
      WEB_MESSAGE_CALLBACK_ON_MESSAGE,
      WEB_MESSAGE_LISTENER
    ),
    SimpleI18nResource(Language.ZH to "网页消息系统", Language.EN to "Web Messaging System")
  )
}

val DWebView.Companion.errorHandlingFeatures by lazy {
  FeatureGroups(
    listOf(
      RECEIVE_WEB_RESOURCE_ERROR,
      RECEIVE_HTTP_ERROR,
      SHOULD_OVERRIDE_WITH_REDIRECTS,
      SAFE_BROWSING_HIT,
      SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY,
      SAFE_BROWSING_RESPONSE_PROCEED,
      SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL,
      WEB_RESOURCE_REQUEST_IS_REDIRECT,
      WEB_RESOURCE_ERROR_GET_DESCRIPTION,
      WEB_RESOURCE_ERROR_GET_CODE
    ),
    SimpleI18nResource(Language.ZH to "错误处理", Language.EN to "Error Handling")
  )
}

val DWebView.Companion.clientAndRendererInteractionFeatures by lazy {
  FeatureGroups(
    listOf(
      GET_WEB_VIEW_CLIENT,
      GET_WEB_CHROME_CLIENT,
      GET_WEB_VIEW_RENDERER,
      WEB_VIEW_RENDERER_TERMINATE,
      WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE
    ),
    SimpleI18nResource(
      Language.ZH to "客户端与渲染器交互",
      Language.EN to "Client and Renderer Interaction"
    )
  )
}

val DWebView.Companion.proxyAndNetworkingFeatures by lazy {
  FeatureGroups(
    listOf(
      PROXY_OVERRIDE,
      PROXY_OVERRIDE_REVERSE_BYPASS
    ),
    SimpleI18nResource(Language.ZH to "代理与网络", Language.EN to "Proxy and Networking")
  )
}

val DWebView.Companion.otherFeatures by lazy {
  FeatureGroups(
    listOf(
      DISABLED_ACTION_MODE_MENU_ITEMS,
      DOCUMENT_START_SCRIPT,
      GET_VARIATIONS_HEADER,
      ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY,
      GET_COOKIE_INFO,
      USER_AGENT_METADATA,
      MULTI_PROFILE,
      ATTRIBUTION_REGISTRATION_BEHAVIOR,
      WEBVIEW_MEDIA_INTEGRITY_API_STATUS,
      MUTE_AUDIO
    ),
    SimpleI18nResource(Language.ZH to "其他功能", Language.EN to "Other Features")
  )
}

/**
 * 以下代码使用 Gemini 1.5 进行解释和翻译
 */
val DWebView.Companion.allFeatures by lazy {
  AllFeatures(
    listOf(
      DWebView.securityAndPrivacyFeatures,
      DWebView.renderingAndPerformanceFeatures,
      DWebView.serviceWorkerFeatures,
      DWebView.multiProcessAndIsolationFeatures,
      DWebView.webMessagingSystemFeatures,
      DWebView.errorHandlingFeatures,
      DWebView.clientAndRendererInteractionFeatures,
      DWebView.proxyAndNetworkingFeatures,
      DWebView.otherFeatures,
    )
  )
}
