package org.dweb_browser.helper

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StrictImageResourceTests {
  @Test
  fun basic() {
    val baseUrl = "http://localhost/"
    assertEquals(
      StrictImageResource.from(
        ImageResource("icon/lowres.webp", "48x48", "image/webp"), baseUrl
      ), StrictImageResource(
        "http://localhost/icon/lowres.webp",
        setOf(ImageResourcePurposes.Any),
        "image/webp",
        listOf(ImageResourceSize(48, 48)),
      )
    )
    assertEquals(
      StrictImageResource.from(ImageResource("icon/lowres", "48x48"), baseUrl),
      StrictImageResource(
        "http://localhost/icon/lowres",
        setOf(ImageResourcePurposes.Any),
        "image/*",
        listOf(ImageResourceSize(48, 48)),
      )
    )
    assertEquals(
      StrictImageResource.from(
        ImageResource(
          "icon/hd_hi.ico", "72x72 96x96 128x128 256x256"
        ), baseUrl
      ), StrictImageResource(
        "http://localhost/icon/hd_hi.ico",
        setOf(ImageResourcePurposes.Any),
        "image/*",
        listOf(
          ImageResourceSize(72, 72),
          ImageResourceSize(96, 96),
          ImageResourceSize(128, 128),
          ImageResourceSize(256, 256),
        ),
      )
    )
    assertEquals(
      StrictImageResource.from(ImageResource("icon/hd_hi.svg", "any", purpose = "maskable monochrome"), baseUrl),
      StrictImageResource(
        "http://localhost/icon/hd_hi.svg",
        setOf(ImageResourcePurposes.Maskable,ImageResourcePurposes.Monochrome),
        "image/svg+xml",
        listOf(ImageResourceSize(46340, 46340)),
      )
    )
  }
}
