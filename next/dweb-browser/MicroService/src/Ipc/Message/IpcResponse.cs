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
    }

    // TODO: FromJson 未完成
    public static IpcResponse FromJson(int req_id, int statusCode, IpcHeaders headers, object jsonAble, Ipc ipc) =>
        FromText(req_id, statusCode, headers.Also(it => it.Init("Content-Type", "application/json")), jsonAble.ToString(), ipc);
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

    public static async Task<IpcResponse> FromResponse(int req_id, HttpResponseMessage response, Ipc ipc) =>
        new IpcResponse(
            req_id,
            (int)response.StatusCode,
            new IpcHeaders(response.Headers, response.Content.Headers),
            response.Content switch
            {
                StringContent stringContent => IpcBodySender.FromText(await stringContent.ReadAsStringAsync(), ipc),
                ByteArrayContent byteArrayContent => IpcBodySender.FromBinary(await byteArrayContent.ReadAsByteArrayAsync(), ipc),
                StreamContent streamContent => IpcBodySender.FromStream(await streamContent.ReadAsStreamAsync(), ipc),
                null => IpcBodySender.FromText("", ipc),
                _ => response.Content.ToString() switch
                {
                    "System.Net.Http.EmptyContent" => IpcBodySender.FromText("", ipc),
                    _ => await response.Content.ReadAsStreamAsync().Let(async streamTask =>
                    {
                        var stream = await streamTask;
                        try
                        {
                            if (stream.Length == 0)
                            {
                                return IpcBodySender.FromText("", ipc);
                            }
                        }
                        catch
                        { // ignore error
                        }
                        return IpcBodySender.FromStream(stream, ipc);
                    })
                }
            },
            ipc);

    public HttpResponseMessage ToResponse() =>
        new HttpResponseMessage((HttpStatusCode)StatusCode).Also(it =>
        {
            switch (Body.Raw)
            {
                case string body:
                    it.Content = new StringContent(body);
                    break;
                case byte[] body:
                    it.Content = new ByteArrayContent(body);
                    break;
                case Stream body:
                    it.Content = new StreamContent(body);
                    break;
                default:
                    throw new Exception(String.Format("invalid body to request: {0}", Body.Raw));
            }

            foreach (var entry in Headers.GetEnumerator())
            {
                if (entry.Key.StartsWith("Content", true, null))
                {
                    it.Content.Headers.Add(entry.Key, entry.Value);
                }
                else
                {
                    it.Headers.TryAddWithoutValidation(entry.Key, entry.Value);
                }
            }
        });

    public IpcResMessage LazyIpcResMessage
    {
        get
        {
            return new Lazy<IpcResMessage>(new Func<IpcResMessage>(() =>
                new IpcResMessage(
                    ReqId,
                    StatusCode,
                    Headers.GetEnumerator().ToDictionary(k => k.Key, v => v.Value),
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
    public IpcResMessage() : base(IPC_MESSAGE_TYPE.RESPONSE)
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
