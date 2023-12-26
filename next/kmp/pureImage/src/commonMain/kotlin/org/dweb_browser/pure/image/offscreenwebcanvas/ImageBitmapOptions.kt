package org.dweb_browser.pure.image.offscreenwebcanvas

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.StringEnumSerializer

@Serializable
data class ImageBitmapOptions(
  val colorSpaceConversion: ColorSpaceConversion? = null,
  val imageOrientation: ImageOrientation? = null,
  val premultiplyAlpha: PremultiplyAlpha? = null,
//  val resizeHeight: Int? = null,
//  val resizeQuality: Float? = null,
//  val resizeWidth: Int? = null,
) {
  @Serializable
  enum class ColorSpaceConversion {
    default, none
  }

  object ImageOrientationSerializer : StringEnumSerializer<ImageOrientation>("ImageOrientation",
    ImageOrientation.entries.associateBy { it.value },
    { value })

  @Serializable(ImageOrientationSerializer::class)
  enum class ImageOrientation(val value: String) {
    none("none"), flipY("flipY"), fromImage("from-image")
  }

  @Serializable
  enum class PremultiplyAlpha {
    default, none, premultiply
  }
}