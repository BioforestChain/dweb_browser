
namespace DwebBrowser.MicroService.Message;

public class IpcStreamEnd : IpcMessage, IpcStream
{
    [JsonPropertyName("stream_id")]
    public string StreamId { get; set; }

    [Obsolete("使用带参数的构造函数", true)]
    public IpcStreamEnd() : base(IPC_MESSAGE_TYPE.STREAM_END)
    {
        /// 给JSON反序列化用的空参数构造函数
    }
    public IpcStreamEnd(string stream_id) : base(IPC_MESSAGE_TYPE.STREAM_END)
    {
        StreamId = stream_id;
    }

    /// <summary>
    /// Serialize IpcStreamEnd
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamEnd</returns>
    public override string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcStreamEnd
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamEnd</param>
    /// <returns>An instance of a IpcStreamEnd object.</returns>
    public static IpcStreamEnd? FromJson(string json) => JsonSerializer.Deserialize<IpcStreamEnd>(json);
}
