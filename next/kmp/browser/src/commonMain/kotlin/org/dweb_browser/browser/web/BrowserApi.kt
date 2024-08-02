package org.dweb_browser.browser.web

import org.dweb_browser.browser.web.model.DwebLinkSearchItem

expect fun getImageResourceRootPath(): String

// 通过 search 和 openinbrowser 打开 web 搜索，目前主要是提供给 Ios 操作，Android直接在common实现
expect suspend fun deepLinkDoSearch(dwebLinkSearchItem: DwebLinkSearchItem)

/**
 * 该方法的功能是为了打开文件，可以是下载文件，也可以是本地文件
 * @param realPath 打开文件的真实路径
 * @param justInstall 如果为 true 表示只打开安装文件，如果是false，表示打开所有文件
 */
expect suspend fun openFileByPath(realPath: String, justInstall: Boolean): Boolean

expect suspend fun dwebviewProxyPrepare()