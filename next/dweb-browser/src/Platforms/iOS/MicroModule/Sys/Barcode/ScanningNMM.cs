namespace DwebBrowser.MicroService.Sys.Barcode;

public class ScanningNMM : NativeMicroModule
{
    public new const string Name = "Barcode Scanning";
    public ScanningNMM() : base("barcode-scanning.sys.dweb")
    { }

    public override List<MicroModuleCategory> Categories { get; init; } = new()
    {
        MicroModuleCategory.Application,
        MicroModuleCategory.Utilities,
    };

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        // 处理二维码图像
        HttpRouter.AddRoute(IpcMethod.Post, "/process", async (request, ipc) =>
        {
            var bytes = await request.Body.ToByteArrayAsync();
            return await _process(bytes);
        });

        // 停止处理
        HttpRouter.AddRoute(IpcMethod.Get, "/stop", async (request, _) =>
        {
            _stop();
            return true;
        });
    }

    private async Task<string[]> _process(byte[] bytes)
    {
        var po = new PromiseOut<string[]>();
        ScanningManager.Start(bytes, po);
        return await po.WaitPromiseAsync();
    }

    private void _stop()
    {
        ScanningManager.Stop();
    }
}

