package org.dweb_browser.sys.toast.ext

import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.sys.toast.EToast
import org.dweb_browser.sys.toast.PositionType

suspend fun NativeMicroModule.showToast(message: String) =
  nativeFetch("file://toast.sys.dweb/show?message=$message")

suspend fun NativeMicroModule.showToast(message: String, duration: EToast) =
  nativeFetch("file://toast.sys.dweb/show?message=$message&duration=${duration.name}")

suspend fun NativeMicroModule.showToast(message: String, position: PositionType) =
  nativeFetch("file://toast.sys.dweb/show?message=$message&position=${position.position}")

suspend fun NativeMicroModule.showToast(message: String, duration: EToast, position: PositionType) =
  nativeFetch("file://toast.sys.dweb/show?message=$message&duration=${duration.name}&position=${position.position}")