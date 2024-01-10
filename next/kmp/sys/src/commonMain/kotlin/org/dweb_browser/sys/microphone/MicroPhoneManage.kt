package org.dweb_browser.sys.microphone

import org.dweb_browser.core.module.MicroModule

expect class MicroPhoneManage() {
  suspend fun recordSound(microModule: MicroModule): String
}