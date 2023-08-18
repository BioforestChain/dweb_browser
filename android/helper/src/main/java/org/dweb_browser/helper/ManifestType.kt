package org.dweb_browser.helper

import java.net.URI

data class ImageResource(
  val src: String,
  val sizes: String? = null,
  val type: String? = null,
  val purpose: String? = null,
  val platform: String? = null
)

data class ShortcutItem(
  val name: String,
  val short_name: String? = null,
  val description: String? = null,
  val url: String,
  val icons: MutableList<ImageResource>? = null
)


enum class DisplayMode {
  fullscreen,
  standalone,
  minimal_ui,
  browser
}

enum class ImageResourcePurposes(val purpose: String) {
  Monochrome("monochrome"),
  Maskable("maskable"),
  Any("any"),
  ;
}

data class ImageResourceSize(val width: Int, val height: Int)

data class StrictImageResource(
  val src: String,
  val purpose: Set<ImageResourcePurposes>,
  val type: String,
  val sizes: List<ImageResourceSize>
) {
  companion object {
    fun from(img: ImageResource, baseUrl: String? = null): StrictImageResource {
      val imgUrl = if (baseUrl != null) URI(baseUrl).resolve(img.src) else URI(img.src)
      var imageType = img.type
      if (imageType == null) {
        // path的获取解析可能会失败
        val imageUrlExt = imgUrl.runCatching { path.substringAfterLast(".") }.getOrNull()
        println("imageUrlExt:$imageUrlExt")
        imageType = when (imageUrlExt) {
          "jpg", "jpeg" -> "image/jpeg"
          "webp", "png", "avif", "apng" -> "image/$imageType"
          "svg" -> "image/svg+xml"
          else -> "image/*"
        }
      }

      val imageSizes = mutableListOf<ImageResourceSize>()
      if (img.sizes == null) {
        if (imageType == "image/svg+xml") {
          imageSizes.add(ImageResourceSize(46340, 46340))
        } else {
          imageSizes.add(ImageResourceSize(1, 1))
        }
      } else if (img.sizes == "any") {
        imageSizes.add(ImageResourceSize(46340, 46340))
      } else {
        img.sizes.split(Regex("\\s+")).forEach { size ->
          Regex("""(\d+)x(\d+)""").find(size)?.let { mathResult ->
            imageSizes.add(
              ImageResourceSize(
                mathResult.groupValues[1].toIntOrNull() ?: 1,
                mathResult.groupValues[2].toIntOrNull() ?: 1
              )
            )
          }
        }
        if (imageSizes.isEmpty()) {
          imageSizes.add(ImageResourceSize(1, 1))
        }
      }

      return StrictImageResource(
        src = imgUrl.toString(),
        purpose = img.purpose?.let { purpose ->
          purpose.split(Regex("""\s+""")).mapNotNull { keyword ->
            ImageResourcePurposes.values().find { it.purpose == keyword }
          }.let { list ->
            if (list.isNotEmpty()) {
              list.toSet()
            } else null
          }
        } ?: setOf(ImageResourcePurposes.Any),
        type = imageType,
        sizes = imageSizes
      )
    }
  }
}
