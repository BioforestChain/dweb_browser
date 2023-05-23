namespace DwebBrowser.Helper;

public record AreaJson(double top, double left, double right, double bottom)
{
    public static readonly AreaJson Empty = new(0, 0, 0, 0);
}

