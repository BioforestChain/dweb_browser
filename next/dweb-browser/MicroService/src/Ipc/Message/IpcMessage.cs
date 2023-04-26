namespace DwebBrowser.MicroService.Message;

public class IpcMessage: IToJsonAble
{
    [JsonPropertyName("type"), JsonInclude, JsonPropertyOrder(-1)]
    public IPC_MESSAGE_TYPE Type { get; set; }
    [Obsolete("使用带参数的构造函数", true)]
    public IpcMessage()
    {
        /// 给JSON反序列化用的空参数构造函数
    }
    public IpcMessage(IPC_MESSAGE_TYPE Type)
    {
        this.Type = Type;
    }

    /// <summary>
    /// Serialize IpcMessage
    /// </summary>
    /// <returns>JSON string representation of the IpcMessage</returns>
    public virtual string ToJson() => JsonSerializer.Serialize(this,
        new JsonSerializerOptions { IncludeFields = true, PropertyNameCaseInsensitive = true });

    /// <summary>
    /// Deserialize IpcMessage
    /// </summary>
    /// <param name="json">JSON string representation of IpcMessage</param>
    /// <returns>An instance of a IpcMessage object.</returns>
    public static IpcMessage? FromJson(string json) => JsonSerializer.Deserialize<IpcMessage>(json,
        new JsonSerializerOptions { IncludeFields = true, PropertyNameCaseInsensitive = true });
}

public interface IpcStream
{
    string StreamId { get; set; }
}