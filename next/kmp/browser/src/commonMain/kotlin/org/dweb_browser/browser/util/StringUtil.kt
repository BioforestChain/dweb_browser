package org.dweb_browser.browser.util

/**
 * 判断字符串是否是 dweb link
 */
fun String.regexDeepLink() = Regex("dweb:.+").matchEntire(this.trim())?.groupValues?.firstOrNull()
