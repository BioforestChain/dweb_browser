using System;
namespace DwebBrowser.Helper;

public record ColorJson(int red, int green, int blue, int alpha)
{
    public static ColorJson White = new(255, 255, 255, 255);
    public static ColorJson Black = new(0, 0, 0, 255);
    public static ColorJson Transparent = new(0, 0, 0, 0);
    public ColorJson SetRed(int red)
    {
        return new ColorJson(red, green, blue, alpha);
    }
    public ColorJson SetGreen(int green)
    {
        return new ColorJson(red, green, blue, alpha);
    }
    public ColorJson SetBlue(int blue)
    {
        return new ColorJson(red, green, blue, alpha);
    }
    public ColorJson SetAlpha(int alpha)
    {
        return new ColorJson(red, green, blue, alpha);
    }
}