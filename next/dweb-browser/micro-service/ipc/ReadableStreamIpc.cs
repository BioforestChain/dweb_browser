using MessagePack;

namespace micro_service.ipc;

public class ReadableStreamIpc : Ipc
{


    public ReadableStreamIpc(MicroModuleInfo remote, string role)
    {
        Remote = remote;
        Role = role;
    }

    public override MicroModuleInfo Remote { get; set; }
    public override string Role { get; }

    public override Task DoClose() => Task.Run(_controller.Close);

    public override Task _doPostMessageAsync(IpcMessage data)
    {
        byte[] message = default;
        switch (SupportMessagePack)
        {
            case true:
                message = MessagePackSerializer.ConvertFromJson(data.ToJson());
                break;
            case false:
                switch (data)
                {
                    case IpcRequest ipcRequest:
                        message = ipcRequest.LazyIpcReqMessage.ToJson().FromUtf8();
                        break;
                    case IpcResponse ipcResponse:
                        message = ipcResponse.LazyIpcResMessage.ToJson().FromUtf8();
                        break;
                    case IpcStreamData ipcStreamData:
                        message = ipcStreamData.ToJson().FromUtf8();
                        break;
                    default:
                        message = data.ToJson().FromUtf8();
                        break;
                }
                break;
        }

        Console.WriteLine($"post/{Stream}", message.Length);
        return EnqueueAsync(message.Length.ToByteArray().Combine(message));
    }

    public override string ToString() => base.ToString() + "@ReadableStreamIpc";

    /// <seealso cref="https://stackoverflow.com/questions/60812587/c-sharp-non-nullable-field-lateinit"/>
    private ReadableStream.ReadableStreamController _controller = null!;

    public ReadableStream Stream
    {
        get
        {
            return new ReadableStream(
                Role,
                controller => _controller = controller,
                args =>
                    Console.WriteLine($"ON-PULL/{args.Item2.Stream}", args.Item1),
                Console.WriteLine);
        }
    }

    private Task EnqueueAsync(byte[] data) => _controller.EnqueueAsync(data);

    private Stream? _incomeStream { get; set; } = null;

    private byte[] _PONG_DATA
    {
        get
        {
            var pong = "pong".FromUtf8();

            return pong.Length.ToByteArray().Combine(pong);
        }
    }

    protected Signal<IpcMessage, ReadableStreamIpc>? _onMessage;

    /**
     * <summary>
     * 输入流要额外绑定
     * </summary>
     */
    public void BindIncomeStream(Stream stream)
    {
        if (_incomeStream is not null)
        {
            throw new Exception("income stream already binded.");
        }

        if (SupportMessagePack)
        {
            throw new Exception("还未实现 MessagePack 的编解码能力");
        }

        Task.Run(async () =>
        {
            //var reader = new BinaryReader(stream);
            while (stream.CanRead)
            {
                var size = await stream.ReadIntAsync();

                // 心跳包？
                if (size <= 0)
                {
                    continue;
                }

                Console.WriteLine($"size/{stream}", size);

                // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
                var message = MessageToIpcMessage.JsonToIpcMessage(await stream.ReadBytesAsync(size), this);
                switch (message)
                {
                    case "close":
                        await Close();
                        break;
                    case "ping":
                        await EnqueueAsync(_PONG_DATA);
                        break;
                    case "pong":
                        Console.WriteLine($"PONG/{stream}");
                        break;
                    case IpcMessage ipcMessage:
                        Console.WriteLine($"ON-MESSAGE/{this}", ipcMessage);
                        await (_onMessage?.Emit(ipcMessage, this)).ForAwait();
                        break;
                    default:
                        throw new Exception($"unknown message: {message}");
                }
            }

            Console.WriteLine($"END/{stream}");
        });

        _incomeStream = stream;
    }
}

