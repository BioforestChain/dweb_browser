package org.dweb_browser.dwebview

data class DWebViewOptions(
  /**
   * 要加载的页面
   */
  val url: String = "",
  /**
   * 是否尽可能使用私有网络：
   * 如果启用 https:*.dweb 会强行变成 dweb+https:*.dweb 的协议访问（即便我们可以通过代理网络技术来拦截 https:*.dweb 协议请求）。
   * 而在 Android 上可能也会使用 service-worker 技术来对 https:*.dweb 进行完全的拦截。
   *
   * > 注意 http:*dweb 本来就会变成 dweb+http:*.dweb 的协议访问。
   *
   * 总之，如果使用了私有网络，会根据不同的操作系统平台作出一定的技术妥协，而好处是不会数据传输不会走任何网络协议
   */
  val privateNet: Boolean = false,
  /**
   * WebView.onDetachedFromWindow 的策略
   *
   * 如果修改了它，就务必注意 WebView 的销毁需要自己去管控 (手动执行 dwebview.destroy())
   */
  val detachedStrategy: DetachedStrategy = DetachedStrategy.Default,
  /**
   * 对于显示裁切（刘海屏、挖孔屏）的显示策略
   */
  val displayCutoutStrategy: DisplayCutoutStrategy = DisplayCutoutStrategy.Default,
  /**
   * 是否打开开发者工具
   */
  val openDevTools: Boolean = false,
  /**
   * 标识
   */
  var viewId: Int? = null,
  /**
   * 是否使用离屏渲染模式(目前主要用于桌面端)，默认开启，开启后才能在compose中正确渲染窗口
   * @suppress Desktop Only
   */
  val enabledOffScreenRender: Boolean = true,
  /**
   * 是否启用默认菜单功能(目前主要用于桌面端)，默认开启
   * @suppress Desktop Only
   */
  val enableContextMenu: Boolean = true,

  /**
   * 是否启用无痕模式，如果为null，则不启用，否则，同个sessionId的会共享该模式下的数据，并在结束时销毁这些数据
   */
  val incognitoSessionId: String? = null,

  /**
   * 档案集
   * 不同的档案之间数据是隔离的
   *
   * 但前提是要判定是否原生支持，目前一些国产Android手机对于profile的支持并不好，即便他们Chromium的内核已经不低了
   */
  val profile: String = "default",

  /**
   * Android 是否要使用 shouldInterceptRequest 来拦截请求直接响应，而不使用网络代理
   * 默认启用
   */
  val androidInterceptGetRequest: Boolean = true,
) {

  enum class DetachedStrategy {
    /**
     * 默认行为，会触发销毁
     */
    Default,

    /**
     * 忽略默认行为，不做任何事情
     */
    Ignore,
  }

  enum class DisplayCutoutStrategy {
    // 默认跟随显示屏裁切
    Default,

    // 忽略显示屏裁切
    Ignore,
  }
}