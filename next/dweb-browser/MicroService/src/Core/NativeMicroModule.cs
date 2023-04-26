using DwebBrowser.Helper;

namespace DwebBrowser.MicroService.Core;

public abstract class NativeMicroModule : MicroModule
{
    protected HttpRouter HttpRouter = new();

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
                Console.WriteLine(String.Format("NMM/Handler {0}", ipcRequest.Url));
                var request = ipcRequest.ToRequest();
                var response = await HttpRouter.RoutesWithContext(request, clientIpc);
                await clientIpc.PostMessageAsync(await IpcResponse.FromResponse(ipcRequest.ReqId, response, clientIpc));
            };
        };
    }

    // TODO: ResponseRegistry 静态初始化问题未解决
    public static class ResponseRegistry
    {
        static readonly Dictionary<Type, Func<object, HttpResponseMessage>> RegMap = new();

        public static void RegistryResponse<T>(Type type, Func<T, HttpResponseMessage> handler)
        {
            RegMap.Add(type, obj => handler((T)obj));
        }

        static ResponseRegistry()
        {
            RegistryResponse<byte[]>(typeof(byte[]), item =>
                new HttpResponseMessage(HttpStatusCode.OK).Also(res => res.Content = new StreamContent(new MemoryStream(item))));
            RegistryResponse<Stream>(typeof(Stream), item =>
                new HttpResponseMessage(HttpStatusCode.OK).Also(res => res.Content = new StreamContent(item)));
        }

        public static void RegistryJsonAble<T>(Type type, Func<T, object> handler)
        {
            RegistryResponse<T>(type, item => AsJson(handler(item)));
        }

        public static HttpResponseMessage Handler(object result)
        {
            switch (RegMap.GetValueOrDefault(result.GetType()))
            {
                case null:
                    var superClassType = result.GetType().BaseType;

                    while (superClassType != null)
                    {
                        // 尝试寻找继承关系
                        switch (RegMap.GetValueOrDefault(superClassType))
                        {
                            case null:
                                superClassType = superClassType.BaseType;
                                break;
                            default:
                                return Handler(result);
                        }
                    }

                    // 否则默认当成JSON来返回
                    return AsJson(result);
                default:
                    return Handler(result);
            }
        }

        static HttpResponseMessage AsJson(object result) =>
            new HttpResponseMessage(HttpStatusCode.OK).Also(res =>
            {
                Console.WriteLine("AsJson:" + result);
                var jsonString = result switch
                {
                    IToJsonAble toJsonAble => toJsonAble.ToJson(),
                    _ => JsonSerializer.Serialize(result)
                };
                res.Content = new StringContent(jsonString);
                res.Content.Headers.ContentType = new("application/json");
            });
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