﻿using System.Runtime.CompilerServices;
using System.Threading.Tasks.Dataflow;
using DwebBrowser.DWebView;
using DwebBrowser.Helper;
using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Message;
using System.Text.Json;
using Foundation;

#nullable enable

namespace DwebBrowser.IpcWeb;

public class MessagePort
{
    private static ConditionalWeakTable<WebMessagePort, MessagePort> s_wm = new();
    public static MessagePort From(WebMessagePort port) => s_wm.GetValue(port, (port) => new MessagePort(port));

    private WebMessagePort _port = null!;

    private MessagePort(WebMessagePort port)
    {
        _port = port;

        Task.Run(async () =>
        {
            await foreach (var message in MessageChannel.ReceiveAllAsync())
            {
                await (OnWebMessage?.Emit(message)).ForAwait();
            }
        }).Background();

        _port.OnMessage += (message, _) => MessageChannel.SendAsync(message);
    }

    public BufferBlock<WebMessage> MessageChannel = new(new DataflowBlockOptions
    { BoundedCapacity = DataflowBlockOptions.Unbounded });

    public event Signal<WebMessage>? OnWebMessage;

    public Task PostMessage(string data) => _port.PostMessage(WebMessage.From(data, new WebMessagePort[] { _port }));

    private bool _isClosed = false;

    public void Close()
    {
        if (_isClosed)
        {
            MessageChannel.Complete();
            return;
        }

        _isClosed = true;

    }
}

public class MessagePortIpc : Ipc
{
    public MessagePort Port { get; init; }
    private IPC_ROLE _role_type { get; init; }
    public override MicroModuleInfo Remote { get; set; }

    public override string Role { get => System.Enum.GetName(_role_type)!; }

    public MessagePortIpc(MessagePort port, Ipc.MicroModuleInfo remote, IPC_ROLE role_type)
    {
        Port = port;
        Remote = remote;
        _role_type = role_type;

        var ipc = this;

        Signal<WebMessage> callback = async (message, _) =>
        {
            switch (message.Data)
            {
                case NSString data:
                    switch (MessageToIpcMessage.JsonToIpcMessage((string)data, ipc))
                    {
                        case "close":
                            await Close();
                            break;
                        case "ping":
                            await Port.PostMessage("pong");
                            break;
                        case "pong":
                            Console.WriteLine(String.Format("PONG/{0}", ipc));
                            break;
                        case IpcMessage ipcMessage:
                            Console.WriteLine(String.Format("ON-MESSAGE/{0} {1}", ipc, message));
                            await _OnMessageEmit(ipcMessage, ipc);
                            break;
                    }
                    break;
                case NSNumber data:
                    Console.WriteLine(String.Format("OnWebMessage is number: {0}", data));
                    break;
                default:
                    throw new Exception(String.Format("unknown message: {0}", message));
            }
        };

        Port.OnWebMessage += callback;
        OnDestory += async (_) => { Port.OnWebMessage -= callback; };
    }

    public MessagePortIpc(WebMessagePort port, Ipc.MicroModuleInfo remote, IPC_ROLE rote_type)
        : this(MessagePort.From(port), remote, rote_type) { }
    

    public override Task _doPostMessageAsync(IpcMessage data)
    {
        string message;
        switch (data)
        {
            case IpcRequest ipcRequest:
                message = ipcRequest.LazyIpcReqMessage.ToJson();
                break;
            case IpcResponse ipcResponse:
                message = ipcResponse.LazyIpcResMessage.ToJson();
                break;
            default:
                message = data.ToJson();
                break;
        }

        return Port.PostMessage(message);
    }

    public override async Task DoClose()
    {
        await Port.PostMessage("close");
        Port.Close();
    }
}

