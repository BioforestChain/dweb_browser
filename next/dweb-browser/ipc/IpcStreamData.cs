namespace ipc;

[JsonConverter(typeof(IpcStreamDataConverter))]
public class IpcStreamData : IpcMessage, IpcStream
{
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.STREAM_DATA;

    public string StreamId { get; set; }

    public object Data { get; set; }

    public IPC_DATA_ENCODING Encoding { get; set; }

    public IpcStreamData(string stream_id, object data, IPC_DATA_ENCODING encoding)
    {
        StreamId = stream_id;
        Data = data;
        Encoding = encoding;

        Binary = new Lazy<byte[]>(new Func<byte[]>(() => EncodingConverter.DataToBinary(Data, Encoding)));
        Text = new Lazy<string>(new Func<string>(() => EncodingConverter.DataToText(Data, Encoding)));
    }

    internal IpcStreamData()
    { }

    public static IpcStreamData FromBinary(string stream_id, byte[] data) => new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY);
    public static IpcStreamData FromBase64(string stream_id, byte[] data) => new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BASE64);
    public static IpcStreamData FromUtf8(string stream_id, byte[] data) => FromUtf8(stream_id, System.Text.UTF8Encoding.UTF8.GetString(data));
    public static IpcStreamData FromUtf8(string stream_id, string data) => new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.UTF8);

    public Lazy<byte[]> Binary;
    public Lazy<string> Text;

    /// <summary>
    /// Serialize IpcStreamData
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamData</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcStreamData
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamData</param>
    /// <returns>An instance of a IpcStreamData object.</returns>
    public static IpcStreamData? FromJson(string json) => JsonSerializer.Deserialize<IpcStreamData>(json);
}

sealed class IpcStreamDataConverter : JsonConverter<IpcStreamData>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;


    public override IpcStreamData? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new Exception("Expected StartObject token");

        var ipcStreamData = new IpcStreamData();

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return ipcStreamData;

            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new Exception("Expected PropertyName token");

            var propName = reader.GetString();

            reader.Read();

            switch (propName)
            {
                case "type":
                    ipcStreamData.Type = (IPC_MESSAGE_TYPE)reader.GetInt16();
                    break;
                case "encoding":
                    ipcStreamData.Encoding = (IPC_DATA_ENCODING)reader.GetInt16();
                    break;
                case "stream_id":
                    ipcStreamData.StreamId = reader.GetString() ?? "";
                    break;
                case "data":
                    ipcStreamData.Data = reader.GetString() ?? "";
                    break;
            }
        }

        throw new JsonException("Expected EndObject token");
    }

    public override void Write(Utf8JsonWriter writer, IpcStreamData value, JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        writer.WriteNumber("type", (int)value.Type);
        writer.WriteNumber("encoding", (int)value.Encoding);
        writer.WriteString("stream_id", value.StreamId);
        writer.WriteString("data", (string)value.Data);

        writer.WriteEndObject();
    }
}

