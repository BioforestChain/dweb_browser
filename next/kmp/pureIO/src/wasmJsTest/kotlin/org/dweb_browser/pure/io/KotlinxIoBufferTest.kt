package org.dweb_browser.pure.io

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.readByteArray
import kotlin.test.Test

class KotlinxIoBufferTest {
  @Test
  fun slowBuffer() = runTest {
    val buffer = Buffer()
    launch {
      var i = 0;
      while (i++ < 10) {
        delay(1000)
        buffer.write("index=$i".encodeToByteArray())
      }
    }
    launch {
      try {
       buffer.exhausted()
        val data = buffer.readByteArray(10)
        println("read data=$data")
      } catch (e:EOFException){

      }
    }
  }
}