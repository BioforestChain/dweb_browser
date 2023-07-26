using DwebBrowser.DWebView;
using DwebBrowser.Helper;
using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Message;
using DwebBrowser.MicroService.Core;

#nullable enable

namespace DwebBrowser.IpcWeb;

public class MessagePortIpc : Ipc
{
    static readonly Debugger Console = new("MessagePortIpc");

    public MessagePort Port { get; init; }
    private IPC_ROLE RoleType { get; init; }
    public override IMicroModule Remote { get; set; }

    public override string Role { get => Enum.GetName(RoleType)!; }

    public MessagePortIpc(MessagePort port, IMicroModule remote, IPC_ROLE role_type)
    {
        Port = port;
        Remote = remote;
        RoleType = role_type;

        var ipc = this;

        Signal<WebMessage> callback = async (message, _) =>
        {
            switch (message.Data)
            {
                case NSArray data:
                    {
                        byte[] bytes = new byte[data.Count];

                        for (uint i = 0; i < data.Count; i++)
                        {
                            bytes[i] = (byte)data.GetItem<NSNumber>(i);
                        }
                        
                        var imsg = MessageToIpcMessage.JsonToIpcMessage(CborHelper.Decode(bytes), ipc);
                        switch (imsg)
                        {
                            case "close":
                                await Close();
                                break;
                            case "ping":
                                await Port.PostMessage("pong");
                                break;
                            case "pong":
                                Console.Log("OnWebMessage", "PONG/{0}", ipc);
                                break;
                            case IpcMessage ipcMessage:
                                Console.Log("OnWebMessage", "ON-MESSAGE/{0} {1}", ipc, ipcMessage.ToJson());
                                await _OnMessageEmit(ipcMessage, ipc);
                                break;
                            default:
                                Console.Log("OnWebMessage", "Default {0}", imsg);
                                break;
                        }
                    }
                    break;
                case NSString data:
                    {
                        var jsonData = (string)data;
                        var imsg = MessageToIpcMessage.JsonToIpcMessage(jsonData, ipc);
                        switch (imsg)
                        {
                            case "close":
                                await Close();
                                break;
                            case "ping":
                                await Port.PostMessage("pong");
                                break;
                            case "pong":
                                Console.Log("OnWebMessage", "PONG/{0}", ipc);
                                break;
                            case IpcMessage ipcMessage:
                                Console.Log("OnWebMessage", "ON-MESSAGE/{0} {1}", ipc, jsonData);
                                await _OnMessageEmit(ipcMessage, ipc);
                                break;
                            default:
                                Console.Log("OnWebMessage", "Default {0}", imsg);
                                break;
                        }
                    }
                    break;
                case NSNumber data:
                    Console.Log("OnWebMessage", "OnWebMessage is number: {0}", data);
                    break;
                default:
                    throw new Exception(string.Format("unknown message: {0}", message.Data));
            }
        };

        Port.OnWebMessage += callback;
        OnDestory += async (_) => { Port.OnWebMessage -= callback; };
        _ = Port.Start();
    }

    public MessagePortIpc(WebMessagePort port, IMicroModule remote, IPC_ROLE rote_type)
        : this(MessagePort.From(port), remote, rote_type) { }


    public override Task _doPostMessageAsync(IpcMessage data)
    {
        string message = data switch
        {
            IpcRequest ipcRequest => ipcRequest.LazyIpcReqMessage.ToJson(),
            IpcResponse ipcResponse => ipcResponse.LazyIpcResMessage.ToJson(),
            _ => data.ToJson(),
        };
        return Port.PostMessage(message);
    }

    public override async Task DoClose()
    {
        await Port.PostMessage("close");
        await Port.Close();
    }
}

