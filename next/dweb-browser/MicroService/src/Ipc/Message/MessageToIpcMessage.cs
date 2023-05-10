
namespace DwebBrowser.MicroService.Message;

public static class MessageToIpcMessage
{
    private readonly static byte[] closeString = "close"u8.ToArray();
    private readonly static byte[] pingString = "ping"u8.ToArray();
    private readonly static byte[] pongString = "pong"u8.ToArray();

    public static object? JsonToIpcMessage(byte[] data, Ipc ipc)
    {
        if (data.SequenceEqual(closeString) || data.SequenceEqual(pingString) || data.SequenceEqual(pongString))
        {
            return data.ToUtf8();
        }

        try
        {
            return JsonSerializer.Deserialize<IpcMessage>(data)!.Type switch
            {
                IPC_MESSAGE_TYPE.REQUEST =>
                    JsonSerializer.Deserialize<IpcReqMessage>(data).Let(it =>
                    {
                        return new IpcRequest(
                            it!.ReqId, it.Url, it.Method, IpcHeaders.With(it.Headers), new IpcBodyReceiver(it.MetaBody, ipc), ipc);
                    }),
                IPC_MESSAGE_TYPE.RESPONSE =>
                    JsonSerializer.Deserialize<IpcResMessage>(data).Let(it =>
                    {
                        return new IpcResponse(
                            it!.ReqId, it.StatusCode, IpcHeaders.With(it.Headers), new IpcBodyReceiver(it.MetaBody, ipc), ipc);
                    }),
                IPC_MESSAGE_TYPE.EVENT => JsonSerializer.Deserialize<IpcEvent>(data)!,
                IPC_MESSAGE_TYPE.STREAM_DATA => JsonSerializer.Deserialize<IpcStreamData>(data)!,
                IPC_MESSAGE_TYPE.STREAM_PULL => JsonSerializer.Deserialize<IpcStreamPulling>(data)!,
                IPC_MESSAGE_TYPE.STREAM_PAUSED => JsonSerializer.Deserialize<IpcStreamPaused>(data)!,
                IPC_MESSAGE_TYPE.STREAM_END => JsonSerializer.Deserialize<IpcStreamEnd>(data)!,
                IPC_MESSAGE_TYPE.STREAM_ABORT => JsonSerializer.Deserialize<IpcStreamAbort>(data)!,
                _ => throw new Exception("IpcMessage Type no found")
            };
        }
        catch
        {
            return data;
        }
    }

    public static object? JsonToIpcMessage(string data, Ipc ipc)
    {
        if (data is "close" or "ping" or "pong")
        {
            return data;
        }

        try
        {
            return JsonSerializer.Deserialize<IpcMessage>(data)!.Type switch
            {
                IPC_MESSAGE_TYPE.REQUEST =>
                    IpcReqMessage.FromJson(data).Let(it =>
                    {
                        return new IpcRequest(
                            it!.ReqId, it.Url, it.Method, IpcHeaders.With(it.Headers), IpcBodyReceiver.From(it.MetaBody, ipc), ipc);
                    }),
                IPC_MESSAGE_TYPE.RESPONSE =>
                    IpcResMessage.FromJson(data).Let(it =>
                    {
                        return new IpcResponse(
                            it!.ReqId, it.StatusCode, IpcHeaders.With(it.Headers), IpcBodyReceiver.From(it.MetaBody, ipc), ipc);
                    }),
                IPC_MESSAGE_TYPE.EVENT => IpcEvent.FromJson(data)!,
                IPC_MESSAGE_TYPE.STREAM_DATA => IpcStreamData.FromJson(data)!,
                IPC_MESSAGE_TYPE.STREAM_PULL => IpcStreamPulling.FromJson(data)!,
                IPC_MESSAGE_TYPE.STREAM_PAUSED => IpcStreamPaused.FromJson(data)!,
                IPC_MESSAGE_TYPE.STREAM_END => IpcStreamEnd.FromJson(data)!,
                IPC_MESSAGE_TYPE.STREAM_ABORT => IpcStreamAbort.FromJson(data)!,
                _ => throw new Exception("IpcMessage Type no found")
            };
        }
        catch
        {
            return data;
        }
    }
}

