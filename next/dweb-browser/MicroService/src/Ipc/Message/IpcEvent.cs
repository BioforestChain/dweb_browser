namespace DwebBrowser.MicroService.Message;

public class IpcEvent : IpcMessage
{
    [JsonPropertyName("name")]
    public string Name { get; set; }
    [JsonPropertyName("data")]
    public object Data { get; set; }
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

        _binary = new Lazy<byte[]>(() => EncodingConverter.DataToBinary(Data, Encoding), true);
        _text = new Lazy<string>(() => EncodingConverter.DataToText(Data, Encoding), true);
    }

    public static IpcEvent FromBinary(string name, byte[] data) => new IpcEvent(name, data, IPC_DATA_ENCODING.BINARY);
    public static IpcEvent FromBase64(string name, byte[] data) =>
        new IpcEvent(name, Convert.ToBase64String(data), IPC_DATA_ENCODING.BASE64);
    public static IpcEvent FromUtf8(string name, byte[] data) => FromUtf8(name, data.ToUtf8());
    public static IpcEvent FromUtf8(string name, string data) => new IpcEvent(name, data, IPC_DATA_ENCODING.UTF8);

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

    /// <summary>
    /// Serialize IpcEvent
    /// </summary>
    /// <returns>JSON string representation of the IpcEvent</returns>
    public override string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcEvent
    /// </summary>
    /// <param name="json">JSON string representation of IpcEvent</param>
    /// <returns>An instance of a IpcEvent object.</returns>
    public static IpcEvent? FromJson(string json) => JsonSerializer.Deserialize<IpcEvent>(json);
}
