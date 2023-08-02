namespace DwebBrowser.MicroService.Sys.Biometrics;

public class BiometricsNMM : NativeMicroModule
{
    static readonly Debugger Console = new("BiometricsNMM");

    public BiometricsNMM() : base("biometrics.sys.dweb", "biometrics")
    {
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        /** 检查是否支持生物识别*/
        HttpRouter.AddRoute(IpcMethod.Get, "/check", async (request, ipc) =>
        {
            var type = request.QueryString("type") ?? "";
            Console.Log("/check", "type: {0}, mmid: {1}", type, ipc.Remote.Mmid);

            return BiometricsManager.Check();
        });

        /** 生物识别 */
        HttpRouter.AddRoute(IpcMethod.Get, "/biometrics", async (request, ipc) =>
        {
            Console.Log("/biometrics", "mmid: {0}", ipc.Remote.Mmid);

            return await BiometricsManager.BiometricsAsync();
        });
    }
}

