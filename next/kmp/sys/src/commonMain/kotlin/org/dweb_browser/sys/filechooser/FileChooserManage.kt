package org.dweb_browser.sys.filechooser

import io.ktor.http.ContentType
import io.ktor.http.fromFilePath
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.some

expect class FileChooserManage() {
  suspend fun openFileChooser(
    microModule: MicroModule.Runtime, accept: String, multiple: Boolean
  ): List<String>
}


/**
 * 将 accept 属性转化为文件名过滤函数
 */
fun acceptToNameFilter(accept: String): (fileName: String) -> Boolean {
  val filters = accept.split(",").mapNotNull { singleAcceptToNameFilter(it) }
  return when (filters.size) {
    0 -> {
      { true }
    }

    1 -> filters.first()
    else -> {
      {
        var matched = false
        for (filter in filters) {
          if (filter(it)) {
            matched = true
            break
          }
        }
        matched
      }
    }
  }
}

private fun singleAcceptToNameFilter(mayBeAccept: String): ((fileName: String) -> Boolean)? {
  val accept = mayBeAccept.trim()
  if (accept.contains("/")) {
    when {
      accept.contains("*") -> {
        val acceptRegexp = Regex(accept.replace("*", ".*"))
        return {
          val mimes = ContentType.fromFilePath(it)
          mimes.some { acceptRegexp.matches(it.toString()) }
        }
      }

      else -> {
        return {
          val mimes = ContentType.fromFilePath(it)
          mimes.some { accept == it.toString() }
        }
      }
    }
  } else if (accept.startsWith(".")) {
    return {
      it.endsWith(accept)
    }
  }
  return null
}