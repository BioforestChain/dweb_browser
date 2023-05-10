using System;
using System.Net;
using System.Net.Http;
namespace DwebBrowser.MicroService.Message;

public class IpcResponse : IpcMessage
{
    public int ReqId { get; init; }
    public int StatusCode { get; init; }
    public IpcHeaders Headers { get; set; }
    public IpcBody Body { get; set; }
    public Ipc Ipc { get; init; }

    public IpcResponse(int req_id, int statusCode, IpcHeaders headers, IpcBody body, Ipc ipc) : base(IPC_MESSAGE_TYPE.RESPONSE)
    {
        ReqId = req_id;
        StatusCode = statusCode;
        Headers = headers;
        Body = body;
        Ipc = ipc;

        if(body is IpcBodySender ipcBodySender)
        {
            IpcBodySender.IPC.UsableByIpc(ipc, ipcBodySender);
        }
    }

    // TODO: FromJson 未完成
    public static IpcResponse FromJson(int req_id, int statusCode, IpcHeaders headers, IToJsonAble jsonAble, Ipc ipc) =>
        FromText(req_id, statusCode, headers.Also(it => it.Init("Content-Type", "application/json")), jsonAble.ToJson(), ipc);
    public static IpcResponse FromText(int req_id, int statusCode, IpcHeaders headers, string text, Ipc ipc) =>
        new IpcResponse(req_id, statusCode, headers.Also(it =>
            it.Init("Content-Type", "text/plain")), IpcBodySender.FromText(text, ipc), ipc);

    public static IpcResponse FromBinary(int req_id, int statusCode, IpcHeaders headers, byte[] binary, Ipc ipc) =>
        new IpcResponse(
            req_id,
            statusCode,
            headers.Also(it =>
            {
                it.Init("Content-Type", "application/octet-stream");
                it.Init("Content-Length", binary.Length.ToString());
            }),
            IpcBodySender.FromBinary(binary, ipc),
            ipc);

    public static IpcResponse FromStream(int req_id, int statusCode, IpcHeaders headers, Stream stream, Ipc ipc) =>
        new IpcResponse(
            req_id,
            statusCode,
            headers.Also(it => it.Init("Content-Type", "application/octet-stream")),
            IpcBodySender.FromStream(stream, ipc),
            ipc);

    public IpcResMessage LazyIpcResMessage
    {
        get
        {
            return new Lazy<IpcResMessage>(new Func<IpcResMessage>(() =>
                new IpcResMessage(
                    ReqId,
                    StatusCode,
                    Headers.ToDictionary(),
                    Body.MetaBody)), true).Value;
        }
    }
}


public class IpcResMessage : IpcMessage
{
    [JsonPropertyName("req_id")]
    public int ReqId { get; set; }
    [JsonPropertyName("statusCode")]
    public int StatusCode { get; set; }
    [JsonPropertyName("headers")]
    public Dictionary<string, string> Headers { get; set; }
    [JsonPropertyName("metaBody")]
    public MetaBody MetaBody { get; set; }

    [Obsolete("使用带参数的构造函数", true)]
#pragma warning disable CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    public IpcResMessage() : base(IPC_MESSAGE_TYPE.RESPONSE)
#pragma warning restore CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    {
        /// 给JSON反序列化用的空参数构造函数
    }
    public IpcResMessage(int req_id, int statusCode, Dictionary<string, string> headers, MetaBody metaBody) : base(IPC_MESSAGE_TYPE.RESPONSE)
    {
        ReqId = req_id;
        StatusCode = statusCode;
        Headers = headers;
        MetaBody = metaBody;
    }

    public IpcResMessage JsonAble() => MetaBody.JsonAble().Let(metaBody =>
    {
        if (metaBody != MetaBody)
        {
            return new IpcResMessage(ReqId, StatusCode, Headers, metaBody);
        }
        return this;
    });


    /// <summary>
    /// Serialize IpcReqMessage
    /// </summary>
    /// <returns>JSON string representation of the IpcReqMessage</returns>
    public override string ToJson() => JsonSerializer.Serialize(JsonAble());

    /// <summary>
    /// Deserialize IpcReqMessage
    /// </summary>
    /// <param name="json">JSON string representation of IpcReqMessage</param>
    /// <returns>An instance of a IpcReqMessage object.</returns>
    public static new IpcResMessage? FromJson(string json) => JsonSerializer.Deserialize<IpcResMessage>(json);
}
