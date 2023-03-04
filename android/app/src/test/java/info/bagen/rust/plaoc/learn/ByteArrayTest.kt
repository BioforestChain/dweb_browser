package info.bagen.rust.plaoc.learn

import org.junit.jupiter.api.Test

class ByteArrayTest {
    @Test
    fun testSliceArray(){
        var ptr = 1
        var _data  = byteArrayOf(1,2,3,4)
            _data = _data.sliceArray(ptr until _data.size)
        println(_data.joinToString())
    }
}