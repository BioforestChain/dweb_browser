package org.dweb_browser.helper.platform

actual class OffscreenWebCanvas actual constructor(width: Int, height: Int) {
    actual companion object {
        actual val Default: OffscreenWebCanvas
            get() = TODO("Not yet implemented")
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