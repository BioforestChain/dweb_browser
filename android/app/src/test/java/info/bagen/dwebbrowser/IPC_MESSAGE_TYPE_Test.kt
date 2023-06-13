package info.bagen.dwebbrowser

import info.bagen.dwebbrowser.microService.core.ipc.IPC_MESSAGE_TYPE
import info.bagen.dwebbrowser.microService.helper.gson
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IPC_MESSAGE_TYPE_Test {
    @Test
    fun testGson() {
        val res = gson.toJson(IPC_MESSAGE_TYPE.REQUEST)
        assertEquals("0", res)
    }
}