
namespace DwebBrowser.MicroService.Sys.Device;

public class DeviceNMM : NativeMicroModule
{
    static readonly Debugger Console = new("DeviceNMM");
    public DeviceNMM() : base("device.sys.dweb")
    {
    }

    record UUIDRecord(string uuid);
    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/uuid", async (request, _) =>
        {
            Console.Log("/uuid", DeviceSystemInfo.GetUUID());
            return new UUIDRecord(DeviceSystemInfo.GetUUID());
        });
    }
}

