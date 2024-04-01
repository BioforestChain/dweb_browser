package org.dweb_browser.sys.contact

import kotlinx.serialization.Serializable
import org.dweb_browser.core.module.MicroModule

@Serializable
data class ContactInfo(
  val name: String, // 联系人姓名
  val phone: List<String>, // 联系人电话
  val email: List<String>, // 电子邮件
)

expect class ContactManage() {
  suspend fun pickContact(microModule: MicroModule.Runtime): ContactInfo?
}