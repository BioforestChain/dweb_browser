using DwebBrowser.MicroService.Core;
namespace DwebBrowser.MicroService.Sys.NativeUI;

public class NativeUiNMM: NativeMicroModule
{
	public NativeUiNMM(): base("nativeui.sys.dweb")
	{
	}

    protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        throw new NotImplementedException();
    }
}

