using System.Text.Json;
using Microsoft.Maui.Graphics;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.SafeArea;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.NavigationBar;
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
        ResponseRegistry.RegistryJsonAble<NavigationBarController>(
            typeof(NavigationBarController), it => it.GetState());
        ResponseRegistry.RegistryJsonAble<VirtualKeyboardController>(
            typeof(VirtualKeyboardController), it => it.GetState());
        ResponseRegistry.RegistryJsonAble<SafeAreaController>(
            typeof(SafeAreaController), it => it.GetState());
    }

}
public static class PureRequestQueryExtensions
{

    public static ColorJson? QueryColor(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key)?.Let(it =>
    {
        return JsonSerializer.Deserialize<ColorJson>(it)!;
    });

    public static BarStyle? QueryStyle(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key)?.Let(it =>
    {
        return new BarStyle(it.ToUpper());
    });

    public static bool? QueryBool(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key)?.ToBooleanStrictOrNull();
    public static string? QueryString(this PureRequest req, string key) => req.ParsedUrl?.SearchParams.Get(key);
    public static string QueryStringRequired(this PureRequest req, string key) => QueryString(req, key) ?? throw new KeyNotFoundException("require query." + key);
}
