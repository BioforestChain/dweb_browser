package org.dweb_browser.browser.jmm

expect fun getChromeWebViewVersion(): String?

expect suspend fun jmmAppHashVerify(jmmNMM: JmmNMM, jmmHistoryMetadata: JmmHistoryMetadata, zipFilePath: String): Boolean
