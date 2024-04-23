package org.dweb_browser.sys.mediacapture

import kotlinx.serialization.Serializable
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.pure.http.PureStream

@Serializable
enum class CameraSource(val type: String) {
  PROMPT("PROMPT"), CAMERA("CAMERA"), PHOTOS("PHOTOS");
}

@Serializable
enum class CameraResultType(val type: String) {
  Uri("uri"), Base64("base64"), DataUrl("dataUrl");
}

@Serializable
enum class CameraDirection(val type: String) {
  Front("FRONT"), Back("BACK");
}

@Serializable
data class ImageOptions(
  /**
   * The quality of image to return as JPEG, from 0-100
   *
   * @since 1.0.0
   */
  val quality: Int = 100,

  /**
   * How the data should be returned. Currently, only 'Base64', 'DataUrl' or 'Uri' is supported
   *
   * @since 1.0.0
   */
  val resultType: CameraResultType = CameraResultType.Base64,

  /**
   * Whether to save the photo to the gallery.
   * If the photo was picked from the gallery, it will only be saved if edited.
   * @default: false
   *
   * @since 1.0.0
   */
  val saveToGallery: Boolean = false,

  /**
   * The source to get the photo from. By default this prompts the user to select
   * either the photo album or take a photo.
   * @default: CameraSource.Prompt
   *
   * @since 1.0.0
   */
  val source: CameraSource = CameraSource.PROMPT,
)

@Serializable
data class Photo(
  /**
   * The base64 encoded string representation of the image, if using CameraResultType.Base64.
   *
   * @since 1.0.0
   */
  val base64String: String? = null,

  /**
   * If using CameraResultType.Uri, the path will contain a full,
   * platform-specific file URL that can be read later using the Filesystem API.
   *
   * @since 1.0.0
   */
  val path: String? = null,

  /**
   * The format of the image, ex: jpeg, png, gif.
   *
   * iOS and Android only support jpeg.
   * Web supports jpeg and png. gif is only supported if using file input.
   *
   * @since 1.0.0
   */
  val format: String = "jpeg",

  /**
   * Whether if the image was saved to the gallery or not.
   *
   * On Android and iOS, saving to the gallery can fail if the user didn't
   * grant the required permissions.
   * On Web there is no gallery, so always returns false.
   *
   * @since 1.1.0
   */
  val saved: Boolean = false,
)

expect class MediaCaptureManage() {
  suspend fun takePicture(microModule: MicroModule.Runtime): PureStream?
  suspend fun captureVideo(microModule: MicroModule.Runtime): PureStream?
  suspend fun recordSound(microModule: MicroModule.Runtime): PureStream?
}