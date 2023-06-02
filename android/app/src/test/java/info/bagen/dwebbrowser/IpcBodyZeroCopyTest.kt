package info.bagen.dwebbrowser

import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.core.ipc.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IpcBodyZeroCopyTest {
    @Test
    fun ipcRequestZeroCopyTest() = runBlocking {
        val m1 = object : NativeMicroModule("m1") {
            override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
            }

            override suspend fun _shutdown() {
            }
        }
        val m2 = object : NativeMicroModule("m2") {
            override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
            }

            override suspend fun _shutdown() {
            }
        }

        val channel = NativeMessageChannel<IpcMessage, IpcMessage>();
        val ipc1 = NativeIpc(channel.port1, m1, IPC_ROLE.SERVER);
        val ipc2 = NativeIpc(channel.port2, m2, IPC_ROLE.CLIENT);

        var readableStream = ReadableStream();
        var ipcBody = IpcBodySender.fromStream(readableStream, ipc1);
        var ipcRequest =
            IpcRequest(1, "http://test.com", IpcMethod.POST, IpcHeaders(), ipcBody, ipc1);


        var httpRequestMessage = ipcRequest.toRequest();
        var httpContentStream = httpRequestMessage.body.stream

        assertEquals(httpContentStream, readableStream);
    }
}