using System.Net;
using System.Text.Json;
using DwebBrowser.MicroService.Http;
// https://learn.microsoft.com/zh-cn/dotnet/csharp/nullable-references
#nullable enable

namespace DwebBrowser.MicroService.Browser.Jmm;

public class JsMicroModule : MicroModule
{
    static readonly Debugger Console = new("JsMicroModule");

    private const string DWEB_APP = "dwebApps";
    /// <summary>
    /// dweb应用目录
    /// </summary>
    static readonly string DWEB_APP_DIR = Path.Join(
       PathHelper.GetIOSDocumentDirectory(), DWEB_APP);

    /// <summary>
    /// 获取一个应用的安装路径
    /// </summary>
    /// <param name="metadata"></param>
    /// <returns></returns>
    public static string GetInstallPath(JmmMetadata metadata)
    {
        return Path.Join(DWEB_APP_DIR, metadata.Id, metadata.Version);
    }
    /// <summary>
    /// 获取一个应用所有的安装版本
    /// TODO 这里应该返回路径和metadata，所以metadata需要写入到文件里，比如 ${DWEB_APP_DIR}/${id}/sys/metadata.json
    /// </summary>
    /// <param name="metadata"></param>
    /// <returns></returns>
    public static MiniJmmInfo[] GetAllVersions(Mmid id)
    {
        return Directory.GetDirectories(Path.Join(DWEB_APP_DIR, id))
            .Select(fullPath =>
            {
                return new MiniJmmInfo(id, Path.GetFileName(fullPath), fullPath);
            }).ToArray();
    }
    public record class MiniJmmInfo(Mmid Id, string Version, string InstallPath);

    public override List<Dweb_DeepLink> Dweb_deeplinks { get; init; }
    public override IpcSupportProtocols IpcSupportProtocols { get; init; }

    record JsMM(JsMicroModule jmm, Mmid remoteMmid);
    static JsMicroModule()
    {
        if (!Directory.Exists(DWEB_APP_DIR))
        {
            Directory.CreateDirectory(DWEB_APP_DIR);
        }

        var nativeToWhiteList = new List<Mmid> { "js.browser.dweb" };
        NativeConnect.ConnectAdapterManager.Append(async (fromMM, toMM, reason) =>
        {
            JsMM? jsMM = null;
            if (nativeToWhiteList.Contains(toMM.Mmid)) { }
            else if (toMM is JsMicroModule tjmm)
            {
                jsMM = new JsMM(tjmm, fromMM.Mmid);
            }
            else if (fromMM is JsMicroModule fjmm)
            {
                jsMM = new JsMM(fjmm, toMM.Mmid);
            }

            if (jsMM is JsMM jsmm)
            {
                /**
                 * 与 NMM 相比，这里会比较难理解：
                 * 因为这里是直接创建一个 Native2JsIpc 作为 ipcForFromMM，
                 * 而实际上的 ipcForToMM ，是在 js-context 里头去创建的，因此在这里是 一个假的存在
                 *
                 * 也就是说。如果是 jsMM 内部自己去执行一个 connect，那么这里返回的 ipcForFromMM，其实还是通往 js-context 的， 而不是通往 toMM的。
                 * 也就是说，能跟 toMM 通讯的只有 js-context，这里无法通讯。
                 */
                var originIpc = await jsmm.jmm._ipcBridgeAsync(jsmm.remoteMmid);
                await fromMM.BeConnectAsync(originIpc, reason);
                await toMM.BeConnectAsync(originIpc, reason);

                return new ConnectResult(originIpc, originIpc);
            }

            return null;
        }, -1);
    }


    public JsMicroModule(JmmMetadata metadata) : base(metadata.Id)
    {
        Metadata = metadata;
        Dweb_deeplinks = Metadata.Dweb_DeepLinks ?? new();
        IpcSupportProtocols = new()
        {
            Cbor = true,
            Protobuf = false,
            Raw = true
        };
    }

    public JmmMetadata Metadata { get; init; }

    /**
     * <summary>
     * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
     * 所以不会和其它程序所使用的 pid 冲突
     * </summary>
     */
    private string? _processId = null;
    public string Pid = Token.RandomCryptoString(8);

    private async Task<ReadableStreamIpc> _createNativeStreamAsync()
    {
        _processId = Pid;
        var streamIpc = new ReadableStreamIpc(this, "code-server");
        streamIpc.OnRequest += async (request, ipc, _) =>
        {
            var response = request.Uri.AbsolutePath.EndsWith("/")
                ? new PureResponse(HttpStatusCode.Forbidden)
                : await NativeFetchAsync(Metadata.Server.Root + request.Uri.AbsolutePath);

            await ipc.PostMessageAsync(response.ToIpcResponse(request.ReqId, ipc));
        };

        var createIpc_req = new PureRequest(

            new URL("file://js.browser.dweb/create-process")
                .SearchParamsSet("entry", Metadata.Server.Entry)
                .SearchParamsSet("process_id", Pid).Href,
            IpcMethod.Post,
            Body: new PureStreamBody(streamIpc.ReadableStream.Stream));
        var createIpc_res = await NativeFetchAsync(createIpc_req);
        streamIpc.BindIncomeStream(createIpc_res.Body.ToStream());
        this.addToIpcSet(streamIpc);

        return streamIpc;
    }

    private record DnsConnectEvent(Mmid mmid);
    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        Console.Log("Bootstrap", "{0}/{1}", Mmid, Metadata);
        var streamIpc = await _createNativeStreamAsync();

        /**
         * 拿到与js.browser.dweb模块的直连通道，它会将 Worker 中的数据带出来
         */
        var connectResult = await bootstrapContext.Dns.ConnectAsync("js.browser.dweb");
        var fetchIpc = connectResult.IpcForFromMM;

        // 监听关闭事件
        _onCloseJsProcess += async (_) =>
        {
            await streamIpc.Close();
            await fetchIpc.Close();
            _fromMmid_originIpc_map.Clear();
        };

        /**
         * 这里 jmm 的对于 request 的默认处理方式是将这些请求直接代理转发出去
         * TODO 跟 dns 要 jmmMetadata 信息然后进行路由限制 eg: jmmMetadata.permissions.contains(ipcRequest.uri.host) // ["camera.sys.dweb"]
         */
        fetchIpc.OnRequest += async (ipcRequest, ipc, _) =>
        {
            try
            {
                var pureRequest = ipcRequest.ToPureRequest();
                var pureResponse = await NativeFetchAsync(pureRequest);
                var ipcResponse = pureResponse.ToIpcResponse(ipcRequest.ReqId, ipc);
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
        fetchIpc.OnEvent += async (ipcEvent, _, _) =>
        {
            /**
             * 收到要与其它模块进行ipc连接的指令
             */
            if (ipcEvent.Name is "dns/connect")
            {
                var Event = JsonSerializer.Deserialize<DnsConnectEvent>(ipcEvent.Text)!;
                try
                {
                    /**
                     * 模块之间的ipc是单例模式，所以我们必须拿到这个单例，再去做消息转发
                     * 但可以优化的点在于：TODO 我们应该将两个连接的协议进行交集，得到最小通讯协议，然后两个通道就能直接通讯raw数据，而不需要在转发的时候再进行一次编码解码
                     *
                     * 此外这里允许js多次建立ipc连接，因为可能存在多个js线程，它们是共享这个单例ipc的
                     */
                    /**
                     * 向目标模块发起连接，注意，这里是很特殊的，因为我们自定义了 JMM 的连接适配器 connectAdapterManager，
                     * 所以 JsMicroModule 这里作为一个中间模块，是没法直接跟其它模块通讯的。
                     *
                     * TODO 如果有必要，未来需要让 connect 函数支持 force 操作，支持多次连接。
                     */
                    var connectResult = await bootstrapContext.Dns.ConnectAsync(Event.mmid);
                    var targetIpc = connectResult.IpcForFromMM;

                    /// 只要不是我们自己创建的直接连接的通道，就需要我们去 创造直连并进行桥接
                    if (targetIpc.Remote.Mmid != Mmid)
                    {
                        await _ipcBridgeAsync(Event.mmid, targetIpc);
                    }
                }
                catch(Exception err)
                {
                    await _ipcConnectFailAsync(Event.mmid, err);
                }
            }
            else if (ipcEvent.Name is "restart")
            {
                // 调用重启
                bootstrapContext.Dns.Restart(Mmid);
            }
        };

        this.addToIpcSet(streamIpc);
        Console.Log("running!!", Mmid);
    }

    private event Signal? _onCloseJsProcess;

    private Dictionary<Mmid, PromiseOut<Ipc>> _fromMmid_originIpc_map = new();


    class JmmIpc : Native2JsIpc
    {
        public JmmIpc(int port_id, IMicroModuleInfo remote) : base(port_id, remote)
        {
        }
    }
    /// <summary>
    /// 桥接ipc到js内部：
    /// 使用 create-ipc 指令来创建一个代理的 WebMessagePortIpc ，然后我们进行中转
    /// </summary>
    /// <param name="fromMmid"></param>
    /// <param name="targetIpc">如果填充了该参数，说明 targetIpc 是别人用于通讯的对象，那么我们需要主动将 targetIpc与originIpc进行桥接；如果没有填充，我们返回的 originIpc 可以直接与 Worker 通讯</param>
    /// <returns></returns>
    private PromiseOut<Ipc> _ipcBridge(Mmid fromMmid, Ipc? targetIpc = null) =>
        _fromMmid_originIpc_map.GetValueOrPut(fromMmid, () =>
            new PromiseOut<Ipc>().Also(async po =>
            {
                try
                {
                    /**
                     * 向js模块发起连接
                     */
                    var portId = await (await NativeFetchAsync(
                        new URL("file://js.browser.dweb/create-ipc")
                        .SearchParamsSet("process_id", Pid).SearchParamsSet("mmid", fromMmid)))
                        .IntAsync() ?? throw new Exception("invalid Native2JsIpc.PortId");

                    var originIpc = new JmmIpc(portId, this);

                    /// 如果传入了 targetIpc，那么启动桥接模式，我们会中转所有的消息给 targetIpc，
                    /// 包括关闭，那么这个 targetIpc 理论上就可以作为 originIpc 的代理
                    if (targetIpc is not null)
                    {
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

                        /**
                         * 监听关闭事件
                         */
                        originIpc.OnClose += async (_) =>
                        {
                            _fromMmid_originIpc_map.Remove(originIpc.Remote.Mmid);
                            await targetIpc.Close();
                        };
                        targetIpc.OnClose += async (_) =>
                        {
                            _fromMmid_originIpc_map.Remove(targetIpc.Remote.Mmid);
                            await originIpc.Close();
                        };
                    }
                    po.Resolve(originIpc);
                }
                catch (Exception e)
                {
                    Console.Log("_ipcBridge", e.Message);
                    po.Reject(e.Message);
                }
            }));

    private Task<Ipc> _ipcBridgeAsync(Mmid fromMmid, Ipc? targetIpc = null) =>
        _ipcBridge(fromMmid, targetIpc).WaitPromiseAsync();

    private async Task<bool> _ipcConnectFailAsync(Mmid mmid, Exception err)
    {
        var errMessage = string.Format("{0}\n{1}", err.Message, err.StackTrace);
        /**
         * 向js模块发起连接
         */
        var url = new URL("file://js.browser.dweb/create-ipc-fail");
        return await (await NativeFetchAsync(
                url.SearchParamsSet("process_id", _processId)
                   .SearchParamsSet("mmid", mmid)
                   .SearchParamsSet("reason", errMessage).Uri
                )).BoolAsync();
    }

    protected override async Task _shutdownAsync()
    {
        Console.Log("closeJsProcessSignal emit", string.Format("{0}/{1}", Mmid, Metadata));
        await NativeFetchAsync("file://js.browser.dweb/close-process");
        await (_onCloseJsProcess?.Emit()).ForAwait();
        _onCloseJsProcess = null;
        _processId = null;
    }
}

