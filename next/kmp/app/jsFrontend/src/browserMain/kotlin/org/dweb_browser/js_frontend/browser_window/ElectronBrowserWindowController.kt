package org.dweb_browser.js_frontend.browser_window

import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import web.http.Response
import web.http.fetch

class ElectronBrowserWindowController{
    fun close(): CompletableDeferred<Response> {
        return _operation("close")
    }

    fun reload(): CompletableDeferred<Response>{
        return _operation("reload")
    }

    private fun _operation(operation: String): CompletableDeferred<Response> {
        val deferred = CompletableDeferred<Response>()
        CoroutineScope(Dispatchers.Unconfined).launch {
            deferred.complete(fetch("http://${window.location.host}/browser-window-operation?operation=${operation}"))
        }
        return deferred
    }
}