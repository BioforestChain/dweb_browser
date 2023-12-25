package org.dweb_browser.sys.share

import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getUIApplication
import org.dweb_browser.helper.toNSString
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.scan.toNSData
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController

actual suspend fun share(
  shareOptions: ShareOptions,
  multiPartData: MultiPartData?,
  shareNMM: NativeMicroModule?
): String {

  return withMainContext {
    val files = multiPartData?.let {
      val listFile = mutableListOf<NSData>()
      multiPartData.forEachPart { partData ->
        val r = when (partData) {
          is PartData.FileItem -> {
            partData.provider.invoke().readBytes()
          }

          else -> {
            null
          }
        }

        r?.let {
          listFile.add(r.toNSData())
        }

        partData.dispose()
      }
      listFile
    }

    val deferred = CompletableDeferred("")
    val activityItems = mutableListOf<Any>()

    shareOptions.title?.also { activityItems.add(it.toNSString()) }
    shareOptions.text?.also { activityItems.add(it.toNSString()) }
    shareOptions.url?.also { activityItems.add(it.toNSString()) }
    files?.also { activityItems.add(it) }

    val controller =
      UIActivityViewController(activityItems = activityItems, applicationActivities = null)

    if (shareNMM != null) {
      shareNMM.getUIApplication().keyWindow?.rootViewController?.presentViewController(
        controller,
        true,
        null
      )

      controller.completionWithItemsHandler = { _, completed, _, _ ->
        deferred.complete(if (completed) "OK" else "Cancel")
      }
    } else {
      deferred.complete("")
    }

    deferred.await()
  }
}

actual suspend fun share(
  shareOptions: ShareOptions,
  files: List<String>,
  shareNMM: NativeMicroModule?
): String {
  return withMainContext {
    val deferred = CompletableDeferred("")
    val activityItems = mutableListOf<Any>()

    shareOptions.title?.also { activityItems.add(it.toNSString()) }
    shareOptions.text?.also { activityItems.add(it.toNSString()) }
    shareOptions.url?.also { activityItems.add(it.toNSString()) }

    files.forEach {
      activityItems.add(NSURL.fileURLWithPath(it))
    }
    val controller =
      UIActivityViewController(activityItems = activityItems, applicationActivities = null)

    if (shareNMM != null) {
      shareNMM.getUIApplication().keyWindow?.rootViewController?.presentViewController(
        controller,
        true,
        null
      )

      controller.completionWithItemsHandler = { _, completed, _, _ ->
        deferred.complete(if (completed) "OK" else "Cancel")
      }
    } else {
      deferred.complete("")
    }

    deferred.await()
  }
}