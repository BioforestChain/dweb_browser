package org.dweb_browser.pure.http.ext

import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureServerRequest

/**
 * 这里使用异步调函数而不是直接返回FetchResponse，目的是使用异步回调函数来传递生命周期的概念，在调用returnBlock结束后，FetchHook可以对一些引用资源进行释放。
 * 这样就可以一些不必要的内存减少拷贝
 */
typealias FetchHook = suspend FetchHookContext.() -> PureResponse?

data class FetchHookContext(
  val request: PureServerRequest,
)