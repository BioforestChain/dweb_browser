package org.dweb_browser.helper

public fun String.format(vararg args: Any): String {
  var format = this
  args.forEach { arg ->
    format = format.replaceFirst("%s", arg.toString())
  }
  return format
}