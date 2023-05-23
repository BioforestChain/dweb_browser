using CoreGraphics;

namespace DwebBrowser.Helper;

public static class AreaJsonExtensions
{
    /// <summary>
    /// 仅用于SafeArea，同步Android功能
    /// 用于返回给前端的AreaJson，与iOS实际不一致
    /// </summary>
    /// <returns></returns>
    public static AreaJson Add(this AreaJson self, AreaJson area)
    {
        return new(
            self.top + area.top,
            0,
            0,
            self.bottom + area.bottom);
    }

    /// <summary>
    /// 返回self超出area的部分
    /// </summary>
    /// <returns></returns>
    public static AreaJson Exclude(this AreaJson self, AreaJson area)
    {
        return new AreaJson(
            self.top > area.top ? self.top - area.top : 0,
            0,
            0,
            self.bottom > area.bottom ? self.bottom - area.bottom : 0);
    }

    /// <summary>
    /// 返回self与area更大的区域
    /// </summary>
    /// <returns></returns>
    public static AreaJson Union(this AreaJson self, AreaJson area)
    {
        return new AreaJson(
            Math.Max(self.top, area.top),
            Math.Max(self.left, area.left),
            Math.Max(self.right, area.right),
            Math.Max(self.bottom, area.bottom));
    }
}

