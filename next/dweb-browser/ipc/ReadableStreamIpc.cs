﻿using MessagePack;
using System.Linq;
using ipc.ipcWeb;

namespace ipc;

public class ReadableStreamIpc : Ipc
{


    public ReadableStreamIpc(MicroModuleInfo remote, String role)
    {
        Remote = remote;
        Role = role;
    }

    public override MicroModuleInfo Remote { get; set; }
    public override string Role { get; }

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

        Console.WriteLine($"post/{Stream}", message.Length);
        return Enqueue(message.Length.ToByteArray().Combine(message));
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
                (controller => _controller = controller),
                (args =>
                    Console.WriteLine($"ON-PULL/{args.Item2.Stream}", args.Item1)),
                (() => Console.WriteLine()));
        }
    }

    private Task Enqueue(byte[] data) => _controller.Enqueue(data);

    private Stream? _incomeStream { get; set; } = null;

    private byte[] _PONG_DATA
    {
        get
        {
            var pong = "pong".FromUtf8();

            return pong.Length.ToByteArray().Combine(pong);
        }
    }

    /**
     * <summary>
     * 输入流要额外绑定
     * </summary>
     */
    public void BindIncomeStream(ReadableStream stream)
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
            while (stream.Available() > 0)
            {
                byte[] buffer = new byte[4];
                var size = await stream.ReadAsync(buffer, 0, buffer.Length);

                // 心跳包？
                if (size <= 0)
                {
                    continue;
                }

                Console.WriteLine($"size/{stream}", size);

                // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
                var message = MessageToIpcMessage.JsonToIpcMessage(buffer.ToUtf8(), this);
                switch (message)
                {
                    case "close":
                        await Close();
                        break;
                    case "ping":
                        await Enqueue(_PONG_DATA);
                        break;
                    case "pong":
                        Console.WriteLine($"PONG/{stream}");
                        break;
                    case IpcMessage ipcMessage:
                        Console.WriteLine($"ON-MESSAGE/{this}", ipcMessage);
                        await _messageSigal.EmitAsync(new IpcMessageArgs(ipcMessage, this));
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

