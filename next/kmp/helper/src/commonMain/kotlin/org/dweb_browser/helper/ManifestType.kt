package org.dweb_browser.helper

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ImageResource(
  val src: String,
  val sizes: String? = null,
  val type: String? = null,
  val purpose: String? = null,
  val platform: String? = null,
)

@Serializable
public data class ShortcutItem(
  val name: String,
  @SerialName("short_name")
  val shortName: String? = null,
  val description: String? = null,
  val url: String,
  val icons: MutableList<ImageResource>? = null,
)


@Suppress("ClassName")
public object DisplayMode_Serializer :
  StringEnumSerializer<DisplayMode>("DisplayMode", DisplayMode.ALL_VALUES, { mode })


@Serializable(with = DisplayMode_Serializer::class)
public enum class DisplayMode(public val mode: String) {
  Fullscreen("fullscreen"), Standalone("standalone"), MinimalUi("minimal_ui"), Browser("browser"),
  ;

  public companion object {
    public val ALL_VALUES: Map<String, DisplayMode> = entries.associateBy { it.mode }
  }
}

@Serializable
public enum class ImageResourcePurposes(public val purpose: String) {
  Monochrome("monochrome"), Maskable("maskable"), Any("any"),
  ;

  public companion object {
    public val ALL_VALUES: Map<ImageResourcePurposes, ImageResourcePurposes> =
      entries.associateBy { it }
  }
}

@Serializable
public data class ImageResourceSize(val width: Int, val height: Int)

@Serializable
public data class StrictImageResource(
  val src: String,
  val purpose: Set<ImageResourcePurposes>,
  val type: String,
  val sizes: List<ImageResourceSize>,
) {
  public companion object {
    public fun from(img: ImageResource, baseUrl: String? = null): StrictImageResource {
      var imageType = img.type
      val imgFullHref = if (img.src.startsWith("data:")) {
        if (imageType == null) {
          imageType = img.src.substring(5).split(";", limit = 2)[0].split(",", limit = 2)[0]
        }
        img.src
      } else {
        val imgUrl =
          if (baseUrl != null) URLBuilder(baseUrl).takeFrom(img.src).build() else Url(img.src)

        if (imageType == null) {
          // path的获取解析可能会失败
          val imageUrlExt = imgUrl.runCatching { encodedPath.substringAfterLast(".") }.getOrNull()
          imageType = when (imageUrlExt) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp", "png", "avif", "apng" -> "image/$imageUrlExt"
            "svg" -> "image/svg+xml"
            else -> "image/*"
          }
        }
        imgUrl.toString()
      }


      val imageSizes = mutableListOf<ImageResourceSize>()
      if (img.sizes == null) {
        if (imageType == "image/svg+xml") {
          imageSizes.add(ImageResourceSize(2048, 2048))
        } else {
          imageSizes.add(ImageResourceSize(128, 128))
        }
      } else if (img.sizes == "any") {
        imageSizes.add(ImageResourceSize(2048, 2048))
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

      return StrictImageResource(src = imgFullHref, purpose = img.purpose?.let { purpose ->
        purpose.split(Regex("""\s+""")).mapNotNull { keyword ->
          ImageResourcePurposes.entries.find { it.purpose == keyword }
        }.let { list ->
          if (list.isNotEmpty()) {
            list.toSet()
          } else null
        }
      } ?: setOf(ImageResourcePurposes.Any), type = imageType, sizes = imageSizes)
    }
  }
}
