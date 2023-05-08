using PeterO.Cbor;
namespace DwebBrowser.MicroService;

public class ReadableStreamIpc : Ipc
{
    static Debugger Console = new Debugger("ReadableStreamIpc");

    public ReadableStreamIpc(MicroModuleInfo remote, string role)
    {
        Remote = remote;
        Role = role;

        ReadableStream = new ReadableStream(
            Role,
            onStart: controller => _controller = controller,
            onPull: args => Console.Log("OnPull", String.Format("ON-PULL/{0}", args.Item2.Stream), args.Item1),
            onClose: () => Console.Log("OnClose", "")
           );
    }

    public override MicroModuleInfo Remote { get; set; }
    public override string Role { get; }

    public override Task DoClose() => Task.Run(_controller.Close);

    public override Task _doPostMessageAsync(IpcMessage data)
    {
        var message = SupportCbor switch
        {
            true => CBORObject.FromJSONBytes(data.ToJson().ToUtf8ByteArray()).EncodeToBytes(),
            false => data switch
            {
                IpcRequest ipcRequest => ipcRequest.LazyIpcReqMessage.ToJson().ToUtf8ByteArray(),
                IpcResponse ipcResponse => ipcResponse.LazyIpcResMessage.ToJson().ToUtf8ByteArray(),
                IpcStreamData ipcStreamData => ipcStreamData.ToJson().ToUtf8ByteArray(),
                _ => data.ToJson().ToUtf8ByteArray(),
            },
        };

        Console.Log("PostMessage", "post/{0} {1}", ReadableStream, message.Length);
        return EnqueueAsync(message.Length.ToByteArray().Combine(message));
    }

    public override string ToString() => base.ToString() + "@ReadableStreamIpc";

    /// <seealso cref="https://stackoverflow.com/questions/60812587/c-sharp-non-nullable-field-lateinit"/>
    private ReadableStream.ReadableStreamController _controller = null!;

    public ReadableStream ReadableStream { get; init; }

    private Task EnqueueAsync(byte[] data) => _controller.EnqueueAsync(data);

    private Stream? _incomeStream { get; set; } = null;

    private byte[] _PONG_DATA
    {
        get
        {
            var pong = "pong"u8;

            return pong.Length.ToByteArray().Combine(pong);
        }
    }

    //protected event Signal<IpcMessage, ReadableStreamIpc>? _onMessage;

    /**
     * <summary>
     * 输入流要额外绑定
     * </summary>
     */
    public ReadableStreamIpc BindIncomeStream(Stream stream)
    {
        if (_incomeStream is not null)
        {
            throw new Exception("income stream already binded.");
        }

        if (SupportCbor)
        {
            //var cbor = CBORObject.ReadJSON(stream);
            //cbor.EncodeToBytes();

            throw new Exception("还未实现 cbor 的编解码能力");
        }

        Task.Run(async () =>
        {
            //var reader = new BinaryReader(stream);
            while (stream.CanRead)
            {
                Console.Log("BindIncomeStream", "waitting/{0:H}", stream);
                var size = await stream.ReadIntAsync();

                // 心跳包？
                if (size <= 0)
                {
                    continue;
                }

                var buffer = await stream.ReadBytesAsync(size);

                // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
                var message = MessageToIpcMessage.JsonToIpcMessage(buffer, this);
                Console.Log("BindIncomeStream", "message/{0:H} {1}({2}bytes)", stream, message, size);
                switch (message)
                {
                    case "close":
                        await Close();
                        break;
                    case "ping":
                        await EnqueueAsync(_PONG_DATA);
                        break;
                    case "pong":
                        Console.Log("BindIncomeStream", "PONG/{0}", stream);
                        break;
                    case IpcMessage ipcMessage:
                        await _OnMessageEmit(ipcMessage, this);
                        break;
                    default:
                        throw new Exception(String.Format("unknown message: {0}", message));
                }
            }

            Console.Log("BindIncomeStream", "END/{0:H}", stream);
        });

        _incomeStream = stream;
        return this;
    }
}

