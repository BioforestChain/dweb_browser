package org.dweb_browser.browser.web

import org.dweb_browser.browser.web.model.DwebLinkSearchItem

expect fun getImageResourceRootPath(): String

// 通过 search 和 openinbrowser 打开 web 搜索，目前主要是提供给 Ios 操作，Android直接在common实现
expect suspend fun deepLinkDoSearch(dwebLinkSearchItem: DwebLinkSearchItem)