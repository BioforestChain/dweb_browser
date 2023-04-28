using DwebBrowser.DWebView;
using DwebBrowser.Helper;

namespace DwebBrowser.IpcWeb;

public static class IpcWebMessageCache
{
    public static Dictionary<int, MessagePort> ALL_MESSAGE_PORT_CACHE = new();
    private static int s_all_ipc_id_acc = 0;

    public static int SaveNative2JsIpcPort(WebMessagePort port) =>
        Interlocked.Increment(ref s_all_ipc_id_acc).Also(port_id =>
        {
            ALL_MESSAGE_PORT_CACHE.Add(port_id, MessagePort.From(port));
        });
}

