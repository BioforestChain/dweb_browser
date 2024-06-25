@file:OptIn(ExperimentalForeignApi::class)

package org.dweb_browser.sys.device

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import org.dweb_browser.helper.toKString
import org.dweb_browser.helper.toNSString
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFArrayRefVar
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecCopyErrorMessageString
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitAll
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.OSStatus


@ExperimentalSettingsImplementation
public class KeychainSettings @ExperimentalSettingsApi constructor(vararg defaultProperties: Pair<CFStringRef?, CFTypeRef?>) :
  Settings {

  @OptIn(ExperimentalSettingsApi::class)
  // NB this calls CFBridgingRetain() without ever calling CFBridgingRelease()
  public constructor(service: String) : this(kSecAttrService to CFBridgingRetain(service))

  @OptIn(ExperimentalSettingsApi::class)
  public constructor() : this(*emptyArray())

  @OptIn(ExperimentalForeignApi::class)
  private val defaultProperties =
    mapOf(kSecClass to kSecClassGenericPassword) + mapOf(*defaultProperties)

  /**
   * A factory that can produce [Settings] instances.
   *
   * This class creates `Settings` objects backed by the Apple keychain.
   */
  public class Factory : Settings.Factory {
    override fun create(name: String?): KeychainSettings =
      if (name != null) KeychainSettings(name) else KeychainSettings()
  }

  public override val keys: Set<String>
    get() = memScoped {
      val attributes = alloc<CFArrayRefVar>()
      val status = keyChainOperation(
        kSecMatchLimit to kSecMatchLimitAll,
        kSecReturnAttributes to kCFBooleanTrue
      ) { SecItemCopyMatching(it, attributes.ptr.reinterpret()) }
      status.checkError(errSecItemNotFound)
      if (status == errSecItemNotFound) {
        return emptySet()
      }

      val list = List(CFArrayGetCount(attributes.value).toInt()) { i ->
        val item: CFDictionaryRef? =
          CFArrayGetValueAtIndex(attributes.value, i.toLong())?.reinterpret()
        val cfKey: CFStringRef? = CFDictionaryGetValue(item, kSecAttrAccount)?.reinterpret()
        val nsKey = CFBridgingRelease(cfKey) as NSString
        nsKey.toKString()
      }
      return list.toSet()
    }

  public override fun clear(): Unit = keys.forEach { remove(it) }
  public override fun remove(key: String): Unit = removeKeychainItem(key)
  public override fun hasKey(key: String): Boolean = hasKeychainItem(key)
  public override fun putString(key: String, value: String): Unit =
    addOrUpdateKeychainItem(key, value.toNSString().dataUsingEncoding(NSUTF8StringEncoding))

  public override fun getString(key: String, defaultValue: String): String =
    getStringOrNull(key) ?: defaultValue

  @OptIn(BetaInteropApi::class)
  public override fun getStringOrNull(key: String): String? =
    getKeychainItem(key)?.let { NSString.create(it, NSUTF8StringEncoding)?.toKString() }

  private fun addOrUpdateKeychainItem(key: String, value: NSData?) {
    if (hasKeychainItem(key)) {
      updateKeychainItem(key, value)
    } else {
      addKeychainItem(key, value)
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun addKeychainItem(key: String, value: NSData?): Unit =
    cfRetain(key, value) { cfKey, cfValue ->
      val status = keyChainOperation(
        kSecAttrAccount to cfKey,
        kSecValueData to cfValue
      ) { SecItemAdd(it, null) }
      status.checkError()
    }

  @OptIn(ExperimentalForeignApi::class)
  private fun removeKeychainItem(key: String): Unit = cfRetain(key) { cfKey ->
    val status = keyChainOperation(
      kSecAttrAccount to cfKey,
    ) { SecItemDelete(it) }
    status.checkError(errSecItemNotFound)
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun updateKeychainItem(key: String, value: NSData?): Unit =
    cfRetain(key, value) { cfKey, cfValue ->
      val status = keyChainOperation(
        kSecAttrAccount to cfKey,
        kSecReturnData to kCFBooleanFalse
      ) {
        val attributes = cfDictionaryOf(kSecValueData to cfValue)
        val output = SecItemUpdate(it, attributes)
        CFBridgingRelease(attributes)
        output
      }
      status.checkError()
    }

  @OptIn(ExperimentalForeignApi::class)
  private fun getKeychainItem(key: String): NSData? = cfRetain(key) { cfKey ->
    val cfValue = alloc<CFTypeRefVar>()
    val status = keyChainOperation(
      kSecAttrAccount to cfKey,
      kSecReturnData to kCFBooleanTrue,
      kSecMatchLimit to kSecMatchLimitOne
    ) { SecItemCopyMatching(it, cfValue.ptr) }
    status.checkError(errSecItemNotFound)
    if (status == errSecItemNotFound) {
      return@cfRetain null
    }
    CFBridgingRelease(cfValue.value) as? NSData
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun hasKeychainItem(key: String): Boolean = cfRetain(key) { cfKey ->
    val status = keyChainOperation(
      kSecAttrAccount to cfKey,
      kSecMatchLimit to kSecMatchLimitOne
    ) { SecItemCopyMatching(it, null) }

    status != errSecItemNotFound
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun MemScope.keyChainOperation(
    vararg input: Pair<CFStringRef?, CFTypeRef?>,
    operation: (query: CFDictionaryRef?) -> OSStatus,
  ): OSStatus {
    val query = cfDictionaryOf(defaultProperties + mapOf(*input))
    val output = operation(query)
    CFBridgingRelease(query)
    return output
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun OSStatus.checkError(vararg expectedErrors: OSStatus) {
    if (this != 0 && this !in expectedErrors) {
      val cfMessage = SecCopyErrorMessageString(this, null)
      val nsMessage = CFBridgingRelease(cfMessage) as? NSString
      val message = nsMessage?.toKString() ?: "Unknown error"
      error("Keychain error $this: $message")
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
internal fun MemScope.cfDictionaryOf(vararg items: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef? =
  cfDictionaryOf(mapOf(*items))

@OptIn(ExperimentalForeignApi::class)
internal fun MemScope.cfDictionaryOf(map: Map<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
  val size = map.size
  val keys = allocArrayOf(*map.keys.toTypedArray())
  val values = allocArrayOf(*map.values.toTypedArray())
  return CFDictionaryCreate(
    kCFAllocatorDefault,
    keys.reinterpret(),
    values.reinterpret(),
    size.toLong(),
    null,
    null
  )
}

@OptIn(ExperimentalForeignApi::class)
internal inline fun <T> cfRetain(value: Any?, block: MemScope.(CFTypeRef?) -> T): T = memScoped {
  val cfValue = CFBridgingRetain(value)
  return try {
    block(cfValue)
  } finally {
    CFBridgingRelease(cfValue)
  }
}

@OptIn(ExperimentalForeignApi::class)
internal inline fun <T> cfRetain(
  value1: Any?,
  value2: Any?,
  block: MemScope.(CFTypeRef?, CFTypeRef?) -> T,
): T =
  memScoped {
    val cfValue1 = CFBridgingRetain(value1)
    val cfValue2 = CFBridgingRetain(value2)
    return try {
      block(cfValue1, cfValue2)
    } finally {
      CFBridgingRelease(cfValue1)
      CFBridgingRelease(cfValue2)
    }
  }
