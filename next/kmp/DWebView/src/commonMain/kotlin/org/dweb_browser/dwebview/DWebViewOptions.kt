//package org.dweb_browser.dwebview
//
//data class DWebViewOptions(
//  /**
//   * 要加载的页面
//   */
//  val url: String = "",
//  /**
//   * WebChromeClient.onJsBeforeUnload 的策略
//   *
//   * 用户可以额外地进行策略补充
//   */
//  val onJsBeforeUnloadStrategy: JsBeforeUnloadStrategy = JsBeforeUnloadStrategy.Default,
//  /**
//   * WebView.onDetachedFromWindow 的策略
//   *
//   * 如果修改了它，就务必注意 WebView 的销毁需要自己去管控
//   */
//  val onDetachedFromWindowStrategy: DetachedFromWindowStrategy = DetachedFromWindowStrategy.Default,
//) {
//  enum class JsBeforeUnloadStrategy {
//    /**
//     * 默认行为，会弹出原生的弹窗提示用户是否要离开页面
//     */
//    Default,
//
//    /**
//     * 不会弹出提示框，总是取消，留下
//     */
//    Cancel,
//
//    /**
//     * 不会弹出提示框，总是确认，离开
//     */
//    Confirm, ;
//  }
//
//  enum class DetachedFromWindowStrategy {
//    /**
//     * 默认行为，会触发销毁
//     */
//    Default,
//
//    /**
//     * 忽略默认行为，不做任何事情
//     */
//    Ignore,
//  }
//}