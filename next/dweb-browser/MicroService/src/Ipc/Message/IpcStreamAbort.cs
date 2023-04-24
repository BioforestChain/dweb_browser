
namespace DwebBrowser.MicroService.Message;

public class IpcStreamAbort : IpcMessage, IpcStream
{

    [JsonPropertyName("stream_id")]
    public string StreamId { get; set; }

    [Obsolete("使用带参数的构造函数", true)]
    public IpcStreamAbort() : base(IPC_MESSAGE_TYPE.STREAM_ABORT)
    {
        /// 给JSON反序列化用的空参数构造函数
    }
    public IpcStreamAbort(string stream_id) : base(IPC_MESSAGE_TYPE.STREAM_ABORT)
    {
        StreamId = stream_id;
    }

    /// <summary>
    /// Serialize IpcStreamAbort
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamAbort</returns>
    public override string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcStreamAbort
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamAbort</param>
    /// <returns>An instance of a IpcStreamAbort object.</returns>
    public static IpcStreamAbort? FromJson(string json) => JsonSerializer.Deserialize<IpcStreamAbort>(json);
}
