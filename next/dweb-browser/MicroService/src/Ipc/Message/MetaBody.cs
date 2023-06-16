namespace DwebBrowser.MicroService.Message;

public class MetaBody : IToJsonAble
{
    /**
     * <summary>
     * 类型信息，包含了 编码信息 与 形态信息
     * 编码信息是对 data 的解释
     * 形态信息（流、内联）是对 "是否启用 streamId" 的描述（注意，流也可以内联第一帧的数据）
     * </summary>
     */
    [JsonPropertyName("type")]
    public IPC_META_BODY_TYPE Type { get; set; }
    [JsonPropertyName("senderUid")]
    public int SenderUid { get; set; }
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
    [JsonPropertyName("streamId")]
    public string? StreamId { get; set; } = null;
    [JsonPropertyName("receiverUid")]
    public int? ReceiverUid { get; set; } = null;

    /**
     * <summary>
     * 唯一id，指代这个数据的句柄
     *
     * 需要使用这个值对应的数据进行缓存操作
     * 远端可以发送句柄回来，这样可以省去一些数据的回传延迟。
     * </summary>
     */
    private string? _metaId;
    [JsonPropertyName("metaId")]
    public string MetaId
    {
        get
        {
            if (_metaId is null)
            {
                _metaId = Token.RandomCryptoString(8);
            }
            return _metaId;
        }
        set => _metaId = value;
    }

    //[Obsolete("使用带参数的构造函数", true)]
    public MetaBody()
    {
        /// 给JSON反序列化用的空参数构造函数
    }

    public MetaBody(
        IPC_META_BODY_TYPE type,
        int senderUid,
        object data,
        string? streamId = null,
        int? receiverUid = null)
    {
        Type = type;
        SenderUid = senderUid;
        Data = data;
        StreamId = streamId;
        ReceiverUid = receiverUid;
    }

    [Flags]
    public enum IPC_META_BODY_TYPE : int
    {
        /** <summary>流</summary> */
        STREAM_ID = 0,

        /** <summary>内联数据</summary> */
        INLINE = 1,

        /** <summary>文本 json html 等</summary> */
        STREAM_WITH_TEXT = STREAM_ID | IPC_DATA_ENCODING.UTF8,

        /** <summary>使用文本表示的二进制</summary> */
        STREAM_WITH_BASE64 = STREAM_ID | IPC_DATA_ENCODING.BASE64,

        /** <summary>二进制</summary> */
        STREAM_WITH_BINARY = STREAM_ID | IPC_DATA_ENCODING.BINARY,

        /** <summary>文本 json html 等</summary> */
        INLINE_TEXT = INLINE | IPC_DATA_ENCODING.UTF8,

        /** <summary>使用文本表示的二进制</summary> */
        INLINE_BASE64 = INLINE | IPC_DATA_ENCODING.BASE64,

        /** <summary>二进制</summary> */
        INLINE_BINARY = INLINE | IPC_DATA_ENCODING.BINARY,
    }

    LazyBox<IPC_DATA_ENCODING> _Type_Encoding = new();
    public IPC_DATA_ENCODING Type_Encoding
    {
        get => _Type_Encoding.GetOrPut(() =>
            (IPC_DATA_ENCODING)(((int)Type) & 0b11111110)
        );
    }

    LazyBox<bool> _Type_IsInline = new();
    public bool Type_IsInline
    {
        get => _Type_IsInline.GetOrPut(() => ((int)Type & 1) == 1);
    }

    LazyBox<bool> _Type_IsStream = new();
    public bool Type_IsStream
    {
        get => _Type_IsStream.GetOrPut(() => ((int)Type & 1) == 0);
    }

    public static MetaBody FromText(
        int senderUid,
        string data,
        string? streamId = null,
        int? receiverUid = null
        ) => new MetaBody(
            type: streamId is null ? IPC_META_BODY_TYPE.INLINE_TEXT : IPC_META_BODY_TYPE.STREAM_WITH_TEXT,
            senderUid: senderUid,
            data: data,
            streamId: streamId,
            receiverUid: receiverUid
        );

    public static MetaBody FromBase64(
        int senderUid,
        string data,
        string? streamId = null,
        int? receiverUid = null
        ) => new MetaBody(
            type: streamId is null ? IPC_META_BODY_TYPE.INLINE_BASE64 : IPC_META_BODY_TYPE.STREAM_WITH_BASE64,
            senderUid: senderUid,
            data: data,
            streamId: streamId,
            receiverUid: receiverUid
        );

    public static MetaBody FromBinary(
        int senderUid,
        byte[] data,
        string? streamId = null,
        int? receiverUid = null
        ) => new MetaBody(
            type: streamId is null ? IPC_META_BODY_TYPE.INLINE_BINARY : IPC_META_BODY_TYPE.STREAM_WITH_BINARY,
            senderUid: senderUid,
            data: data,
            streamId: streamId,
            receiverUid: receiverUid
        );

    public static MetaBody FromBinary(
        Ipc senderIpc,
        byte[] data,
        string? streamId = null,
        int? receiverUid = null
        ) => senderIpc.SupportBinary
            ? FromBinary(senderIpc.Uid, data, streamId, receiverUid)
            : FromBase64(senderIpc.Uid, Convert.ToBase64String(data), streamId, receiverUid);


    public MetaBody JsonAble() => Type_Encoding switch
    {
        IPC_DATA_ENCODING.BINARY => FromBase64(
            SenderUid,
            (Data as byte[])!.ToBase64(),
            StreamId,
            ReceiverUid
        ),
        _ => this
    };

    /// <summary>
    /// Serialize MetaBody
    /// </summary>
    /// <returns>JSON string representation of the MetaBody</returns>
    public string ToJson() => JsonSerializer.Serialize(JsonAble(), options: new() { IncludeFields = true });

    /// <summary>
    /// Deserialize MetaBody
    /// </summary>
    /// <param name="json">JSON string representation of MetaBody</param>
    /// <returns>An instance of a MetaBody object.</returns>
    public static MetaBody? FromJson(string json) => JsonSerializer.Deserialize<MetaBody>(json);
}
