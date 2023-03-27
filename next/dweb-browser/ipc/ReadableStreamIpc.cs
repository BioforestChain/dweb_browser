using MessagePack;
using System.Linq;

namespace ipc;

public class ReadableStreamIpc: Ipc
{
	

	public ReadableStreamIpc(MicroModuleInfo remote, String role)
	{
        Remote = remote;
        Role = role;
	}

    public override MicroModuleInfo Remote { get; set; }
    public override string Role { get; set; }

    public override Task DoClose() => Task.Run(() => _controller.Close());

    public override Task _doPostMessageAsync(IpcMessage data)
    {
        byte[] message = default;
        switch (SupportMessagePack)
        {
            case true:
                message = MessagePack.MessagePackSerializer.ConvertFromJson(data.ToJson());
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

        Console.WriteLine($"post/{RStream}", message.Length);
        return Enqueue(BitConverter.GetBytes(message.Length).Concat(message).ToArray());
    }

    public override string ToString() => base.ToString() + "@ReadableStreamIpc";

    /// <seealso cref="https://stackoverflow.com/questions/60812587/c-sharp-non-nullable-field-lateinit"/>
    private ReadbleStream.ReadableStreamController _controller = null!;

    public ReadbleStream RStream
    {
        get
        {
            return new ReadbleStream(
                Role,
                new Action<ReadbleStream.ReadableStreamController>(controller => _controller = controller),
                new Action<(int, ReadbleStream.ReadableStreamController)>(args =>
                    Console.WriteLine($"ON-PULL/{args.Item2.RStream}", args.Item1)),
                new Action(() => Console.WriteLine()));
        }
    }

    private Task Enqueue(byte[] data) => _controller.Enqueue(data);

    private Stream? _incomeStream { get; set; } = null;

    private byte[] _PONG_DATA
    {
        get
        {
            return new Lazy<byte[]>(new Func<byte[]>(() =>
            {
                var pong = "pong".FromUtf8();

                return BitConverter.GetBytes(pong.Length).Concat(pong).ToArray();
            }), true).Value;
        }
    }

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
            //while()
            //while(await stream.Re)
        });
    }
}

