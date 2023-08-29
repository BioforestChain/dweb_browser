using System.Runtime.CompilerServices;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DesktopWindowsManager : WindowsManager
{
    static readonly Debugger Console = new("DesktopWindowsManager");

    private static readonly ConditionalWeakTable<DeskController, DesktopWindowsManager> Instances = new();

    public static DesktopWindowsManager GetInstance(DeskController deskController, Action<DesktopWindowsManager> onPut)
        => Instances.GetValueOrPut(deskController, () =>
        {
            return new DesktopWindowsManager(deskController).Also(dwm =>
            {
                onPut(dwm);
                deskController.OnDestroy.OnListener += async (_) =>
                {
                    Instances.Remove(deskController);
                };
            });
        });

    public DesktopWindowsManager(DeskController deskController) : base(deskController)
    {
        var offAdapter = WindowAdapterManager.Instance.Append(async winState =>
        {
            var mutable = winState.Bounds.ToMutable();
            {
                var bounds = UIScreen.MainScreen.Bounds;
                var displayWidth = Convert.ToSingle(bounds.Width);
                var displayHeight = Convert.ToSingle(bounds.Height);
                if (float.IsNaN(mutable.Width))
                {
                    mutable.Width = (float)(displayWidth / Math.Sqrt(2));
                }
                if (float.IsNaN(mutable.Height))
                {
                    mutable.Height = (float)(displayHeight / Math.Sqrt(3));
                }
                if (float.IsNaN(mutable.Left))
                {
                    var maxLeft = displayWidth - mutable.Width;
                    var gapSize = 47f;
                    var gapCount = Convert.ToInt32(maxLeft / gapSize);

                    mutable.Left = gapSize + AllWindows.Count % gapCount * gapSize;
                }
                if (float.IsNaN(mutable.Top))
                {
                    var maxTop = displayHeight - mutable.Height;
                    var gapSize = 71f;
                    var gapCount = Convert.ToInt32(maxTop / gapSize);

                    mutable.Top = gapSize + AllWindows.Count % gapCount * gapSize;
                }
            }

            winState.UpdateMutableBounds(mutable);

            var win = new DesktopWindowController(this, winState);
            AddNewWindow(win);

            return win;
        });

        deskController.OnDestroy.OnListener += async (_) =>
        {
            offAdapter();
        };
    }
}

