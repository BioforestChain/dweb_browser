package org.dweb_browser.sys

interface KmpNativeBridgeInterface {
    fun getShareController(): KmpNativeBridgeShareInterface?
}

interface KmpNativeBridgeShareInterface {
//    fun share(title: String, text: String)
}

//主要用于持有iOS注册的实际imp
public class KmpNativeRegister() {
    companion object {
        var iOSImp:KmpNativeBridgeInterface? = null
    }
}
