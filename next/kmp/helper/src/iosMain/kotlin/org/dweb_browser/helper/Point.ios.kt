@file:OptIn(ExperimentalForeignApi::class)

package org.dweb_browser.helper

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.useContents
import kotlinx.cinterop.utf8
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytes
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFNumberRef
import platform.CoreFoundation.CFRangeMake
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.numberWithInt
import platform.Foundation.stringWithUTF8String

@OptIn(ExperimentalForeignApi::class)
public fun CValue<CGPoint>.toPoint(): PurePoint =
  useContents { PurePoint(x.toFloat(), y.toFloat()) }

public fun PurePoint.toIosPoint(): CValue<CGPoint> = CGPointMake(x.toDouble(), y.toDouble())

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalForeignApi::class)
internal fun ByteArray.bridgingRetain() = asUByteArray().let {
  CFDataCreate(null, it.refTo(0), it.size.convert()) as CFDataRef
}

@OptIn(ExperimentalForeignApi::class)
internal fun <T : CPointer<U>, U> MemScope.autorelease(value: T): T {
  defer { CFRelease(value as CFTypeRef) }
  return value
}

@OptIn(ExperimentalForeignApi::class)
internal fun <T : CPointer<U>, U> T.autorelease(scope: MemScope): T = scope.autorelease(this)

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalForeignApi::class)
internal fun CFDataRef.toByteArray() = ByteArray(CFDataGetLength(this).convert())
  .also { CFDataGetBytes(this, CFRangeMake(0, it.size.convert()), it.asUByteArray().refTo(0)) }

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalForeignApi::class)
internal fun String.bridgingRetain() = memScoped {
  CFBridgingRetain(NSString.stringWithUTF8String(this@bridgingRetain.utf8.getPointer(this))) as CFStringRef
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("UNCHECKED_CAST")
internal fun Int.bridgingRetain(): CFNumberRef {
  return CFBridgingRetain(NSNumber.numberWithInt(this)) as CFNumberRef
}
