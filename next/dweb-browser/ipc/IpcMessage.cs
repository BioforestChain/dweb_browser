
namespace ipc;

public abstract class IpcMessage
{
    [JsonPropertyName("type")]
    public abstract IPC_MESSAGE_TYPE Type { get; set; }

    /// <summary>
    /// Serialize IpcMessage
    /// </summary>
    /// <returns>JSON string representation of the IpcMessage</returns>
    public virtual string ToJson() => JsonSerializer.Serialize(this);
}

[JsonConverter(typeof(IpcMessageTypeConverter))]
public record IpcMessageType(IPC_MESSAGE_TYPE Type);

#region IpcMessageType序列化反序列化 - 用于IpcMessage反序列化判断IpcMessage子类类型
public class IpcMessageTypeConverter : JsonConverter<IpcMessageType>
{
    public override bool CanConvert(Type typeToConvert) => true;


    public override IpcMessageType? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new Exception("Expected StartObject token");

        var typeValue = 0;

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return new IpcMessageType((IPC_MESSAGE_TYPE)typeValue);

            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new Exception("Expected PropertyName token");

            var propName = reader.GetString();

            reader.Read();

            switch (propName)
            {
                case "type":
                    typeValue = reader.GetInt16();
                    break;
                default:
                    continue;
            }
        }

        throw new JsonException("Expected type PropertyName token");
    }

    public override void Write(Utf8JsonWriter writer, IpcMessageType value, JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        writer.WriteNumber("type", (int)value.Type);

        writer.WriteEndObject();
    }
}
#endregion

public interface IpcStream
{
    string StreamId { get; set; }
}