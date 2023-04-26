using PeterO.Cbor;
using System.Diagnostics;
namespace DwebBrowser.MicroService;

public class ReadableStreamIpc : Ipc
{


    public ReadableStreamIpc(MicroModuleInfo remote, string role)
    {
        Remote = remote;
        Role = role;

        ReadableStream = new ReadableStream(
            Role,
            controller => _controller = controller,
            args =>
                Console.WriteLine(String.Format("ON-PULL/{0}", args.Item2.Stream), args.Item1),
            Console.WriteLine);
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

        Console.WriteLine(String.Format("post/{0}", ReadableStream), message.Length);
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
    public void BindIncomeStream(Stream stream)
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
                var size = await stream.ReadIntAsync();

                // 心跳包？
                if (size <= 0)
                {
                    continue;
                }

                Console.WriteLine(String.Format("size/{0} {1}", stream, size));
                var buffer = await stream.ReadBytesAsync(size);

                // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
                var message = MessageToIpcMessage.JsonToIpcMessage(buffer, this);
                switch (message)
                {
                    case "close":
                        await Close();
                        break;
                    case "ping":
                        await EnqueueAsync(_PONG_DATA);
                        break;
                    case "pong":
                        Console.WriteLine(String.Format("PONG/{0}", stream));
                        break;
                    case IpcMessage ipcMessage:
                        //Console.WriteLine("ON-MESSAGE/{0} {1}", this, ipcMessage);
                        //await (_onMessage?.Emit(ipcMessage, this)).ForAwait();
                        await _OnMessageEmit(ipcMessage, this);
                        break;
                    default:
                        throw new Exception(String.Format("unknown message: {0}", message));
                }
            }

            Console.WriteLine(String.Format("END/{0}", stream));
        });

        _incomeStream = stream;
    }
}

