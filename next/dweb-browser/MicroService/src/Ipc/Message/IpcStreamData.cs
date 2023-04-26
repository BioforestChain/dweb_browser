namespace DwebBrowser.MicroService.Message;

public class IpcStreamData : IpcMessage, IpcStream
{
    [JsonPropertyName("stream_id")]
    public string StreamId { get; set; }

    [JsonPropertyName("data")]
    public object Data { get; set; }

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

        _binary = new Lazy<byte[]>(() => EncodingConverter.DataToBinary(Data, Encoding), true);
        _text = new Lazy<string>(() => EncodingConverter.DataToText(Data, Encoding), true);
    }

    public static IpcStreamData FromBinary(string stream_id, byte[] data) =>
        new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY);
    public static IpcStreamData FromBase64(string stream_id, byte[] data) =>
        new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BASE64);
    public static IpcStreamData FromUtf8(string stream_id, byte[] data) =>
        FromUtf8(stream_id, data.ToUtf8());
    public static IpcStreamData FromUtf8(string stream_id, string data) =>
        new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.UTF8);

    private Lazy<byte[]> _binary;
    public byte[] Binary
    {
        get { return _binary.Value; }
    }
    private Lazy<string> _text;
    public string Text
    {
        get { return _text.Value; }
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

