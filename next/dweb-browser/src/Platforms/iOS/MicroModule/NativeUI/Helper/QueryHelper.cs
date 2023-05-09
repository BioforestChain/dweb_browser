using Microsoft.Maui.Graphics;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.VirtualKeyboard;

#nullable enable

namespace DwebBrowser.Helper;

public class QueryHelper
{
    static QueryHelper()
    {
        NativeMicroModule.ResponseRegistry.RegistryJsonAble<StatusBarController>(
            typeof(StatusBarController), it => it.GetState());
        NativeMicroModule.ResponseRegistry.RegistryJsonAble<VirtualKeyboardController>(
            typeof(VirtualKeyboardController), it => it.GetState());
    }

    public static ColorJson? QueryColor(HttpRequestMessage req) =>
        req.QueryValidate<string>("color", false)?.Let(it =>
        {
            var color = Color.FromRgba(it);
            return new ColorJson(color.Red, color.Green, color.Blue, color.Alpha);
        });

    public static BarStyle? QueryStyle(HttpRequestMessage req) =>
        req.QueryValidate<string>("style", false)?.Let(it =>
        {
            return new BarStyle(it.ToUpper());
        });

    public static bool? QueryVisible(HttpRequestMessage req) =>
        req.QueryValidate<bool>("visible", false);

    public static bool? QueryOverlay(HttpRequestMessage req) =>
        req.QueryValidate<bool>("overlay", false);
}

