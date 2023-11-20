package org.dweb_browser.dwebview

data class DWebViewOptions(
  /**
   * 要加载的页面
   */
  val url: String = "",
  /**
   * WebView.onDetachedFromWindow 的策略
   *
   * 如果修改了它，就务必注意 WebView 的销毁需要自己去管控 (手动执行 dwebview.destroy())
   */
  val detachedStrategy: DetachedStrategy = DetachedStrategy.Default,
  /**
   * 对于显示裁切（刘海屏、挖孔屏）的显示策略
   */
  val displayCutoutStrategy: DisplayCutoutStrategy = DisplayCutoutStrategy.Ignore,
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