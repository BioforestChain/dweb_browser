package org.dweb_browser.shared.bridge

interface KmpNativeBridgeInterface

//主要用于持有iOS注册的实际imp
class KmpNativeRegister {
  companion object {
    var iOSImp: KmpNativeBridgeInterface? = null
  }
}

// 暴露给iOS native使用的。主要是将sys中的KmpNativeRegister.iOSImp暴露给iOS去实现。
class KmpNativeBridge {
  companion object {
    fun registerIos(imp: KmpNativeBridgeInterface) {
      KmpNativeRegister.iOSImp = imp
    }
  }
}