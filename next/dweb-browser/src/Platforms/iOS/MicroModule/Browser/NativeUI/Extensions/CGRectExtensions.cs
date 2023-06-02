using CoreGraphics;

namespace DwebBrowser.Helper;

public static class CGRectExtensions
{
    public static AreaJson ToAreaJson(this CGRect rect) =>
        new(rect.Top.Value, rect.Left.Value, rect.Right.Value, rect.Bottom.Value);
}
