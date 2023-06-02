using DwebBrowser.Helper;
using Microsoft.Maui.Graphics;

namespace DwebBrowserIOSUnitTest.Tests;

public static class ColorTest
{
	static readonly Debugger Console = new("ColorTest");

	public static void FromArgb_ReturnSuccess()
	{
		/// argb 顺序 alpha red green blue，前两位 alpha
		var color1 = Color.FromArgb("#ff0000ff");
		Console.Log("FromArgb", "r: {0}, g: {1}, b: {2}, a: {3}",
			color1.Red, color1.Green, color1.Blue, color1.Alpha);

		/// 如果没有传入alpha，前两位 red，默认 alpha 为 1
		var color2 = Color.FromArgb("#ff0000");
        Console.Log("FromArgb", "r: {0}, g: {1}, b: {2}, a: {3}",
            color2.Red, color2.Green, color2.Blue, color2.Alpha);

		/// 可以识别一位缩写
		var color3 = Color.FromArgb("#f00f");
        Console.Log("FromArgb", "r: {0}, g: {1}, b: {2}, a: {3}",
            color3.Red, color3.Green, color3.Blue, color3.Alpha);

        var color4 = Color.FromArgb("0xFF0000FF");
        Console.Log("FromArgb", "r: {0}, g: {1}, b: {2}, a: {3}",
            color4.Red, color4.Green, color4.Blue, color4.Alpha);
    }

	public static void FromRgba_string_ReturnSuccess()
	{
        /// rgba 顺序 red green blue alpha，前两位 red
        var color1 = Color.FromRgba("#ff0000ff");
        Console.Log("FromArgb", "r: {0}, g: {1}, b: {2}, a: {3}",
            color1.Red, color1.Green, color1.Blue, color1.Alpha);

        /// 如果没有传入alpha，默认alpha 为 1
        var color2 = Color.FromRgba("#ff0000");
        Console.Log("FromArgb", "r: {0}, g: {1}, b: {2}, a: {3}",
            color2.Red, color2.Green, color2.Blue, color2.Alpha);

        /// 可以识别一位缩写
        var color3 = Color.FromRgba("#f00f");
        Console.Log("FromArgb", "r: {0}, g: {1}, b: {2}, a: {3}",
            color3.Red, color3.Green, color3.Blue, color3.Alpha);

        var color4 = Color.FromRgba("0xFF0000FF");
        Console.Log("FromArgb", "r: {0}, g: {1}, b: {2}, a: {3}",
            color4.Red, color4.Green, color4.Blue, color4.Alpha);
    }

    public static void FromRgba_int_ReturnSuccess()
    {
        var color1 = Color.FromRgba(255, 0, 0, 255);
        Console.Log("FromArgb", "r: {0}, g: {1}, b: {2}, a: {3}",
            color1.Red, color1.Green, color1.Blue, color1.Alpha);
    }
}

