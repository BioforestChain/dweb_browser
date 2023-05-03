namespace DwebBrowser.MicroService.Message;

public class IpcStreamData : IpcMessage, IpcStream
{
    [JsonPropertyName("stream_id")]
    public string StreamId { get; set; }

    [JsonPropertyName("data")]
    public object _Data { get; set; }
    public object Data
    {
        get => _Data switch
        {
            JsonElement element => _Data = element.GetString()!, // JSON 模式下，只可能输出字符串格式，不可能是 byte[]
            /// TODO 未来支持 CBOR 的时候，这里可以直接读取出 byte[]
            _ => _Data,
        };
        set => _Data = value;
    }

    [JsonPropertyName("encoding")]
    public IPC_DATA_ENCODING Encoding { get; set; }

    [Obsolete("使用带参数的构造函数", true)]
    public IpcStreamData() : base(IPC_MESSAGE_TYPE.STREAM_DATA)
    {
        /// 给JSON反序列化用的空参数构造函数
    }
    public IpcStreamData(string stream_id, object data, IPC_DATA_ENCODING encoding) : base(IPC_MESSAGE_TYPE.STREAM_DATA)
    {
        StreamId = stream_id;
        Data = data;
        Encoding = encoding;
    }

    public static IpcStreamData FromBinary(string stream_id, byte[] data) =>
        new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY);
    public static IpcStreamData FromBase64(string stream_id, byte[] data) =>
        new IpcStreamData(stream_id, data.ToBase64(), IPC_DATA_ENCODING.BASE64);
    public static IpcStreamData FromUtf8(string stream_id, byte[] data) =>
        FromUtf8(stream_id, data.ToUtf8());
    public static IpcStreamData FromUtf8(string stream_id, string data) =>
        new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.UTF8);

    private LazyBox<byte[]> _binary = new();
    public byte[] Binary
    {
        get { return _binary.GetOrPut(() => EncodingConverter.DataToBinary(Data, Encoding)); }
    }
    private LazyBox<string> _text = new();
    public string Text
    {
        get { return _text.GetOrPut(() => EncodingConverter.DataToText(Data, Encoding)); }
    }
    public IpcStreamData JsonAble() => Encoding switch
    {
        IPC_DATA_ENCODING.BINARY => FromBase64(
            StreamId,
            (Data as byte[])!
        ),
        _ => this
    };
    /// <summary>
    /// Serialize IpcStreamData
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamData</returns>
    public override string ToJson() => JsonSerializer.Serialize(JsonAble());

    /// <summary>
    /// Deserialize IpcStreamData
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamData</param>
    /// <returns>An instance of a IpcStreamData object.</returns>
    public static IpcStreamData? FromJson(string json) => JsonSerializer.Deserialize<IpcStreamData>(json);
}

