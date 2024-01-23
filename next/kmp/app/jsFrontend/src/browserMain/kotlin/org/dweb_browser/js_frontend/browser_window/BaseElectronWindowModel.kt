package org.dweb_browser.js_frontend.browser_window

import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.js_frontend.view_model.BaseViewModel

import web.http.fetch
import web.http.Response

abstract class BaseElectronWindowModel(frontendViewModelId: String): BaseViewModel(frontendViewModelId){
    val electronWindowOperation = ElectronWindowOperation(frontendViewModelId)

}

/**
 * 控制window的类
 */
class ElectronWindowOperation(
    val frontendViewModelId: String,
){
    fun close(): CompletableDeferred<Response>{
        return _operation("close")
    }

    private fun _operation(operation: String): CompletableDeferred<Response>{
        val deferred = CompletableDeferred<Response>()
        CoroutineScope(Dispatchers.Unconfined).launch {
            deferred.complete(fetch("http://${window.location.host}/browser-window-operation?operation=${operation}&frontendViewModelId=${frontendViewModelId}"))
        }
        return deferred
    }
}




