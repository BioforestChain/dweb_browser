namespace DwebBrowser.MicroService.Message;

public class IpcEvent : IpcMessage
{
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.EVENT;

    public string Name { get; set; }
    public object Data { get; set; }
    public IPC_DATA_ENCODING Encoding { get; set; }

    public IpcEvent(string name, object data, IPC_DATA_ENCODING encoding)
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
    public static IpcEvent FromUtf8(string name, byte[] data) => FromUtf8(name, Convert.ToString(data) ?? "");
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

#region IpcEvent序列化反序列化
sealed class IpcEventConverter : JsonConverter<IpcEvent>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;

    public override IpcEvent? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new Exception("Expected StartObject token");

        IPC_MESSAGE_TYPE type = default;
        IPC_DATA_ENCODING encoding = default;
        string name = default;
        string data = default;

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return new IpcEvent(name ?? "", data ?? "", encoding);

            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new Exception("Expected PropertyName token");

            var propName = reader.GetString();

            reader.Read();

            switch (propName)
            {
                case "type":
                    type = (IPC_MESSAGE_TYPE)reader.GetInt16();
                    break;
                case "encoding":
                    encoding = (IPC_DATA_ENCODING)reader.GetInt16();
                    break;
                case "name":
                    name = reader.GetString() ?? "";
                    break;
                case "data":
                    data = reader.GetString() ?? "";
                    break;
            }
        }

        throw new JsonException("Expected EndObject token");
    }

    public override void Write(Utf8JsonWriter writer, IpcEvent value, JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        writer.WriteNumber("type", (int)value.Type);
        writer.WriteNumber("encoding", (int)value.Encoding);
        writer.WriteString("name", value.Name);
        writer.WriteString("data", (string)value.Data);

        writer.WriteEndObject();
    }
}
#endregion