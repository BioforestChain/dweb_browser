using System.Globalization;
using UIKit;

namespace DwebBrowser.Helper;

public static class StringExtensions
{
    public static int AsColorHex(this string self, int start = 0, int len = 2)
    {
        var hex = self[start..(start + len)];
        if (len == 1)
        {
            hex += hex;
        }

        return hex.ToInt(NumberStyles.HexNumber);
    }

    public static UIColor? Hex(this string hex) => hex[0] != '#' ? null : hex.Length switch
    {
        // #RGB
        4 => UIColor.FromRGB(hex.AsColorHex(1, 1), hex.AsColorHex(2, 1), hex.AsColorHex(3, 1)),
        // #RGBA
        5 => UIColor.FromRGBA(hex.AsColorHex(1, 1), hex.AsColorHex(2, 1), hex.AsColorHex(3, 1), hex.AsColorHex(4, 1)),
        // #RRGGBB
        7 => UIColor.FromRGB(hex.AsColorHex(1), hex.AsColorHex(3), hex.AsColorHex(5)),
        // #RRGGBBAA
        9 => UIColor.FromRGBA(hex.AsColorHex(1), hex.AsColorHex(3), hex.AsColorHex(5), hex.AsColorHex(7)),
        _ => null
    };

    public static UIColor AsWindowStateColor(this string self, Func<UIColor> autoColor) => self.Hex() ?? autoColor();
    public static UIColor AsWindowStateColor(this string self, UIColor autoColor) => self.AsWindowStateColor(() => autoColor);
    public static UIColor AsWindowStateColor(this string self, UIColor lightColor, UIColor darkColor, bool isDark)
        => self.AsWindowStateColor(() =>
        {
            if (isDark)
            {
                return darkColor;
            }
            else
            {
                return lightColor;
            }
        });

}

