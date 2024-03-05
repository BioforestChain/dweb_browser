package org.dweb_browser.js_common.state_compose.close

import kotlinx.coroutines.CompletableDeferred


/**
 * 不可恢复的Close接口
 * -
 */
interface IClosed<Reason: Any>{
    val closeDeferred: CompletableDeferred<Unit>
    val closedListeners: MutableList<(reason: Reason?) -> Unit>

    fun onClose(cb: (reason: Reason?) -> Unit): () -> Unit{
        closedListeners.add(cb)
        return {closedListeners.remove(cb)}
    }
    suspend fun waitClose() = closeDeferred.await()

    fun close(reason: Reason?){
        closedListeners.forEach {
           it(reason)
        }
        closeDeferred.complete(Unit)
    }

}

// 类如何调用 interface 里面的方法



