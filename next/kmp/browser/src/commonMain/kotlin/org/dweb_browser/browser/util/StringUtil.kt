package org.dweb_browser.browser.util


/**
 * 判断输入内容是否是域名或者有效的网址
 */
fun String.isUrl(): Boolean {
  // 以 http 或者 https 或者 ftp 打头，可以没有
  // 字符串中只能包含数字和字母，同时可以存在-
  // 最后以 2~5个字符 结尾，可能还存在端口信息，端口信息限制数字，长度为1~5位
  val regex =
    "^((https?|ftp)://)?([a-zA-Z0-9]+([-.][a-zA-Z0-9]+)*\\.[a-zA-Z]{2,5}(:[0-9]{1,5})?(/.*)?)$".toRegex()
  return regex.matches(this)
}

fun String.isHost(): Boolean {
  // 只判断 host(长度1~63,结尾是.然后带2~6个字符如[.com]，没有端口判断)：val regex = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}\$".toRegex()
  val regex =
    "((https?|ftp)://)(((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}(:[0-9]{1,5})?(/.*)?)".toRegex()
  return regex.matches(this)
}

fun String.isUrlOrHost() = this.isUrl() || this.isHost()
fun String.isDeepLink() = this.startsWith("dweb://")

/**
 * 将输入的内容补充为网址，如果本身就是网址直接返回
 */
fun String.toRequestUrl() = if (this.isUrl() || this.isDeepLink()) {
  this
} else if (this.isHost()) {
  "https://$this"
} else {
  null
}

/**
 * 为了判断字符串是否是内置的地址
 */
fun String.isSystemUrl() = this.startsWith("file:///android_asset") ||
    this.startsWith("chrome://") || this.startsWith("about:") ||
    this.startsWith("https://web.browser.dweb") // || this.isDeepLink()
