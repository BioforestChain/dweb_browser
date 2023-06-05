using DwebBrowser.DWebView;
using DwebBrowser.Helper;
using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Message;
using System.Text.Json;
using Foundation;

#nullable enable

namespace DwebBrowser.IpcWeb;

public class MessagePortIpc : Ipc
{
    static readonly Debugger Console = new("MessagePortIpc");

    public MessagePort Port { get; init; }
    private IPC_ROLE _role_type { get; init; }
    public override IMicroModuleInfo Remote { get; set; }

    public override string Role { get => System.Enum.GetName(_role_type)!; }

    public MessagePortIpc(MessagePort port, IMicroModuleInfo remote, IPC_ROLE role_type)
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
                            Console.Log("OnWebMessage", "ON-MESSAGE/{0} {1}", ipc, message);
                            await _OnMessageEmit(ipcMessage, ipc);
                            break;
                        default:
                            Console.Log("OnWebMessage", "Default {0}", imsg);
                            break;
                    }
                    break;
                case NSNumber data:
                    Console.Log("OnWebMessage", "OnWebMessage is number: {0}", data);
                    break;
                default:
                    throw new Exception(string.Format("unknown message: {0}", message));
            }
        };

        Port.OnWebMessage += callback;
        OnDestory += async (_) => { Port.OnWebMessage -= callback; };
        _ = Port.Start();
    }

    public MessagePortIpc(WebMessagePort port, Ipc.IMicroModuleInfo remote, IPC_ROLE rote_type)
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

