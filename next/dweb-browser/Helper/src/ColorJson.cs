using System;
namespace DwebBrowser.Helper;

public record ColorJson(double red, double green, double blue, double alpha)
{
    public static readonly ColorJson White = new(1, 1, 1, 1);
    public static readonly ColorJson Black = new(0, 0, 0, 1);
    public static readonly ColorJson Transparent = new(0, 0, 0, 0);
    public ColorJson SetRed(int red)
    {
        return new ColorJson(red, green, blue, alpha);
    }
    public ColorJson SetGreen(float green)
    {
        return new ColorJson(red, green, blue, alpha);
    }
    public ColorJson SetBlue(float blue)
    {
        return new ColorJson(red, green, blue, alpha);
    }
    public ColorJson SetAlpha(float alpha)
    {
        return new ColorJson(red, green, blue, alpha);
    }
}