
using ipc.extensions;

namespace ipc.ipcWeb;

public static class MessageToIpcMessage
{
    public static object? JsonToIpcMessage(string data, Ipc ipc)
    {
        if (data is "close" or "ping" or "pong")
        {
            return data;
        }

        try
        {
            IpcMessage result = default;
            switch (JsonSerializer.Deserialize<IpcMessageType>(data)!.Type)
            {
                case IPC_MESSAGE_TYPE.REQUEST:
                    result = IpcReqMessage.FromJson(data).Let(it =>
                    {
                        return new IpcRequest(
                            it!.ReqId, it.Url, it.Method, IpcHeaders.With(it.Headers), new IpcBodyReceiver(it.MetaBody, ipc), ipc);
                    });
                    break;
                case IPC_MESSAGE_TYPE.RESPONSE:
                    result = IpcResMessage.FromJson(data).Let(it =>
                    {
                        return new IpcResponse(
                            it!.ReqId, it.StatusCode, IpcHeaders.With(it.Headers), new IpcBodyReceiver(it.MetaBody, ipc), ipc);
                    });
                    break;
                case IPC_MESSAGE_TYPE.EVENT:
                    result = IpcEvent.FromJson(data)!;
                    break;
                case IPC_MESSAGE_TYPE.STREAM_DATA:
                    result = IpcStreamData.FromJson(data)!;
                    break;
                case IPC_MESSAGE_TYPE.STREAM_PULL:
                    result = IpcStreamPull.FromJson(data)!;
                    break;
                case IPC_MESSAGE_TYPE.STREAM_PAUSED:
                    result = IpcStreamPaused.FromJson(data)!;
                    break;
                case IPC_MESSAGE_TYPE.STREAM_END:
                    result = IpcStreamEnd.FromJson(data)!;
                    break;
                case IPC_MESSAGE_TYPE.STREAM_ABORT:
                    result = IpcStreamAbort.FromJson(data)!;
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

