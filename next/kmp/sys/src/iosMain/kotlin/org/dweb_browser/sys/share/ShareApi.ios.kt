package org.dweb_browser.sys.share

import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.core.readBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CompletableDeferred
import objcnames.classes.LPLinkMetadata
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.getUIApplication
import org.dweb_browser.helper.platform.NSDataHelper.toNSData
import org.dweb_browser.helper.toNSString
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withMainContext
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.LinkPresentation.LPMetadataProvider
import platform.UIKit.UIActivityItemSourceProtocol
import platform.UIKit.UIActivityType
import platform.UIKit.UIActivityViewController
import platform.darwin.NSObject

actual suspend fun share(
  shareOptions: ShareOptions,
  multiPartData: MultiPartData?,
  shareNMM: MicroModule.Runtime,
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

    shareNMM.getUIApplication().keyWindow?.rootViewController?.apply {

      presentViewController(
        controller, true, null
      )

      controller.completionWithItemsHandler = { _, completed, _, _ ->
        deferred.complete(if (completed) "OK" else "Cancel")
      }
    } ?: deferred.complete("")


    deferred.await()
  }
}

actual suspend fun share(
  shareOptions: ShareOptions,
  files: List<String>,
  shareNMM: MicroModule.Runtime,
): String {
  return withMainContext {
    val deferred = CompletableDeferred<String>()
    val activityItems = mutableListOf<Any>()
    var title: String? = null
    var content = ""

    shareOptions.title?.also {
      it.isNotBlank().trueAlso {
        title = it
        activityItems.add(it)
      }
    }
    shareOptions.url?.also {
      it.isNotBlank().trueAlso {
        activityItems.add(NSURL(string = it))
      }
    }

    val lpMetadataProvider = LPMetadataProvider()

    files.forEachIndexed { index, fileUri ->
      val filePath = fileUri.replace("file://", "")
      val fileUrl = NSURL.fileURLWithPath(filePath)
      if (index == 0) {
        CompletableDeferred<Unit>().also { deferred ->
          lpMetadataProvider.startFetchingMetadataForURL(fileUrl) { metadata, _ ->
            when (metadata) {
              null -> {}
              else -> {
                activityItems.add(FileShareModel(title, content, metadata))
              }
            }
            deferred.complete(Unit)
          }
        }.await()
      }
    }

    shareOptions.text?.also {
      it.isNotBlank().trueAlso {
        activityItems.add(it)
      }
    }

    val controller =
      UIActivityViewController(activityItems = activityItems, applicationActivities = null)

    shareNMM.getUIApplication().keyWindow?.rootViewController?.apply {
      presentViewController(
        controller, true, null
      )
      controller.completionWithItemsHandler = { _, completed, _, _ ->
        deferred.complete(if (completed) "OK" else "Cancel")
      }
    } ?: deferred.complete("")


    deferred.await()
  }
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
class FileShareModel constructor(
  val title: String?,
  val content: String,
  private val lpLinkMetadata: platform.LinkPresentation.LPLinkMetadata? = null,
) : NSObject(), UIActivityItemSourceProtocol {
  @ObjCSignatureOverride
  override fun activityViewController(
    activityViewController: UIActivityViewController, itemForActivityType: UIActivityType?
  ): Any? {
    return lpLinkMetadata?.URL
  }

  override fun activityViewControllerPlaceholderItem(activityViewController: UIActivityViewController): Any {
    return title ?: lpLinkMetadata?.title ?: ""
  }

  @OptIn(ExperimentalForeignApi::class)
  override fun activityViewControllerLinkMetadata(activityViewController: UIActivityViewController): LPLinkMetadata? {
    return lpLinkMetadata as LPLinkMetadata
  }
}