﻿using UIKit;
namespace DwebBrowser.Helper;

public static class UIColorExtensions
{
    public static ColorJson ToColor(this UIColor self)
    {
        self.GetRGBA(out var red, out var green, out var blue, out var alpha);
        
        return new ColorJson(red * 255f, green * 255f, blue * 255f, alpha * 255f);
    }
}
