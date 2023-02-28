package info.bagen.rust.plaoc

import info.bagen.rust.plaoc.microService.helper.gson
import info.bagen.rust.plaoc.microService.ipc.IPC_MESSAGE_TYPE
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IPC_MESSAGE_TYPE_Test {
    @Test
    fun testGson() {
        val res = gson.toJson(IPC_MESSAGE_TYPE.REQUEST)
        assertEquals("0", res)
    }
}