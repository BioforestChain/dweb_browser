package org.dweb_browser.core.module

import androidx.compose.ui.util.fastJoinToString
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.base64String
import org.dweb_browser.helper.hexString
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.pure.crypto.hash.sha256Sync

fun generateDwebDevIcons(text: String) = listOf(
  ImageResource(
    src = "data:image/svg+xml;base64,${
      generateDwebIconTemplate1(text).utf8Binary.base64String
    }",
    type = "image/svg+xml",
    purpose = "maskable",
  )
)

private val dwebIconTemplate1 = """
  <svg class="icon" viewBox="0 0 74 74" xmlns="http://www.w3.org/2000/svg" width="74" height="74">
    <linearGradient id="bgGradient" x1="0" x2="0" y1="0" y2="1">
      <stop offset="0%" stop-color="#ffffff" />
      <stop offset="100%" stop-color="#000000" />
    </linearGradient>
    <rect width="74" height="74" fill="url(#bgGradient)" />
    <text x="5" y="23" fill="#ff0099" font-size="16">L1</text>
    <text x="5" y="43" fill="#ff9900" font-size="16">L2</text>
    <text x="5" y="63" fill="#00ff99" font-size="16">L3</text>
    <g transform="rotate(-45)" transform-origin="50% 50%">
      <rect y="3" width="74" height="16" fill="#10101060">
      </rect>
      <text x="22" y="16" font-size="16" fill="#ff2040a0">DEV</text>
    </g>
  </svg>
""".trimIndent()

private fun generateDwebIconTemplate1(text: String): String {
  val randomSeed = sha256Sync(text.utf8Binary)
  val isDark = randomSeed[0] >= 0x80
  fun ByteArray.transformBytes(toDark: Boolean) = map {
    when {
      toDark -> when {
        it < 0x80 -> it
        else -> it / 2
      }

      else -> when {
        it >= 0x80 -> it
        else -> it + 0x80
      }
    }.toByte()
  }.toByteArray()

  val bgColor1 = randomSeed.sliceArray(0..<3).transformBytes(isDark).hexString
  val bgColor2 = randomSeed.sliceArray(3..<6).transformBytes(isDark).hexString
  val color1 = randomSeed.sliceArray(4..<7).transformBytes(!isDark).hexString
  val color2 = randomSeed.sliceArray(7..<10).transformBytes(!isDark).hexString
  val color3 = randomSeed.sliceArray(10..<13).transformBytes(!isDark).hexString
  val text2 = when {
    text.length > 12 -> text.substring(0, 12)
    else -> (" ".repeat((12 - text.length) / 2) + text).padEnd(12, ' ')
  }
  val text3 = text2.map {
    when (it.code) {
      in 0x21..0x7E -> (it.code + 0xFEE0).toChar()
      0x20 -> 0x3000.toChar()
      else -> it
    }
  }
  val line1 = text3.subList(0, 4).fastJoinToString("")
  val line2 = text3.subList(4, 8).fastJoinToString("")
  val line3 = text3.subList(8, 12).fastJoinToString("")
  return dwebIconTemplate1
    .replace("ffffff", bgColor1)
    .replace("000000", bgColor2)
    .replace("ff0099", color1)
    .replace("ff9900", color2)
    .replace("00ff99", color3)
    .replace("L1", line1)
    .replace("L2", line2)
    .replace("L3", line3)
}