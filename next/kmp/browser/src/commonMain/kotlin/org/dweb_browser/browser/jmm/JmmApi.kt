package org.dweb_browser.browser.jmm

expect fun getChromeWebViewVersion(): String?

expect suspend fun jmmAppHashVerify(jmmNMM: JmmNMM.JmmRuntime, jmmMetadata: JmmMetadata, zipFilePath: String): Boolean
