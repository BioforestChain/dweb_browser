using System;
using DwebBrowser.MicroService.Http;

namespace DwebBrowser.MicroService.Core;


// TODO: ResponseRegistry 静态初始化问题未解决
public static class ResponseRegistry
{
    static Debugger Console = new("ResponseRegistry");
    static readonly Dictionary<Type, Func<object, PureResponse>> RegMap = new();

    public static void RegistryResponse<T>(Type type, Func<T, PureResponse> handler)
    {
        RegMap.Add(type, obj => handler((T)obj));
    }

    static ResponseRegistry()
    {
        /// 注册基础的底层的类型

        RegistryResponse<byte[]>(typeof(byte[]), item =>
        {
            return new PureResponse(HttpStatusCode.OK, Body: new PureByteArrayBody(item));
        });
        RegistryResponse<Stream>(typeof(Stream), item =>
        {
            return new PureResponse(HttpStatusCode.OK, Body: new PureStreamBody(item));
        });
    }

    public static void RegistryJsonAble<T>(Type type, Func<T, object> handler)
    {
        RegistryResponse<T>(type, item => AsJson(handler(item)));
    }

    public static PureResponse Handler(object result)
    {
        dynamic handler;
        switch (handler = RegMap.GetValueOrDefault(result.GetType()))
        {
            case null:
                var superClassType = result.GetType().BaseType; // 这里要声明在 while 循环外，因为要循环更新
                while (superClassType is not null)
                {
                    // 尝试寻找继承关系
                    switch (handler = RegMap.GetValueOrDefault(superClassType))
                    {
                        case null:
                            superClassType = superClassType.BaseType;
                            break;
                        default:
                            return handler(result);
                    }
                }

                // 否则默认当成JSON来返回
                return AsJson(result);
            default:
                return handler(result);
        }
    }

    static PureResponse AsJson(object result) => new PureResponse(
        HttpStatusCode.OK,
        new IpcHeaders().Set("Content-Type", "application/json"),
        new PureUtf8StringBody(result switch
        {
            IToJsonAble toJsonAble => toJsonAble.ToJson(),
            _ => JsonSerializer.Serialize(result)
        }));
}
