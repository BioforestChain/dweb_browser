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
   * 打开新窗口的行为
   *
   * 默认是走 `dweb://open?url=*`
   */
  val createWindowBehavior: CreateWindowBehavior = CreateWindowBehavior.Deeplink,
  /**
   * 是否打开开发者工具
   */
  val openDevTools: Boolean = false,
  /**
   * 标识
   */
  var viewId: Int? = null,
  /**
   * 是否使用离屏渲染模式(目前主要用于桌面端web browser)
   * @suppress Desktop Only
   */
  val enabledOffScreenRender: Boolean = false,
  /**
   * 子文件夹，默认情况下，数据跟着模块id走。这里提供一个子文件夹名称，可以将同一个模块的多个dwebview分开来存储数据
   */
  val subDataDirName: String? = null,

  /**
   * 是否启用无痕模式，如果为null，则不启用，否则，同个sessionId的会共享该模式下的数据，并在结束时销毁这些数据
   */
  val incognitoSessionId: String? = null,
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

  enum class CreateWindowBehavior {
    Deeplink,
    Custom,
  }
}