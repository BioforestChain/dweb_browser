package org.dweb_browser.sys.contact

import org.dweb_browser.core.module.MicroModule

actual class ContactManage actual constructor() {
  actual suspend fun pickContact(microModule: MicroModule.Runtime): ContactInfo? {
    return null
  }
}