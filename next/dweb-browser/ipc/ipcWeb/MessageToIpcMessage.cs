
namespace ipc.ipcWeb;

public static class MessageToIpcMessage
{
    private readonly static byte[] closeString = "close".FromUtf8();
    private readonly static byte[] pingString = "ping".FromUtf8();
    private readonly static byte[] pongString = "pong".FromUtf8();

    public static object? JsonToIpcMessage(byte[] data, Ipc ipc)
    {
        if (data.SequenceEqual(closeString) || data.SequenceEqual(pingString) || data.SequenceEqual(pongString))
        {
            return data.ToUtf8();
        }

        try
        {
            IpcMessage result = default;
            switch (JsonSerializer.Deserialize<IpcMessageType>(data)!.Type)
            {
                case IPC_MESSAGE_TYPE.REQUEST:
                    result = JsonSerializer.Deserialize<IpcReqMessage>(data).Let(it =>
                    {
                        return new IpcRequest(
                            it!.ReqId, it.Url, it.Method, IpcHeaders.With(it.Headers), new IpcBodyReceiver(it.MetaBody, ipc), ipc);
                    });
                    break;
                case IPC_MESSAGE_TYPE.RESPONSE:
                    result = JsonSerializer.Deserialize<IpcResMessage>(data).Let(it =>
                    {
                        return new IpcResponse(
                            it!.ReqId, it.StatusCode, IpcHeaders.With(it.Headers), new IpcBodyReceiver(it.MetaBody, ipc), ipc);
                    });
                    break;
                case IPC_MESSAGE_TYPE.EVENT:
                    result = JsonSerializer.Deserialize<IpcEvent>(data)!;
                    break;
                case IPC_MESSAGE_TYPE.STREAM_DATA:
                    result = JsonSerializer.Deserialize<IpcStreamData>(data)!;
                    break;
                case IPC_MESSAGE_TYPE.STREAM_PULL:
                    result = JsonSerializer.Deserialize<IpcStreamPull>(data)!;
                    break;
                case IPC_MESSAGE_TYPE.STREAM_PAUSED:
                    result = JsonSerializer.Deserialize<IpcStreamPaused>(data)!;
                    break;
                case IPC_MESSAGE_TYPE.STREAM_END:
                    result = JsonSerializer.Deserialize<IpcStreamEnd>(data)!;
                    break;
                case IPC_MESSAGE_TYPE.STREAM_ABORT:
                    result = JsonSerializer.Deserialize<IpcStreamAbort>(data)!;
                    break;
            }

            return result;
        }
        catch
        {
            return data;
        }
    }
}

