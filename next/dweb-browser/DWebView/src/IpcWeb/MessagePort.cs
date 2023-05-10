using System.Runtime.CompilerServices;
using System.Threading.Tasks.Dataflow;
using DwebBrowser.DWebView;
using DwebBrowser.Helper;

#nullable enable

namespace DwebBrowser.IpcWeb;

public class MessagePort
{
    static Debugger Console = new Debugger("MessagePort");

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

    public Task Start() => _port.Start();
    public Task PostMessage(string data) => _port.PostMessage(WebMessage.From(data));

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

