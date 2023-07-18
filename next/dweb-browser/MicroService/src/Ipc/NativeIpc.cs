namespace DwebBrowser.MicroService;

public class NativeIpc : Ipc
{
    public NativePort<IpcMessage, IpcMessage> Port;
    public override IMicroModuleInfo Remote { get; set; }
    private IPC_ROLE RoleType { get; set; }

    public NativeIpc(NativePort<IpcMessage, IpcMessage> port, IMicroModuleInfo remote, IPC_ROLE role)
    {
        Port = port;
        Remote = remote;
        RoleType = role;

        SupportRaw = true;
        SupportBinary = true;

        Port.OnMessage += async (message, _) =>
        {
            await _OnMessageEmit(message, this);
        };

        //_ = Task.Run(Port.StartAsync).NoThrow();
        _ = Task.Run(async () =>
        {
            Port.OnClose += (_) => Close();
            await Port.StartAsync();
        }).NoThrow();
    }

    public override string Role
    {
        get
        {
            return RoleType.ToString();
        }
    }

    public override string ToString() => base.ToString() + "@NativeIpc";

    public override Task _doPostMessageAsync(IpcMessage data) => Port.PostMessageAsync(data);

    public override Task DoClose() => Task.Run(() => Port.Close()).NoThrow();
}
