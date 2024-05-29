package org.dweb_browser.sys.contact

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.ContactsContract
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.getAppContextUnsafe

actual class ContactManage {
  @SuppressLint("Recycle", "Range")
  actual suspend fun pickContact(microModule: MicroModule.Runtime): ContactInfo? =
    ContactPickerActivity.launchAndroidPickerContact(microModule)?.let { uri ->
      debugContact("pickContact", "uri=$uri")
      val resolver = getAppContextUnsafe().contentResolver
      val baseCursor = resolver.query(uri, null, null, null, null)
      var contactId: String? = null
      var displayName: String? = null
      baseCursor?.let {
        if (baseCursor.moveToFirst()) {
          contactId = baseCursor.getString(
            baseCursor.getColumnIndex(ContactsContract.Contacts._ID)
          )
          displayName = baseCursor.getString(
            baseCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
          )
        }
      }
      baseCursor?.close() // 使用后就直接关闭了
      debugContact("pickContact", "contactId=$contactId, displayName=$displayName")
      // 通过 contactId 去查询其他数据
      contactId?.let { _id ->
        val number = queryContactData1(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, _id)
        val email = queryContactData1(ContactsContract.CommonDataKinds.Email.CONTENT_URI, _id)
        debugContact("pickContact", "_id=$_id, number=$number, email=$email")
        ContactInfo(displayName!!, listOf(number), listOf(email))
      }
    }

  @SuppressLint("Recycle")
  private fun queryContactData1(uri: Uri, contactId: String): String {
    val projection: Array<String> = arrayOf("data1") // ContactsContract.CommonDataKinds.Phone.Data1
    val selection = "contact_id = $contactId"
    val args: Array<String>? = null
    val sort: String? = null
    val cursor = getAppContextUnsafe().contentResolver.query(uri, projection, selection, args, sort)
    return cursor?.let {
      val result = if (cursor.moveToFirst()) {
        val stringBuffer = StringBuffer()
        do {
          stringBuffer.append(cursor.getString(0))
          stringBuffer.append(", ")
        } while (cursor.moveToNext())
        stringBuffer.toString().trimEnd(',', ' ')
      } else "null"
      cursor.close()
      result
    } ?: "null"
  }
}