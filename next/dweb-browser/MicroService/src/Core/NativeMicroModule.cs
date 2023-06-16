namespace DwebBrowser.MicroService.Core;

public abstract class NativeMicroModule : MicroModule
{
    static readonly Debugger Console = new("NMM");
    protected HttpRouter HttpRouter = new();
    public override List<Dweb_DeepLink> Dweb_deeplinks { get; init; } = new();
    public override IpcSupportProtocols IpcSupportProtocols { get; init; } = new()
    {
        Cbor = true,
        Protobuf = true,
        Raw = true
    };

    static NativeMicroModule()
    {
        NativeConnect.ConnectAdapterManager.Append(async (fromMM, toMM, reason) =>
        {
            if (toMM is NativeMicroModule nmm)
            {
                var channel = new NativeMessageChannel<IpcMessage, IpcMessage>();
                var toNativeIpc = new NativeIpc(channel.Port1, fromMM, IPC_ROLE.SERVER);
                var fromNativeIpc = new NativeIpc(channel.Port2, nmm, IPC_ROLE.CLIENT);
                await fromMM.BeConnectAsync(fromNativeIpc, reason);
                await nmm.BeConnectAsync(toNativeIpc, reason);
                return new ConnectResult(fromNativeIpc, toNativeIpc);
            }

            return null;
        });

    }

    public NativeMicroModule(Mmid mmid) : base(mmid)
    {
        OnConnect += async (clientIpc, _, _) =>
        {
            clientIpc.OnRequest += async (ipcRequest, _, _) =>
            {
                Console.Log("OnRequest", "Handler {0}", ipcRequest.Url);
                var pureResponse = await HttpRouter.RoutesWithContext(ipcRequest.ToPureRequest(), clientIpc);
                await clientIpc.PostMessageAsync(pureResponse.ToIpcResponse(ipcRequest.ReqId, clientIpc));
            };
        };
    }


    /// <summary>
    /// 在关闭后，路由会被完全清空，释放
    /// </summary>
    /// <returns></returns>
    protected new Task _afterShutdownAsync()
    {
        HttpRouter.ClearRoutes();
        return base._afterShutdownAsync();
    }
}

public interface IToJsonAble
{
    public string ToJson();
}