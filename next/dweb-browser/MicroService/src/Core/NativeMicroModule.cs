using System;
using System.Net;

namespace DwebBrowser.MicroService.Core;

public abstract class NativeMicroModule : MicroModule
{
    static NativeMicroModule()
    {
        NativeConnect.ConnectAdapterManager.Append((fromMM, toMM, reason) =>
        {
            if (toMM is NativeMicroModule nmm)
            {
                var channel = new NativeMessageChannel<IpcMessage, IpcMessage>();
                var toNativeIpc = new NativeIpc(channel.Port1, fromMM, IPC_ROLE.SERVER);
                var fromNativeIpc = new NativeIpc(channel.Port2, nmm, IPC_ROLE.CLIENT);
                fromMM.BeConnectAsync(fromNativeIpc, reason);
                nmm.BeConnectAsync(toNativeIpc, reason);
                return new ConnectResult(fromNativeIpc, toNativeIpc);
            }

            return null;
        });

    }

    public class ResponseRegistry
    {
        public static Dictionary<object, Func<object, HttpResponseMessage>> RegMap = new();

        public static void RegistryResponse<T>(Type type, Func<T, HttpResponseMessage> handler) =>
            RegMap.TryAdd(type, handler as Func<object, HttpResponseMessage>);

        //public static HttpResponseMessage Handler(object result)
        //{

        //}

        public HttpResponseMessage AsJson(object result, Type type) =>
            new HttpResponseMessage(HttpStatusCode.OK).Also(it =>
            {
                // 设置Json序列化选项
                var options = new JsonSerializerOptions
                {
                    PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
                    WriteIndented = true
                };
                it.Content = new StringContent(JsonSerializer.Serialize(result, options));
            });
    }
}
