package org.dweb_browser.shared.microService

import org.dweb_browser.sys.KmpNativeBridgeInterface
import org.dweb_browser.sys.KmpNativeRegister

// 暴露给iOS native使用的。主要是将sys中的KmpNativeRegister.iOSImp暴露给iOS去实现。
public class KmpNativeBridge() {
    companion object {
        fun registerIos(imp: KmpNativeBridgeInterface) {
            KmpNativeRegister.iOSImp = imp
        }
    }
}

