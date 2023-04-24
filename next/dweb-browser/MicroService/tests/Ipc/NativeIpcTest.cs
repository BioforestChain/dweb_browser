using System.Threading;
using System.Threading.Tasks;
using DwebBrowser.MicroService.Core;

namespace DwebBrowser.MicroServiceTests;

public class NativeIpcTest
{
    class M1 : NativeMicroModule
    {
        public M1() : base("m1")
        { }
        protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
        {
            throw new NotImplementedException();
        }

        protected override Task _onActivityAsync(IpcEvent Event, Ipc ipc)
        {
            throw new NotImplementedException();
        }

        protected override Task _shutdownAsync()
        {
            throw new NotImplementedException();
        }
    }

    class M2 : NativeMicroModule
    {
        public M2() : base("m2")
        { }
        protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
        {
            throw new NotImplementedException();
        }

        protected override Task _onActivityAsync(IpcEvent Event, Ipc ipc)
        {
            throw new NotImplementedException();
        }

        protected override Task _shutdownAsync()
        {
            throw new NotImplementedException();
        }
    }

    [Fact]
    public async Task OnRequest_NativeIpcBase_ReturnSuccess()
    {
        var channel = new NativeMessageChannel<IpcMessage, IpcMessage>();
        var m1 = new M1();
        var m2 = new M2();
        var ipc1 = new NativeIpc(channel.Port1, m1, IPC_ROLE.SERVER);
        var ipc2 = new NativeIpc(channel.Port2, m2, IPC_ROLE.CLIENT);

        ipc1.OnRequest += async (req, ipc, _) =>
        {
            await Task.Delay(200);
            await ipc.PostMessageAsync(
                IpcResponse.FromText(
                    req.ReqId,
                    200,
                    new IpcHeaders(),
                    String.Format("ECHO: {0}", req.Body.Text),
                    ipc));
        };

        await Task.Delay(100);
        foreach (var j in Enumerable.Range(1, 10))
        {
            Debug.WriteLine(String.Format("开始发送 ${0}", j));
            var req = new HttpRequestMessage(HttpMethod.Post, "https://www.baidu.com/").Also(it => it.Content = new ByteArrayContent(String.Format("hi-{0}", j).ToUtf8ByteArray()));
            Debug.WriteLine(String.Format("req {0}", req));
            var res = await ipc2.Request(req);
            Debug.WriteLine(String.Format("res {0}", res));
            Assert.Equal(await res.TextAsync(), String.Format("ECHO: {0}", await req.Content.ReadAsStringAsync()));
        }

        await ipc2.Close();
    }
}

