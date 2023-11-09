package org.dweb_browser.sys

import platform.Foundation.NSData

interface KmpNativeBridgeInterface {
    fun invokeKmpEvent(event: KmpToIosEvent): Any?
    suspend fun invokeAsyncKmpEvent(event: KmpToIosEvent): Any?
}

public data class KmpToIosEvent(val name: String, val inputDatas: Map<String, Any>?)

//主要用于持有iOS注册的实际imp
public class KmpNativeRegister() {
    companion object {
        var iOSImp:KmpNativeBridgeInterface? = null
    }
}
