package org.dweb_browser.helper.platform

import android.webkit.WebView

actual class OffscreenWebCanvas actual constructor(width: Int, height: Int) {
    val webview = WebView(PlatformViewController.appContext!!)

    init {

    }

    actual companion object {
        actual val Default: OffscreenWebCanvas by lazy { OffscreenWebCanvas(128, 128) }
    }

    actual suspend fun evalJavaScriptWithResult(jsCode: String): kotlin.Result<String> {
        TODO("Not yet implemented")
    }

    actual suspend fun evalJavaScriptWithVoid(jsCode: String): kotlin.Result<Unit> {
        TODO("Not yet implemented")
    }

    actual val width: Int
        get() = TODO("Not yet implemented")
    actual val height: Int
        get() = TODO("Not yet implemented")

}