
namespace DwebBrowser.MicroService.Sys.Device;

public class DeviceNMM : NativeMicroModule
{
    static readonly Debugger Console = new("DeviceNMM");
    public override List<MicroModuleCategory> Categories { get; init; } = new()
    {
        MicroModuleCategory.Service,
        MicroModuleCategory.Device_Management_Service,
    };

    public override string ShortName { get; set; } = "Device";
    public DeviceNMM() : base("device.sys.dweb", "Device Info")
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

