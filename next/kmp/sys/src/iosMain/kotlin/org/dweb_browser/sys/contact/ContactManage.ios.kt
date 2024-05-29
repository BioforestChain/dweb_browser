package org.dweb_browser.sys.contact

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.getUIApplication
import platform.Contacts.CNContact
import platform.Contacts.CNContactFormatter
import platform.Contacts.CNContactFormatterStyle
import platform.Contacts.CNLabeledValue
import platform.Contacts.CNPhoneNumber
import platform.ContactsUI.CNContactPickerDelegateProtocol
import platform.ContactsUI.CNContactPickerViewController
import platform.darwin.NSObject


actual class ContactManage {
  class CNContactDelegate(val cancel: () -> Unit, val selected: (CNContact) -> Unit) : NSObject(),
    CNContactPickerDelegateProtocol {

    override fun contactPickerDidCancel(picker: CNContactPickerViewController) {
      cancel()
    }

    override fun contactPicker(
      picker: CNContactPickerViewController,
      didSelectContact: CNContact
    ) {
      picker.dismissModalViewControllerAnimated(true)
      selected(didSelectContact)
    }
  }


  var delegateHolder: CNContactDelegate? = null

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun pickContact(microModule: MicroModule.Runtime): ContactInfo? {

    val scope = CoroutineScope(Dispatchers.Main)
    val deferred = CompletableDeferred<CNContact?>()

    scope.launch {

      val controller = microModule.getUIApplication().keyWindow?.rootViewController

      if (controller != null) {
        val delegate = CNContactDelegate(cancel = {
          deferred.complete(null)
        }, selected = {
          deferred.complete(it)
        })

        val picker = CNContactPickerViewController().apply {
          setDelegate(delegate)
        }
        controller.presentModalViewController(picker, true)
        delegateHolder = delegate
      } else {
        deferred.complete(null)
      }
    }

    val contact = deferred.await()
    delegateHolder = null

    return contact?.let {
      ContactInfo(
        getContactName(it),
        getPhoneNumbers(it),
        getEmails(it)
      )
    }
  }

  private fun getContactName(contact: CNContact): String {
    return CNContactFormatter.stringFromContact(
      contact,
      CNContactFormatterStyle.CNContactFormatterStyleFullName
    ) ?: ""
  }

  private fun getPhoneNumbers(contact: CNContact): List<String> {
    if (contact.phoneNumbers.isEmpty()) return emptyList()

    return contact.phoneNumbers.map {
      val phoneNumber = (it as CNLabeledValue).value
      (phoneNumber as CNPhoneNumber).stringValue
    }
  }

  private fun getEmails(contact: CNContact): List<String> {
    if (contact.emailAddresses.isEmpty()) return emptyList()

    return contact.emailAddresses.map {
      (it as CNLabeledValue).value.toString()
    }
  }
}

