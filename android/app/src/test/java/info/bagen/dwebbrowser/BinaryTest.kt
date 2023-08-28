package info.bagen.dwebbrowser

import org.dweb_browser.helper.toByteArray
import org.dweb_browser.helper.toInt
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BinaryTest {
  @Test
  fun testInt2ByteArray() {
    for (num in 0..10000) {
      val arr1 = num.toByteArray()
      val arr2 = num.toByteArray()
      val num1 = arr1.toInt()
      val num2 = arr2.toInt()
      assertEquals(num, num1, "fail at num1 $num")
      assertEquals(num, num2, "fail at num2 $num")
    }

//        val bb4 = ByteBuffer.allocate(4)
//        bb4.put(byteArrayOf(1, 2, 3, 4), 0, 4)
//        println("bb4: ${bb4.getInt(0)}")
//        bb4.clear()
//        bb4.put(byteArrayOf(1, 2, 3, 4), 0, 4)
//        println("bb4: ${bb4.getInt(0)}")
//

  }
}