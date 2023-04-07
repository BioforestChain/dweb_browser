using System;

namespace DwebBrowser.MicroService.Sys.Jmm;

public class JsMicroModule: MicroModule
{
	public JsMicroModule()
	{
	}

    public override string Mmid { get => throw new NotImplementedException(); init => throw new NotImplementedException(); }

    protected override Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        throw new NotImplementedException();
    }

    protected override Task _onActivityAsync(IpcEvent Event, Ipc ipc)
    {
        throw new NotImplementedException();
    }

    protected override Task _shutdownAsync()
    {
        throw new NotImplementedException();
    }
}

