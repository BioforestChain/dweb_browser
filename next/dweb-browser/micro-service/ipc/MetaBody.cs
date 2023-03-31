
namespace micro_service.ipc;

[JsonConverter(typeof(MetaBodyConverter))]
public struct SMetaBody
{
    /**
     * <summary>
     * 类型信息，包含了 编码信息 与 形态信息
     * 编码信息是对 data 的解释
     * 形态信息（流、内联）是对 "是否启用 streamId" 的描述（注意，流也可以内联第一帧的数据）
     * </summary>
     */
    public IPC_META_BODY_TYPE Type { get; set; }
    public int SenderUid { get; set; }
    public Object Data { get; set; }
    public string? StreamId { get; set; } = null;
    public int? ReceiverUid { get; set; } = null;

    /**
     * <summary>
     * 唯一id，指代这个数据的句柄
     *
     * 需要使用这个值对应的数据进行缓存操作
     * 远端可以发送句柄回来，这样可以省去一些数据的回传延迟。
     * </summary>
     */
    public string MetaId = Token.RandomCryptoString(8);


    public SMetaBody(
        IPC_META_BODY_TYPE type,
        int senderUid,
        Object data,
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

    public class IpcMetaBodyType
    {
        public IPC_META_BODY_TYPE Type;
        private Lazy<IPC_DATA_ENCODING> _encoding;
        public IPC_DATA_ENCODING Encoding
        {
            get { return _encoding.Value; }
        }

        private Lazy<bool> _isInline;
        public bool IsInline
        {
            get { return _isInline.Value; }
        }

        private Lazy<bool> _isStream;
        public bool IsStream
        {
            get { return _isStream.Value; }
        }

        public IpcMetaBodyType(IPC_META_BODY_TYPE type)
        {
            Type = type;

            _encoding = new Lazy<IPC_DATA_ENCODING>(() =>
            {
                var encoding = (int)Type & 0b11111110;
                return (IPC_DATA_ENCODING)encoding;
            }, true);

            _isInline = new Lazy<bool>(() => ((int)Type & 1) == 1, true);
            _isStream = new Lazy<bool>(() => ((int)Type & 1) == 0, true);
        }
    }

    public static SMetaBody FromText(
        int senderUid,
        string data,
        string? streamId = null,
        int? receiverUid = null
        ) => new SMetaBody(
            type: streamId == null ? IPC_META_BODY_TYPE.INLINE_TEXT : IPC_META_BODY_TYPE.STREAM_WITH_TEXT,
            senderUid: senderUid,
            data: data,
            streamId: streamId,
            receiverUid: receiverUid
        );

    public static SMetaBody FromBase64(
        int senderUid,
        string data,
        string? streamId = null,
        int? receiverUid = null
        ) => new SMetaBody(
            type: streamId == null ? IPC_META_BODY_TYPE.INLINE_BASE64 : IPC_META_BODY_TYPE.STREAM_WITH_BASE64,
            senderUid: senderUid,
            data: data,
            streamId: streamId,
            receiverUid: receiverUid
        );

    public static SMetaBody FromBinary(
        int senderUid,
        byte[] data,
        string? streamId = null,
        int? receiverUid = null
        ) => new SMetaBody(
            type: streamId == null ? IPC_META_BODY_TYPE.INLINE_BINARY : IPC_META_BODY_TYPE.STREAM_WITH_BINARY,
            senderUid: senderUid,
            data: data,
            streamId: streamId,
            receiverUid: receiverUid
        );

    public static SMetaBody FromBinary(
        Ipc senderIpc,
        byte[] data,
        string? streamId = null,
        int? receiverUid = null
        ) => senderIpc.SupportBinary
            ? FromBinary(senderIpc.Uid, data, streamId, receiverUid)
            : FromBase64(senderIpc.Uid, Convert.ToBase64String(data), streamId, receiverUid);


    /// <summary>
    /// Serialize MetaBody
    /// </summary>
    /// <returns>JSON string representation of the MetaBody</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize MetaBody
    /// </summary>
    /// <param name="json">JSON string representation of MetaBody</param>
    /// <returns>An instance of a MetaBody object.</returns>
    public static SMetaBody? FromJson(string json) => JsonSerializer.Deserialize<SMetaBody>(json);
}

#region MetaBody序列化反序列化
sealed class MetaBodyConverter : JsonConverter<SMetaBody>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;

    public override SMetaBody Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new JsonException("Expected StartObject token");

        SMetaBody.IPC_META_BODY_TYPE type = default;
        int senderUid = default;
        string data = default;
        string? stream_id = null;
        int? receiverUid = null;
        string metaId = default;

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return new SMetaBody(type, senderUid, data ?? "", stream_id, receiverUid) { MetaId = metaId ?? "" };

            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new JsonException("Expected PropertyName token");

            var propName = reader.GetString();

            reader.Read();

            switch (propName)
            {
                case "type":
                    type = (SMetaBody.IPC_META_BODY_TYPE)reader.GetInt64();
                    break;
                case "senderUid":
                    senderUid = reader.GetInt32();
                    break;
                case "data":
                    data = reader.GetString() ?? "";
                    break;
                case "streamId":
                    stream_id = reader.GetString() ?? null;
                    break;
                case "receiverUid":
                    receiverUid = reader.GetInt32();
                    break;
                case "metaId":
                    metaId = reader.GetString() ?? "";
                    break;
            }
        }

        throw new JsonException("Expected EndObject token");
    }

    public override void Write(
        Utf8JsonWriter writer,
        SMetaBody value,
        JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        writer.WriteNumber("type", (int)value.Type);
        writer.WriteNumber("senderUid", value.SenderUid);

        if (value.ReceiverUid != null)
        {
            writer.WriteString("streamId", value.StreamId);
        }
        else
        {
            writer.WriteNull("streamId");
        }

        if (value.ReceiverUid != null)
        {
            writer.WriteNumber("receiverUid", (decimal)value.ReceiverUid!);
        }
        else
        {
            writer.WriteNull("receiverUid");
        }

        writer.WriteString("metaId", value.MetaId);
        writer.WriteString("data", (string)value.Data);

        writer.WriteEndObject();
    }
}
#endregion