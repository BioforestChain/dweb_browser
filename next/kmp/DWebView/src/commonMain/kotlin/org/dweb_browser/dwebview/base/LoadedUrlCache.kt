package org.dweb_browser.dwebview.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.dns.debugFetch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.helper.trueAlso

class LoadedUrlCache(private val scope: CoroutineScope) {
  private val changeSignal = Signal<String>()
  val onChange = changeSignal.toListener()

  var preLoadedUrlArgs = genLoadedUrlArgs(null)
    set(value) {
      if (field != value) {
        field = value
        scope.launch {
          changeSignal.emit(value)
        }
      }
    }

  fun genLoadedUrlArgs(
    url: String?,
    additionalHttpHeaders: MutableMap<String, String>? = null
  ): String {
    var curLoadUrlArgs = "$url\n";
    if (additionalHttpHeaders != null) {
      curLoadUrlArgs += additionalHttpHeaders.toList()
        .joinToString("\n") { it.first + ":" + it.second }
    }
    return curLoadUrlArgs
  }

  /**
   * 检查参数是否与现在缓存的加载中不一致，如果不一致
   * onUpset 可以提供更新函数，如果onUpset返回true，说明确定更新缓存
   */
  inline fun checkLoadedUrl(
    url: String?,
    additionalHttpHeaders: MutableMap<String, String>? = null,
    onUpset: () -> Boolean = { false }
  ): Boolean {
    val checkArgs = genLoadedUrlArgs(url, additionalHttpHeaders)
    return (preLoadedUrlArgs.startsWith(checkArgs)).falseAlso {
      onUpset().trueAlso {
        preLoadedUrlArgs = checkArgs
        debugFetch("LoadedUrl", "checkLoadedUrl=$url")
      }
    }
  }
}