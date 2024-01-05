package org.dweb_browser.sys.share

import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.core.readBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import objcnames.classes.LPLinkMetadata
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getUIApplication
import org.dweb_browser.helper.toNSString
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.scan.toNSData
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.lastPathComponent
import platform.LinkPresentation.LPMetadataProvider
import platform.UIKit.UIActivityItemSourceProtocol
import platform.UIKit.UIActivityType
import platform.UIKit.UIActivityViewController
import platform.darwin.NSObject
import kotlin.experimental.ExperimentalObjCName

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

    val deferred = CompletableDeferred<String>()
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

@OptIn(ExperimentalForeignApi::class, ExperimentalObjCName::class)
actual suspend fun share(
  shareOptions: ShareOptions,
  files: List<String>,
  shareNMM: NativeMicroModule?
): String {
  return withMainContext {
    val deferred = CompletableDeferred<String>()
    val activityItems = mutableListOf<Any>()

    shareOptions.title?.also { activityItems.add(it.toNSString()) }
    shareOptions.text?.also { activityItems.add(it.toNSString()) }
    shareOptions.url?.also { activityItems.add(it.toNSString()) }

    val lpMetadataProvider = LPMetadataProvider()
    val providerDeferred = CompletableDeferred<Int>()
    var size = files.size
    files.forEach {
      val fileUrl = NSURL.fileURLWithPath(it.replace("file://", ""))
      lpMetadataProvider.startFetchingMetadataForURL(fileUrl) { metadata, _ ->
        activityItems.add(FileShareModel(fileUrl, metadata))
        if (--size == 0) {
          providerDeferred.complete(size)
        }
      }
    }
    providerDeferred.await()

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

class FileShareModel @OptIn(ExperimentalForeignApi::class) constructor(
  val url: NSURL,
  private val lpLinkMetadata: platform.LinkPresentation.LPLinkMetadata? = null
) : NSObject(), UIActivityItemSourceProtocol {
  override fun activityViewController(
    activityViewController: UIActivityViewController,
    itemForActivityType: UIActivityType?
  ): Any? {
    return url
  }

  override fun activityViewControllerPlaceholderItem(activityViewController: UIActivityViewController): Any {
    return lpLinkMetadata?.title ?: url.lastPathComponent ?: url
  }

  @OptIn(ExperimentalForeignApi::class)
  override fun activityViewControllerLinkMetadata(activityViewController: UIActivityViewController): LPLinkMetadata? {
    return lpLinkMetadata as LPLinkMetadata
  }
}