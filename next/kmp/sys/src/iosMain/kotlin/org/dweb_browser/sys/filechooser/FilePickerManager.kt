package org.dweb_browser.sys.filechooser

import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_group_create
import platform.darwin.dispatch_group_enter
import platform.darwin.dispatch_group_leave
import platform.darwin.dispatch_group_notify

class FilePickerManager {

  enum class FilePickerType {
    IMAGE, VIDEO
  }

  private var filePathCallback: ((result: MutableList<String>) -> Unit)? = null
  private var typeString = ""

  val rootController: UIViewController?
    get() {
      return UIApplication.sharedApplication.keyWindow?.rootViewController
    }

  fun chooseImages(multiple: Boolean, callback: (result: MutableList<String>) -> Unit) {
    typeString = "public.image"
    filePathCallback = callback
    openFileViewController(FilePickerType.IMAGE, multiple)

  }

  fun chooseVideos(multiple: Boolean, callback: (result: MutableList<String>) -> Unit) {
    typeString = "public.movie"
    filePathCallback = callback
    openFileViewController(FilePickerType.VIDEO, multiple)
  }

  private fun openFileViewController(type: FilePickerType, multiple: Boolean) {
    val configuration = PHPickerConfiguration()
    if (multiple) {
      configuration.selectionLimit = Long.MAX_VALUE
    } else {
      configuration.selectionLimit = 1
    }
    when (type) {
      FilePickerType.IMAGE -> configuration.filter = PHPickerFilter.imagesFilter
      FilePickerType.VIDEO -> configuration.filter = PHPickerFilter.videosFilter
    }

    dispatch_async(dispatch_get_main_queue()) {
      val pickerController = PHPickerViewController(configuration)
      pickerController.delegate = delegate
      rootController?.presentViewController(pickerController, true, null)
    }
  }

  inner class PHPickerDelegate : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
      picker.dismissViewControllerAnimated(true, null)
      val paths = mutableListOf<String>()
      println("didFinishPicking")
      val dispatchGroup = dispatch_group_create()
      didFinishPicking.filterIsInstance<PHPickerResult>()
        .map {
          dispatch_group_enter(dispatchGroup)
          it.itemProvider.loadFileRepresentationForTypeIdentifier(typeString)
          { url, error ->
            println("loadFileRepresentationForTypeIdentifier")
            println(error)
            println(url)
            if (error == null && url != null) {
              url.absoluteString?.let { it1 -> paths.add(it1) }
            }
            dispatch_group_leave(dispatchGroup)
          }
        }
      dispatch_group_notify(dispatchGroup, dispatch_get_main_queue()) {
        println("dispatch_group_notify")
        filePathCallback?.let { it(paths) }
      }
    }
  }

  private val delegate = PHPickerDelegate()
}