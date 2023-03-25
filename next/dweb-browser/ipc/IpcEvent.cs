namespace ipc;

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
    }

    internal IpcEvent()
    { }

    public static IpcEvent FromBinary(string name, byte[] data) => new IpcEvent(name, data, IPC_DATA_ENCODING.BINARY);
    public static IpcEvent FromBase64(string name, byte[] data) =>
        new IpcEvent(name, Convert.ToBase64String(data), IPC_DATA_ENCODING.BASE64);
    public static IpcEvent FromUtf8(string name, byte[] data) => FromUtf8(name, Convert.ToString(data) ?? "");
    public static IpcEvent FromUtf8(string name, string data) => new IpcEvent(name, data, IPC_DATA_ENCODING.UTF8);

    public Lazy<byte[]> Binary
    {
        get
        {
            return new Lazy<byte[]>(new Func<byte[]>(() => EncodingConverter.DataToBinary(Data, Encoding)));
        }
    }
    public Lazy<string> Text
    {
        get
        {
            return new Lazy<string>(new Func<string>(() => EncodingConverter.DataToText(Data, Encoding)));
        }
    }

    /// <summary>
    /// Serialize IpcEvent
    /// </summary>
    /// <returns>JSON string representation of the IpcEvent</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcEvent
    /// </summary>
    /// <param name="json">JSON string representation of IpcEvent</param>
    /// <returns>An instance of a IpcEvent object.</returns>
    public static IpcEvent? FromJson(string json) => JsonSerializer.Deserialize<IpcEvent>(json);
}

sealed class IpcEventConverter : JsonConverter<IpcEvent>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;

    public override IpcEvent? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new Exception("Expected StartObject token");

        var ipcEvent = new IpcEvent();

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return ipcEvent;

            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new Exception("Expected PropertyName token");

            var propName = reader.GetString();

            reader.Read();

            switch (propName)
            {
                case "type":
                    ipcEvent.Type = (IPC_MESSAGE_TYPE)reader.GetInt16();
                    break;
                case "encoding":
                    ipcEvent.Encoding = (IPC_DATA_ENCODING)reader.GetInt16();
                    break;
                case "name":
                    ipcEvent.Name = reader.GetString() ?? "";
                    break;
                case "data":
                    ipcEvent.Data = reader.GetString() ?? "";
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
