using System;
using static DwebBrowser.MicroServiceTests.NativeIpcTest;

namespace DwebBrowser.MicroServiceTests;

public class IpcBodyZeroCopyTest
{

    [Fact]
    public async void IpcRequestZeroCopyTest()
    {
        var channel = new NativeMessageChannel<IpcMessage, IpcMessage>();
        var m1 = new M1();
        var m2 = new M2();
        var ipc1 = new NativeIpc(channel.Port1, m1, IPC_ROLE.SERVER);
        var ipc2 = new NativeIpc(channel.Port2, m2, IPC_ROLE.CLIENT);

        var readableStream = new ReadableStream();
        var ipcBody = IpcBodySender.FromStream(readableStream.Stream, ipc1);
        var ipcRequest = new IpcRequest(1, "http://test.com", IpcMethod.Post, new(), ipcBody, ipc1);


        var httpRequestMessage = ipcRequest.ToRequest();
        //var httpContentStream = await httpRequestMessage.Content!.ReadAsStreamAsync();
        var httpContentStream = httpRequestMessage.Content!.ReadAsStream();

        Assert.Equal(httpContentStream, readableStream.Stream);
    }
}

