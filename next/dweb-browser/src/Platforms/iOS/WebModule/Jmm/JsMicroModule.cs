using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Message;
using DwebBrowser.Helper;
using DwebBrowser.IpcWeb;
using System.Net;
using System.Text.Json;

// https://learn.microsoft.com/zh-cn/dotnet/csharp/nullable-references
#nullable enable

namespace DwebBrowser.WebModule.Jmm;

public class JsMicroModule : MicroModule
{
    static JsMicroModule()
    {
        NativeConnect.ConnectAdapterManager.Append(async (fromMM, toMM, reason) =>
        {
            if (toMM is JsMicroModule jmm)
            {
                var pid = jmm._processId ?? throw new Exception(String.Format("JMM: {0} no ready", toMM.Mmid));
                /**
                 * 向js模块发起连接
                 */
                var portId = await (await toMM.NativeFetchAsync(new Uri("file://js.sys.dweb/create-ipc")
                    .AppendQuery("process_id", pid)
                    .AppendQuery("mmid", fromMM.Mmid))).IntAsync();

                var originIpc = new Native2JsIpc(portId, toMM);
                // 同样要被生命周期管理销毁
                await toMM.BeConnectAsync(originIpc, reason);

                return new ConnectResult(originIpc, null);
            }

            return null;
        });
    }

    public JsMicroModule(JmmMetadata metadata): base(metadata.Id)
    {
        Metadata = metadata;
    }

    public JmmMetadata Metadata { get; init; }

    /**
     * <summary>
     * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
     * 所以不会和其它程序所使用的 pid 冲突
     * </summary>
     */
    private string? _processId = null;

    private record DnsConnectEvent(Mmid mmid);
    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        Console.WriteLine(String.Format("bootstrap... {0}/{1}", Mmid, Metadata));
        var pid = Token.RandomCryptoString(8);
        _processId = pid;
        var streamIpc = new ReadableStreamIpc(this, "code-server");
        streamIpc.OnRequest += async (request, ipc, _) =>
        {
            var response = request.Uri.AbsolutePath.EndsWith("/")
                ? new HttpResponseMessage(HttpStatusCode.Forbidden)
                : await NativeFetchAsync(Metadata.Server.Root + request.Uri.AbsolutePath);

            await ipc.PostMessageAsync(await IpcResponse.FromResponse(request.ReqId, response, ipc));
        };

        //streamIpc.BindIncomeStream(await (await NativeFetchAsync(
        //        new HttpRequestMessage(
        //            HttpMethod.Get, new Uri("file://js.sys.dweb/create-process")).Also(
        //            it => { it.Content = new StreamContent(streamIpc.Stream.Stream); })
        //        )).Content.ReadAsStreamAsync());
        streamIpc.BindIncomeStream(await (await NativeFetchAsync(
                new HttpRequestMessage(
                    HttpMethod.Post,
                    new Uri("file://js.sys.dweb/create-process")
                        .AppendQuery("entry", Metadata.Server.Entry)
                        .AppendQuery("process_id", pid))
                .Also(
                    it => { it.Content = new StreamContent(streamIpc.Stream.Stream); })
                )).StreamAsync());

        // 监听关闭事件
        _onCloseJsProcess += (_) => streamIpc.Close();

        /**
         * 拿到与js.sys.dweb模块的直连通道，它会将 Worker 中的数据带出来
         */
        var connectResult = await bootstrapContext.Dns.ConnectAsync("js.sys.dweb");
        var jsIpc = connectResult.IpcForFromMM;
        /**
         * 这里 jmm 的对于 request 的默认处理方式是将这些请求直接代理转发出去
         * TODO 跟 dns 要 jmmMetadata 信息然后进行路由限制 eg: jmmMetadata.permissions.contains(ipcRequest.uri.host) // ["camera.sys.dweb"]
         */
        jsIpc.OnRequest += async (ipcRequest, ipc, _) =>
        {
            try
            {
                var request = ipcRequest.ToRequest();
                var response = await NativeFetchAsync(request);
                var ipcResponse = await IpcResponse.FromResponse(ipcRequest.ReqId, response, ipc);
                await ipc.PostMessageAsync(ipcResponse);
            }
            catch (Exception ex)
            {
                await ipc.PostMessageAsync(
                    IpcResponse.FromText(
                        ipcRequest.ReqId, 500, new IpcHeaders(), ex.Message ?? "", ipc));
            }
        };

        /**
         * 收到 Worker 的事件，如果是指令，执行一些特定的操作
         */
        jsIpc.OnEvent += async (ipcEvent, _, _) =>
        {
            /**
             * 收到要与其它模块进行ipc连接的指令
             */
            if (ipcEvent.Name == "dns/connect")
            {
                _ = Task.Run(async () =>
                {
                    var Event = JsonSerializer.Deserialize<DnsConnectEvent>(ipcEvent.Text);
                    /**
                     * 模块之间的ipc是单例模式，所以我们必须拿到这个单例，再去做消息转发
                     * 但可以优化的点在于：TODO 我们应该将两个连接的协议进行交集，得到最小通讯协议，然后两个通道就能直接通讯raw数据，而不需要在转发的时候再进行一次编码解码
                     *
                     * 此外这里允许js多次建立ipc连接，因为可能存在多个js线程，它们是共享这个单例ipc的
                     */
                    /**
                     * 向目标模块发起连接
                     */
                    var connectResult = await bootstrapContext.Dns.ConnectAsync(Event!.mmid);
                    var targetIpc = connectResult.IpcForFromMM;
                    /**
                     * 向js模块发起连接
                     */
                    var portId = await (await NativeFetchAsync(new Uri("file://js.sys.dweb/create-ipc")
                            .AppendQuery("process_id", pid)
                            .AppendQuery("mmid", Event.mmid))).IntAsync();

                    var originIpc = new Native2JsIpc(portId, this).Also(it =>
                    {
                        BeConnectAsync(it, new HttpRequestMessage(HttpMethod.Get, String.Format("file://{0}/event/dns/connect", Mmid)));
                    });

                    /**
                     * 将两个消息通道间接互联
                     */
                    originIpc.OnMessage += async (ipcMessage, _, _) =>
                    {
                        await targetIpc.PostMessageAsync(ipcMessage);
                    };
                    targetIpc.OnMessage += async (ipcMessage, _, _) =>
                    {
                        await originIpc.PostMessageAsync(ipcMessage);
                    };
                });
            }
        };
    }

    protected override async Task _onActivityAsync(IpcEvent Event, Ipc ipc)
    {

    }

    private event Signal? _onCloseJsProcess;

    protected override async Task _shutdownAsync()
    {
        await (_onCloseJsProcess?.Emit()).ForAwait();
        _processId = null;
    }
}

