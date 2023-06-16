namespace DwebBrowser.MicroService.Message;

public class IpcEvent : IpcMessage
{
    [JsonPropertyName("name")]
    public string Name { get; set; }
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
    public IpcEvent() : base(IPC_MESSAGE_TYPE.EVENT)
    {
        /// 给JSON反序列化用的空参数构造函数
    }
    public IpcEvent(string name, object data, IPC_DATA_ENCODING encoding) : base(IPC_MESSAGE_TYPE.EVENT)
    {
        Name = name;
        Data = data;
        Encoding = encoding;
    }

    public static IpcEvent FromBinary(string name, byte[] data) => new IpcEvent(name, data, IPC_DATA_ENCODING.BINARY);
    public static IpcEvent FromBase64(string name, byte[] data) =>
        new IpcEvent(name, Convert.ToBase64String(data), IPC_DATA_ENCODING.BASE64);
    public static IpcEvent FromUtf8(string name, byte[] data) => FromUtf8(name, data.ToUtf8());
    public static IpcEvent FromUtf8(string name, string data) => new IpcEvent(name, data, IPC_DATA_ENCODING.UTF8);

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
    public IpcEvent JsonAble() => Encoding switch
    {
        IPC_DATA_ENCODING.BINARY => FromBase64(
            Name,
            (Data as byte[])!
        ),
        _ => this
    };
    /// <summary>
    /// Serialize IpcEvent
    /// </summary>
    /// <returns>JSON string representation of the IpcEvent</returns>
    public override string ToJson() => JsonSerializer.Serialize(JsonAble());

    /// <summary>
    /// Deserialize IpcEvent
    /// </summary>
    /// <param name="json">JSON string representation of IpcEvent</param>
    /// <returns>An instance of a IpcEvent object.</returns>
    public static IpcEvent? FromJson(string json) => JsonSerializer.Deserialize<IpcEvent>(json);
}
