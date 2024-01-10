package org.dweb_browser.sys.camera.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch

/**
 * 打开拍照功能并获取返回路径
 */
suspend fun MicroModule.takeSystemPicture() =
  nativeFetch("file://camera.sys.dweb/takePicture").body.toPureString()


/**
 * 打开录像功能并获取返回路径
 */
suspend fun MicroModule.captureSystemVideo() =
  nativeFetch("file://camera.sys.dweb/captureVideo").body.toPureString()