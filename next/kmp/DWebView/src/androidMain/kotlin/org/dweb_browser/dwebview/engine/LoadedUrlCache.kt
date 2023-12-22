package org.dweb_browser.dwebview.engine

import org.dweb_browser.helper.falseAlso
import org.dweb_browser.helper.trueAlso

class LoadedUrlCache {

  var preLoadedUrlArgs = genLoadedUrlArgs(null)
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
        println("QAQ checkLoadedUrl=$url")
      }
    }
  }
}