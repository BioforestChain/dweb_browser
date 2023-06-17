using PeterO.Cbor;
namespace DwebBrowser.MicroService;

public class ReadableStreamIpc : Ipc
{
    static readonly Debugger Console = new("ReadableStreamIpc");

    public ReadableStreamIpc(IMicroModuleInfo remote, string role)
    {
        Remote = remote;
        Role = role;

        ReadableStream = new ReadableStream(
            Role,
            onStart: controller => _controller = controller,
            onPull: args => Console.Log("OnPull", string.Format("ON-PULL/{0}", args.Item2.Stream), args.Item1),
            onClose: () => Console.Log("OnClose", "")
           );
    }

    public override IMicroModuleInfo Remote { get; set; }
    public override string Role { get; }

    public override async Task DoClose()
    {
        _controller.Close();
        //_incomeStream?.Close();
    }

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

        Console.Log("PostMessage", "post/{0} {1:H}", ReadableStream.Stream, data);
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

    /// <summary>
    /// 输入流要额外绑定
    /// </summary>
    /// <param name="stream"></param>
    /// <exception cref="Exception"></exception>
    /// 注意，这里不返回Task，所以这里是async void，属于 Task.Background。
    public async void BindIncomeStream(Stream stream)
    {
        if (_incomeStream is not null)
        {
            throw new Exception("income stream already binded.");
        }
        _incomeStream = stream;
        if(stream is ReadableStream.PipeStream rstream)
        {
            ReadableStream.Stream.output_sid = "<-" + rstream.id;
        }

        if (SupportCbor)
        {
            //var cbor = CBORObject.ReadJSON(stream);
            //cbor.EncodeToBytes();

            throw new Exception("还未实现 cbor 的编解码能力");
        }

        var streamName = stream.ToString();

        Console.Log("RR", "START/{0}", stream);
        //var reader = new BinaryReader(stream);
        while (stream.CanRead)
        {
            Console.Log("BindIncomeStream", "waitting/{0}", stream);
            var size = await stream.ReadIntAsync();

            // 心跳包？
            if (size <= 0)
            {
                continue;
            }

            var buffer = await stream.ReadBytesAsync(size);

            // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
            var message = MessageToIpcMessage.JsonToIpcMessage(buffer, this);
            Console.Log("BindIncomeStream", "message/{0} {1}({2}bytes)", stream, message, size);
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
                    throw new Exception(string.Format("unknown message: {0}", message));
            }
        }

        Console.Log("BindIncomeStream", "end/{0}", stream);
        Console.Log("RR", "END/{0}", stream);

    }
}

