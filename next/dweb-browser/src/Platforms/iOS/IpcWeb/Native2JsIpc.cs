using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Message;

namespace DwebBrowser.IpcWeb;

public class Native2JsIpc: MessagePortIpc
{
    public int PortId { get; init; }
	public Native2JsIpc(
        int port_id,
        Ipc.MicroModuleInfo remote,
        IPC_ROLE role = IPC_ROLE.CLIENT
        ): base(
            IpcWebMessageCache.ALL_MESSAGE_PORT_CACHE.GetValueOrDefault(port_id)
            ?? throw new Exception($"no found port2(js-process) by id: {port_id}"),
            remote,
            role)
	{
        PortId = port_id;

        OnClose += async (_) =>
        {
            IpcWebMessageCache.ALL_MESSAGE_PORT_CACHE.Remove(PortId);
        };

    }
}

