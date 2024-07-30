package org.dweb_browser.sys.haptics.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.sys.haptics.HapticsImpactType
import org.dweb_browser.sys.haptics.HapticsNotificationType


suspend fun MicroModule.Runtime.vibrateImpact(style: HapticsImpactType = HapticsImpactType.LIGHT) =
  nativeFetch("file://haptics.sys.dweb/impact?style=${style.type}")

suspend fun MicroModule.Runtime.vibrateNotification(style: HapticsNotificationType = HapticsNotificationType.SUCCESS) =
  nativeFetch("file://haptics.sys.dweb/notification?style=${style.type}")

suspend fun MicroModule.Runtime.vibrateClick() = nativeFetch("file://haptics.sys.dweb/click")
suspend fun MicroModule.Runtime.vibrateDisabled() =
  nativeFetch("file://haptics.sys.dweb/disabled")

suspend fun MicroModule.Runtime.vibrateDoubleClick() =
  nativeFetch("file://haptics.sys.dweb/doubleClick")

suspend fun MicroModule.Runtime.vibrateHeavyClick() =
  nativeFetch("file://haptics.sys.dweb/heavyClick")

suspend fun MicroModule.Runtime.vibrateTick() = nativeFetch("file://haptics.sys.dweb/tick")
suspend fun MicroModule.Runtime.vibrateCustomize(duration: List<Long>) =
  nativeFetch("file://haptics.sys.dweb/customize?duration=${duration.joinToString(",")}")
