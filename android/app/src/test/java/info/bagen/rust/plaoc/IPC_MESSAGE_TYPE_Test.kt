package info.bagen.rust.plaoc

import info.bagen.dwebbrowser.microService.helper.gson
import info.bagen.dwebbrowser.microService.ipc.IPC_MESSAGE_TYPE
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IPC_MESSAGE_TYPE_Test {
    @Test
    fun testGson() {
        val res = gson.toJson(IPC_MESSAGE_TYPE.REQUEST)
        assertEquals("0", res)
    }
}