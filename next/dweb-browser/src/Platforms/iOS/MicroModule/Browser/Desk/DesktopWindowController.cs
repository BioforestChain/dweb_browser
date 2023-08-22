namespace DwebBrowser.MicroService.Browser.Desk;

public class DesktopWindowController : WindowController
{
    public override DesktopWindowsManager DesktopWindowsManager { get; init; }

    public DesktopWindowController(DesktopWindowsManager desktopWindowsManager, WindowState state) : base(state)
    {
        DesktopWindowsManager = desktopWindowsManager;
    }
}

