using Microsoft.Maui.Graphics;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.VirtualKeyboard;
using DwebBrowser.MicroService.Http;

#nullable enable

namespace DwebBrowser.Helper;

public class QueryHelper
{
    static QueryHelper()
    {
        ResponseRegistry.RegistryJsonAble<StatusBarController>(
            typeof(StatusBarController), it => it.GetState());
        ResponseRegistry.RegistryJsonAble<VirtualKeyboardController>(
            typeof(VirtualKeyboardController), it => it.GetState());
    }

}
public static class PureRequestQueryExtensions
{

    public static ColorJson? QueryColor(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key)?.Let(it =>
    {
        var color = Color.FromRgba(it);
        return new ColorJson(color.Red, color.Green, color.Blue, color.Alpha);
    });

    public static BarStyle? QueryStyle(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key)?.Let(it =>
    {
        return new BarStyle(it.ToUpper());
    });

    public static bool? QueryBool(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key)?.ToBooleanStrictOrNull();
    public static string? QueryString(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key);
    public static string QueryStringRequired(this PureRequest req, string key) => QueryString(req, key) ?? throw new KeyNotFoundException("require query." + key);
}
