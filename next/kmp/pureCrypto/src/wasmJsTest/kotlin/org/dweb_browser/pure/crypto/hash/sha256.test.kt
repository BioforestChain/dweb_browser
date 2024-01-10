package org.dweb_browser.pure.crypto.hash

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class Sha256Test {
  @OptIn(ExperimentalUnsignedTypes::class)
  @Test
  fun sha256() = runTest {
    val result = sha256(byteArrayOf(1, 2, 3))
    assertContentEquals(
      result, ubyteArrayOf(
        3U, 144U, 88U, 198U, 242U, 192U, 203U, 73U,
        44U, 83U, 59U, 10U, 77U, 20U, 239U, 119U,
        204U, 15U, 120U, 171U, 204U, 206U, 213U, 40U,
        125U, 132U, 161U, 162U, 1U, 28U, 251U, 129U
      ).toByteArray()
    )
  }


}