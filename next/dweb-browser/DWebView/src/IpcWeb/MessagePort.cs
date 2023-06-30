using System.Runtime.CompilerServices;
using System.Threading.Tasks.Dataflow;
using DwebBrowser.DWebView;
using DwebBrowser.Helper;

#nullable enable

namespace DwebBrowser.IpcWeb;

public class MessagePort
{
    static readonly Debugger Console = new("MessagePort");

    private static ConditionalWeakTable<WebMessagePort, MessagePort> s_wm = new();
    public static MessagePort From(WebMessagePort port) => s_wm.GetValue(port, (port) => new MessagePort(port));

    private WebMessagePort _port { init; get; }

    private MessagePort(WebMessagePort port)
    {
        _port = port;

        _ = Task.Run(async () =>
        {
            await foreach (var message in MessageChannel.ReceiveAllAsync())
            {
                await (OnWebMessage?.Emit(message)).ForAwait();
            }
        }).NoThrow();

        _port.OnMessage += (message, _) => MessageChannel.SendAsync(message);
    }

    public BufferBlock<WebMessage> MessageChannel = new(new DataflowBlockOptions
    { BoundedCapacity = DataflowBlockOptions.Unbounded });

    public event Signal<WebMessage>? OnWebMessage;

    public Task Start() => _port.Start().NoThrow();
    public Task PostMessage(string data) => _port.PostMessage(WebMessage.From(data)).NoThrow();

    private bool _isClosed = false;

    public async Task Close()
    {
        if (_isClosed)
        {
            return;
        }

        _isClosed = true;
        OnWebMessage = null;
        MessageChannel.Complete();
        await _port.Close();

    }
}

