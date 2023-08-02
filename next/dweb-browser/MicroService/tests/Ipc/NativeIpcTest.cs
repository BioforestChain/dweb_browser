using System.Threading;
using System.Threading.Tasks;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService;

namespace DwebBrowser.MicroServiceTests;

public class NativeIpcTest
{
    public class M1 : NativeMicroModule
    {
        public M1() : base("m1", "m1")
        { }
        protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
        {
            throw new NotImplementedException();
        }

        protected override Task _shutdownAsync()
        {
            throw new NotImplementedException();
        }
    }

    public class M2 : NativeMicroModule
    {
        public M2() : base("m2", "m2")
        { }
        protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
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
                    string.Format("ECHO: {0}", req.Body.Text),
                    ipc));
        };

        await Task.Delay(100);
        foreach (var j in Enumerable.Range(1, 10))
        {
            Debug.WriteLine(string.Format("开始发送 ${0}", j));
            var req = new PureRequest("https://www.baidu.com/", IpcMethod.Post, Body: new PureUtf8StringBody(string.Format("hi-{0}", j)));
            Debug.WriteLine(string.Format("req {0}", req));
            var res = await ipc2.Request(req);
            Debug.WriteLine(string.Format("res {0}", res));
            Assert.Equal(await res.TextAsync(), string.Format("ECHO: {0}", req.Body.ToUtf8String()));
        }

        await ipc2.Close();
    }

    [Fact]
    public async Task IpcOnClose()
    {
        var channel = new NativeMessageChannel<IpcMessage, IpcMessage>();
        var m1 = new M1();
        var m2 = new M2();
        var ipc1 = new NativeIpc(channel.Port1, m1, IPC_ROLE.SERVER);
        var ipc2 = new NativeIpc(channel.Port2, m2, IPC_ROLE.CLIENT);

        var t = 0;
        ipc1.OnClose += async (_) =>
        {
            t += 1;
            Debug.WriteLine(string.Format("closed {0}", ipc1.Remote.Mmid));
        };
        ipc2.OnClose += async (_) =>
        {
            t += 1;
            Debug.WriteLine(string.Format("closed {0}", ipc2.Remote.Mmid));
        };

        await ipc1.Close();

        Assert.Equal(2, t);
    }
}

