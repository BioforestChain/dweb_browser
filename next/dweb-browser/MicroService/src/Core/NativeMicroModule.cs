using DwebBrowser.MicroService.Sys.Http.Net;

namespace DwebBrowser.MicroService.Core;

public abstract class NativeMicroModule : MicroModule
{
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

    public NativeMicroModule()
    {
        OnConnect += async (clientIpc, _, _) =>
        {
            clientIpc.OnRequest += async (ipcRequest, _, _) =>
            {
                Console.WriteLine($"NMM/Handler {ipcRequest.Url}");
                var request = ipcRequest.ToRequest();
                var response = await HttpRouter.RoutesWithContext(request, clientIpc);
                await clientIpc.PostMessageAsync(IpcResponse.FromResponse(ipcRequest.ReqId, response, clientIpc));
            };
        };
    }

    // TODO: ResponseRegistry 静态初始化问题未解决
    public class ResponseRegistry
    {
        public static Dictionary<object, Func<object, HttpResponseMessage>> RegMap = new();

        public static void RegistryResponse<T>(T type, Func<T, HttpResponseMessage> handler) =>
            RegMap.TryAdd(type, handler as Func<object, HttpResponseMessage>);

        //static ResponseRegistry()
        //{
        //    RegistryResponse(typeof(byte[]), it =>
        //    {
        //        return new HttpResponseMessage(HttpStatusCode.OK).Also(res =>
        //        {
        //            res.Content = new ByteArrayContent(it);
        //        });
        //    });
        //}

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

        public static HttpResponseMessage AsJson(object result) =>
            new HttpResponseMessage(HttpStatusCode.OK).Also(it =>
            {
                // 设置Json序列化选项
                var options = new JsonSerializerOptions
                {
                    PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
                    WriteIndented = true
                };
                it.Content = new StringContent(JsonSerializer.Serialize(result, options));
                it.Content.Headers.Add("Content-Type", "application/json");
            });
    }

    //public async Task<HttpResponseMessage> DefineHandler(HttpRequestMessage request, Ipc? ipc = null)
    //{
    //    switch (await (HttpRouter.RouterHandler(request, ipc)))
    //    {
    //        case null:
    //            return new HttpResponseMessage(HttpStatusCode.OK);
    //        case HttpResponseMessage response:
    //            return response;
    //        case byte[] result:
    //            return new HttpResponseMessage(HttpStatusCode.OK).Also(it =>
    //            {
    //                it.Content = new StreamContent(new MemoryStream().Let(s =>
    //                {
    //                    s.Write(result, 0, result.Length);
    //                    return s;
    //                }));
    //            });
    //        case Stream stream:
    //            return new HttpResponseMessage(HttpStatusCode.OK).Also(it => it.Content = new StreamContent(stream));
    //        default:
    //            return new HttpResponseMessage(HttpStatusCode.OK);
    //    }
    //}
}
